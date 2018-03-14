package com.takesoon.proxy.core;

import com.ouer.cache.redis.RedisCacheService;
import com.takesoon.proxy.core.cookie.CookieImpl;
import com.takesoon.proxy.core.cookie.CookieMemoryImpl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 抽象代理类
 */
public abstract class AbstractProxyPool {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 调用次数
     **/
    protected volatile AtomicInteger invokeTimes = new AtomicInteger();

    /**
     * 客户端列表
     **/
    protected volatile List<OkHttpClient> okHttpClients = new ArrayList<>();

    protected volatile OkHttpClient defaultOkHttp;

    /**
     * 初始化代理服务器实例
     *
     * @param proxyServers 代理服务器地址
     */
    protected void init(String proxyServers) {
        init(proxyServers,null);
    }

    /**
     * 初始化代理服务器实例
     *
     * @param proxyServers 代理服务器地址
     */
    protected void init(String proxyServers,RedisCacheService cacheService) {
        if (StringUtils.isNotBlank(proxyServers)) {
            String[] servers = proxyServers.split(",");
            if (null != servers && servers.length > 0) {
                for (String server : servers) {
                    String[] tmpSplit = server.split(":");
                    if (null != tmpSplit && tmpSplit.length == 2) {
                        OkHttpClient.Builder builder = new OkHttpClient.Builder();
                        builder.readTimeout(15, TimeUnit.SECONDS);
                        builder.connectTimeout(15, TimeUnit.SECONDS);
                        builder.retryOnConnectionFailure(true);
                        TrustAllManager trustAllManager = new TrustAllManager();
                        builder.sslSocketFactory(createSSLSocketFactory(), trustAllManager);
                        builder.hostnameVerifier(new TrustAllHostnameVerifier());
                        InetSocketAddress inetSocketAddress = new InetSocketAddress(tmpSplit[0], Integer.valueOf(tmpSplit[1]));
                        Proxy proxy = new Proxy(Proxy.Type.HTTP, inetSocketAddress);
                        OkHttpClient.Builder tmpBuilder = builder.proxy(proxy);
                        if(null != cacheService) {
                            // 走缓存逻辑
                            tmpBuilder.cookieJar(new CookieImpl(tmpSplit[0],proxy,cacheService));
                        }else{
                            // 走内存逻辑
                            tmpBuilder.cookieJar(new CookieMemoryImpl(tmpSplit[0]));
                        }
                        okHttpClients.add(tmpBuilder.build());
                    }
                }
                logger.debug("ProxyPool.init.size:{}", okHttpClientSize());
            }
        } else {
            if(null != cacheService) {
                buildDefaultOkHttpClient(cacheService);
            }else{
                buildDefaultOkHttpClient();
            }
        }
        initInvokieTime();
    }

    /**
     * 初始化调用次数
     */
    private void initInvokieTime() {
        String uuid = UUID.randomUUID().toString();
        Integer hashCode = uuid.hashCode();
        if (hashCode < 0) {
            hashCode = Math.abs(hashCode);
        }
        invokeTimes.set(hashCode);
        logger.info("initInvokieTime:{}", invokeTimes.get());
    }

    /**
     * 构建默认的客户端
     */
    private void buildDefaultOkHttpClient() {
        buildDefaultOkHttpClient(null);
    }

    /**
     * 构建默认的客户端
     * @param cacheService  缓存对象
     */
    private void buildDefaultOkHttpClient(RedisCacheService cacheService) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.readTimeout(15, TimeUnit.SECONDS);
        builder.connectTimeout(15, TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(true);
        TrustAllManager trustAllManager = new TrustAllManager();
        builder.sslSocketFactory(createSSLSocketFactory(), trustAllManager);
        builder.hostnameVerifier(new TrustAllHostnameVerifier());
        if(null != cacheService) {
            builder.cookieJar(new CookieImpl("0.0.0.0",null,cacheService));
        }else{
            builder.cookieJar(new CookieMemoryImpl("0.0.0.0"));
        }
        defaultOkHttp = builder.build();
    }

    /**
     * 默认信任所有的证书
     * TODO 最好加上证书认证，主流App都有自己的证书
     *
     * @return
     */
    private static SSLSocketFactory createSSLSocketFactory() {

        SSLSocketFactory sSLSocketFactory = null;

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllManager()},
                    new SecureRandom());
            sSLSocketFactory = sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sSLSocketFactory;
    }

    /**
     *
     */
    private static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }
    }

    /**
     *
     */
    private static class TrustAllHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }


    /**
     * 获取OkHttpClient实例
     *
     * @return
     */
    public synchronized OkHttpClient getOkHttpClient() {
        Integer mode = getMode();
        if (okHttpClientSize() == 0) {
            logger.error("getOkHttpClient.error,mode:{},client.size:{},userDefaultClient", mode, okHttpClientSize());
            return getDefaultOkHttp();
        }
        Assert.assertNotNull(okHttpClients);
        return okHttpClients.get(mode);
    }

    /**
     * 获取OkHttpClient实例
     *
     * @return
     */
    public synchronized List<OkHttpClient> getOkHttpClients() {
        synchronized (okHttpClients) {
            if (CollectionUtils.isNotEmpty(okHttpClients)) {
                return okHttpClients;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 返回默认的OkHttp客户端
     *
     * @return
     */
    public OkHttpClient getDefaultOkHttp() {
        if (null == defaultOkHttp) {
            buildDefaultOkHttpClient();
        }
        return defaultOkHttp;
    }

    /**
     * 获取OkHttpClient实例数量
     *
     * @return
     */
    private synchronized Integer okHttpClientSize() {
        if (null == okHttpClients) {
            return 0;
        }
        return okHttpClients.size();
    }


    /**
     * 获取取模后的下标值
     *
     * @return
     */
    private synchronized Integer getMode() {
        if (invokeTimes.get() >= Integer.MAX_VALUE) {
            invokeTimes.set(0);
        }
        if (okHttpClientSize() == 0) {
            return 0;
        }
        Integer mode = Math.abs(invokeTimes.getAndIncrement()) % okHttpClientSize();
        if (invokeTimes.get() % 100 == 0) {
            logger.info("invokeTimes:{},mode:{}", invokeTimes.get(), mode);
        }
        return 0;
    }

}
