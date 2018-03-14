package com.takesoon.proxy.core.cookie;

import com.alibaba.fastjson.JSON;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 基于内存的cookie实现类
 */
public class CookieMemoryImpl implements CookieJar {

    private Logger logger = LoggerFactory.getLogger(CookieMemoryImpl.class);
    private String tag;
    public static final String CACHE_KEY = "%s:%s";

    private volatile ConcurrentHashMap<String, ConcurrentHashMap<String, Cookie>> memoryCookies = new ConcurrentHashMap<>();

    private CookieMemoryImpl() {
    }

    public CookieMemoryImpl(String tag) {
        this.tag = tag;
    }


    @Override
    public synchronized void saveFromResponse(HttpUrl httpUrl, List<Cookie> cookies) {
        //TODO 待改造
        if (null != httpUrl && StringUtils.isNotBlank(httpUrl.host())) {
            ConcurrentHashMap<String, Cookie> maps = null;
            if (!memoryCookies.containsKey(httpUrl.host())) {
                maps = new ConcurrentHashMap<>();
            } else {
                maps = memoryCookies.get(httpUrl.host());
            }
            //TODO 遍历cookie并添加到对应的域名下
            for (Cookie cookie : cookies) {
                if (StringUtils.isNotBlank(cookie.name()) && null != cookie) {
                    if (cookie.expiresAt() > now()) {
                        maps.put(cookie.name(), cookie);
                        logger.info("cookie:{}", cookie.toString());
                    }
                }
            }
            logger.debug(">>>>>> saveFromResponse host:{},cookie.size:{}，{}", httpUrl.host(), maps.size(), cookies);
            memoryCookies.put(httpUrl.host(), maps);
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl httpUrl) {
        List<Cookie> list = new ArrayList<Cookie>();
        if (null != httpUrl && StringUtils.isNotBlank(httpUrl.host())) {
            if (memoryCookies.containsKey(httpUrl.host())) {
                ConcurrentHashMap<String, Cookie> maps = memoryCookies.get(httpUrl.host());
                if (null != maps && maps.size() > 0) {
                    for (Cookie cookie : maps.values()) {
                        if (cookie.expiresAt() > now()) {
                            list.add(cookie);
                        }
                    }
                }
            }
            logger.debug("loadForRequest host:{},cookies:{}", httpUrl.host(), list.size(), JSON.toJSONString(list));
        }
        return list;
    }

    /**
     * 生成缓存key
     *
     * @param host
     * @param name
     * @return
     */
    private String key(String host, String name) {
        return String.format(CACHE_KEY, host, name);
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
