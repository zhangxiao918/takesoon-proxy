package com.takesoon.proxy.core.cookie;

import com.alibaba.fastjson.JSON;
import com.ouer.cache.redis.RedisCacheService;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于缓存的cookie实现类
 * @author bluestome
 */
public class CookieImpl implements CookieJar {

    private Logger logger = LoggerFactory.getLogger(CookieImpl.class);
    private Proxy proxy;
    private String tag;
    public static final String CACHE_KEY = "%s:%s";

    private volatile ConcurrentHashMap<String, ConcurrentHashMap<String, String>> memoryCookies = new ConcurrentHashMap<>();

    private RedisCacheService cacheService;

    public CookieImpl(String tag, Proxy proxy, RedisCacheService cacheService) {
        this.tag = tag;
        this.proxy = proxy;
        this.cacheService = cacheService;
    }


    @Override
    public synchronized void saveFromResponse(HttpUrl httpUrl, List<Cookie> cookies) {
        String key = key(String.valueOf(hashCode()), httpUrl.host());
        if (StringUtils.isNotBlank(tag)) {
            key = key(tag, httpUrl.host());
        }

        if (null != httpUrl && StringUtils.isNotBlank(httpUrl.host()) && StringUtils.isNotBlank(key)) {
            Map<String, String> cookieMap = new HashMap<>();
            //Redis中获取已部分已存的缓存
            if (StringUtils.isNotBlank(key)) {
                String cacheJson = cacheService.get(key, String.class, null);
                if (StringUtils.isNotBlank(cacheJson)) {
                    Map<String, String> cacheCookieMap = JSON.parseObject(cacheJson, Map.class);
                    if (MapUtils.isNotEmpty(cacheCookieMap)) {
                        cookieMap.putAll(cacheCookieMap);
                    }
                }
            }

            //实际传入需要被缓存的Cookie的列表处理
            for (Cookie cookie : cookies) {
                if (StringUtils.isNotBlank(cookie.name()) && null != cookie) {
                    if (cookie.expiresAt() > now()) {
                        cookieMap.put(cookie.name(), cookie.toString());
                    }
                }
            }

            //对需要缓存的对象进行加工处理
            if (MapUtils.isNotEmpty(cookieMap)) {
                ConcurrentHashMap<String, String> hostCookieCache = memoryCookies.get(httpUrl.host());
                if (MapUtils.isEmpty(hostCookieCache)) {
                    hostCookieCache = new ConcurrentHashMap<>();
                }
                hostCookieCache.putAll(cookieMap);
                if (MapUtils.isNotEmpty(hostCookieCache)) {
                    memoryCookies.put(httpUrl.host(), hostCookieCache);
                }

                String json = JSON.toJSONString(cookieMap);
                if (StringUtils.isNotBlank(json)) {
                    cacheService.put(key, json);
                }
            }
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl httpUrl) {
        List<Cookie> list = new ArrayList<Cookie>();
        ConcurrentHashMap<String, String> hostCookieCache = memoryCookies.get(httpUrl.host());
        if (MapUtils.isNotEmpty(hostCookieCache)) {
            //本地缓存存在
            if (MapUtils.isNotEmpty(hostCookieCache)) {
                for (String cookieStr : hostCookieCache.values()) {
                    if (StringUtils.isNotBlank(cookieStr)) {
                        Cookie cookie = Cookie.parse(httpUrl, cookieStr);
                        if (null != cookie && cookie.expiresAt() > now()) {
                            list.add(cookie);
                        }
                    }
                }
            }
        } else {
            hostCookieCache = new ConcurrentHashMap<>();
            //第一次从Redis中获取
            String key = key(String.valueOf(hashCode()), httpUrl.host());
            if (StringUtils.isNotBlank(tag)) {
                key = key(tag, httpUrl.host());
            }

            Map<String, String> cacheCookieMap = new HashMap<>();
            if (StringUtils.isNotBlank(key)) {
                String jsonString = cacheService.get(key, String.class, null);
                if (StringUtils.isNotBlank(jsonString)) {
                    Map<String, String> tmpCacheCookieMap = JSON.parseObject(jsonString, Map.class);
                    if (MapUtils.isNotEmpty(tmpCacheCookieMap)) {
                        cacheCookieMap.putAll(tmpCacheCookieMap);
                    }
                }
            }

            if (MapUtils.isNotEmpty(cacheCookieMap)) {
                for (Map.Entry<String, String> entry : cacheCookieMap.entrySet()) {
                    if (StringUtils.isNotBlank(entry.getValue())) {
                        Cookie cookie = Cookie.parse(httpUrl, entry.getValue());
                        if (null != cookie) {
                            if (cookie.expiresAt() > now()) {
                                hostCookieCache.put(cookie.name(), cookie.toString());
                                list.add(cookie);
                            }
//                            else {
//                                logger.error("remove key:{},value:{}", entry.getKey(), entry.getValue());
//                                cacheCookieMap.remove(entry.getKey());
//                                hostCookieCache.remove(cookie.name());
//                            }
                        }
                    }
                }

                if (MapUtils.isNotEmpty(hostCookieCache)) {
                    memoryCookies.put(httpUrl.host(), hostCookieCache);
                }

            }
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

