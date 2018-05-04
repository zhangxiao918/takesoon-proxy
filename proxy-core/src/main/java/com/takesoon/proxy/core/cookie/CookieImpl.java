package com.takesoon.proxy.core.cookie;

import com.ouer.cache.redis.RedisCacheService;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于缓存的cookie实现类
 *
 * @author bluestome
 */
public class CookieImpl implements CookieJar {

    private Logger logger = LoggerFactory.getLogger(CookieImpl.class);
    private Proxy proxy;
    private String tag;
    public static final String CACHE_KEY = "%s:%s";
    public static final String PROXY_HOST_CACHE_KEY = "%s:host:%s";
    public static final String PROXY_COOKIE_DOMAIN_CACHE_KEY_V2 = "%s:cookie:%s";
    public static final String PROXY_COOKIE_VALUE_CACHE_KEY_V2 = "%s:cookie:%s:%s";

    private volatile ConcurrentHashMap<String, ConcurrentHashMap<String, String>> memoryCookies = new ConcurrentHashMap<>();

    private RedisCacheService cacheService;

    public CookieImpl(String tag, Proxy proxy, RedisCacheService cacheService) {
        this.tag = tag;
        this.proxy = proxy;
        this.cacheService = cacheService;
    }


    @Override
    public synchronized void saveFromResponse(HttpUrl httpUrl, List<Cookie> cookies) {
        String key = String.format(PROXY_HOST_CACHE_KEY, String.valueOf(hashCode()), httpUrl.host());
        if (StringUtils.isNotBlank(tag)) {
            key = String.format(PROXY_HOST_CACHE_KEY, tag, httpUrl.host());
        }
        for (Cookie cookie : cookies) {
            if (StringUtils.isNotBlank(cookie.name()) && null != cookie) {
                if (cookie.expiresAt() > now()) {
                    String key2 = String.format(PROXY_COOKIE_DOMAIN_CACHE_KEY_V2, tag, cookie.domain());
                    cacheService.sadd(key, key2);

                    String key3 = String.format(PROXY_COOKIE_VALUE_CACHE_KEY_V2, tag, cookie.domain(), cookie.name());
                    cacheService.sadd(key2, key3);
                    cacheService.put(key3, cookie.toString());
                    cacheService.expireAfter(key3, (int) ((cookie.expiresAt() - System.currentTimeMillis()) / 1000) - 5);

                }
            }
        }

    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl httpUrl) {
        List<Cookie> list = new ArrayList<Cookie>();
        boolean existsExpiredCookies = false;
        //从host中获取对应的cookie.domain列表
        String key = String.format(PROXY_HOST_CACHE_KEY, String.valueOf(hashCode()), httpUrl.host());
        if (StringUtils.isNotBlank(tag)) {
            key = String.format(PROXY_HOST_CACHE_KEY, tag, httpUrl.host());
        }
        Set<String> cookieDomains = cacheService.smembers(key);
        if (CollectionUtils.isNotEmpty(cookieDomains)) {
            for (String cookieDomain : cookieDomains) {
                if (existsExpiredCookies) {
                    cacheService.remove(cookieDomain);
                } else {
                    Set<String> cookieNames = cacheService.smembers(cookieDomain);
                    if (CollectionUtils.isNotEmpty(cookieNames)) {
                        for (String cookieName : cookieNames) {
                            if (existsExpiredCookies) {
                                cacheService.remove(cookieName);
                            } else {
                                String cacheCookie = cacheService.get(cookieName, String.class, null);
                                if (StringUtils.isNotBlank(cacheCookie)) {
                                    Cookie cookie = Cookie.parse(httpUrl, cacheCookie);
                                    if (null != cookie) {
                                        if (cookie.expiresAt() > now()) {
                                            list.add(cookie);
                                        } else {
                                            existsExpiredCookies = true;
                                            cacheService.remove(cookieName);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (existsExpiredCookies) {
            cacheService.remove(key);
        }
        return list;
    }

    /**
     * 当前时间戳
     *
     * @return
     */
    private Long now() {
        return System.currentTimeMillis();
    }
}

