package com.takesoon.proxy.core.cookie;

import com.alibaba.fastjson.JSON;
import com.ouer.cache.redis.RedisCacheService;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.util.*;
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
    public static final String CACHE_SPEC_KEY = "%s_%s";

    private volatile ConcurrentHashMap<String, ConcurrentHashMap<String, String>> memoryCookies = new ConcurrentHashMap<>();

    private RedisCacheService cacheService;

    public CookieImpl(String tag, Proxy proxy, RedisCacheService cacheService) {
        this.tag = tag;
        this.proxy = proxy;
        this.cacheService = cacheService;
    }


    @Override
    public synchronized void saveFromResponse(HttpUrl httpUrl, List<Cookie> cookies) {
        logger.debug("set-cookie", JSON.toJSONString(cookies));
        String key = key(String.valueOf(hashCode()), httpUrl.host());
        if (StringUtils.isNotBlank(tag)) {
            key = key(tag, httpUrl.host());
        }
        for (Cookie cookie : cookies) {
            if (StringUtils.isNotBlank(cookie.name()) && null != cookie) {
                if (cookie.expiresAt() > now()) {
                    cacheService.sadd(key, cookie.domain());
                    cacheService.sadd(cookie.domain(), cookie.name());
                    String dck = String.format(CACHE_SPEC_KEY, cookie.domain(), cookie.name());
                    cacheService.put(dck, cookie.toString());
                    cacheService.expireAfter(dck, (int) (cookie.expiresAt() - System.currentTimeMillis()));
                }
            }
        }

    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl httpUrl) {
        List<Cookie> list = new ArrayList<Cookie>();
        //从host中获取对应的cookie.domain列表
        String key = key(String.valueOf(hashCode()), httpUrl.host());
        if (StringUtils.isNotBlank(tag)) {
            key = key(tag, httpUrl.host());
        }
        Set<String> tmpCookies = new HashSet<>();
        Set<String> domains = cacheService.smembers(key);
        for (String domain : domains) {
            Set<String> cacheCookieNames = cacheService.smembers(domain);
            for (String cacheCookieName : cacheCookieNames) {
                String dck = String.format(CACHE_SPEC_KEY, domain, cacheCookieName);
                String cacheCookie = cacheService.get(dck, String.class, null);
                if (StringUtils.isNotBlank(cacheCookie)) {
                    Cookie cookie = Cookie.parse(httpUrl, cacheCookie);
                    if (null != cookie) {
                        if (cookie.expiresAt() > now()) {
                            list.add(cookie);
                            tmpCookies.add(cookie.toString());
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(tmpCookies)) {
            logger.debug("loadForRequest.tmpCookies {}", JSON.toJSONString(tmpCookies));
        }
        return list;
    }

    /**
     * 生成缓存key
     *
     * @param proxyServer 代理服务
     * @param domain
     * @return
     */
    private String key(String proxyServer, String domain) {
        return String.format(CACHE_KEY, proxyServer, domain);
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

