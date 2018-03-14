package com.takesoon.proxy.core;

import com.takesoon.proxy.config.ProxyConfig;
import com.takesoon.proxy.core.config.ProxyCoreConfig;
import com.takesoon.proxy.core.header.impl.UAGenerator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles(ProxyConfig.PROFILE_NAME_TEST)
@ContextConfiguration(classes = {ProxyConfig.class, ProxyCoreConfig.class})
public class TestBase {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    public static final Integer MAX_RETRY = 3;
    protected static final String RESP_HEAD_LOCATION = "Location";
    protected static final String REQ_HEAD_UA = "User-Agent";
    protected static final String REQ_HEAD_REFERER = "Referer";
    protected BlockingQueue<String> blockingQueue = new LinkedBlockingQueue<>(1);


    @Before
    public void init() {
        long start = System.currentTimeMillis();
        System.out.println(String.format("开始时间:%d", start));
    }

    /**
     * 发起get请求
     *
     * @param okHttpClient
     * @param url
     * @param referer      引用页面
     * @param times        调用次数
     * @return
     */
    protected String get(OkHttpClient okHttpClient, String url, String referer, int times) throws IOException {
        if (times >= MAX_RETRY) {
            logger.error("request.fetch.times:{},url:{}", times, url);
            return null;
        }
        if (times >= 1) {
            try {
                blockingQueue.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        builder.header(REQ_HEAD_UA, UAGenerator.getUA());
        if (StringUtils.isNotEmpty(referer)) {
            builder.header(REQ_HEAD_REFERER, referer);
        }
        //TODO 兼容做一个特殊的处理
        Request getReq = builder.get().build();
        Response response = null;
        try {
            response = okHttpClient.newCall(getReq).execute();
            if (null != response) {
                if (response.isSuccessful()) {
                    if (null != response.body()) {
                        return response.body().string();
                    }
                } else {
                    if (response.isRedirect()) {
                        String location = response.header(RESP_HEAD_LOCATION);
                        if (!location.equals(url)) {
                            times++;
                            return get(okHttpClient, location, referer, times);
                        }
                    }
                    response.close();
                }
            }
        } catch (IOException e) {
            if (null != response) {
                response.close();
            }
            String proxy = "0.0.0.0:0000";
            if (null != okHttpClient.proxy()
                    && null != okHttpClient.proxy().address()
                    && okHttpClient.proxy().address() instanceof InetSocketAddress) {
                //有代理服务的流程
                InetSocketAddress inetSocketAddress = (InetSocketAddress) okHttpClient.proxy().address();
                proxy = inetSocketAddress.getHostName() + ":" + inetSocketAddress.getPort();
            }
            logger.error("makeGetReq.exception,proxy:{},url:{},times:{},referer:{},exception:{}", proxy, url, times, referer, e);
            throw new IOException("遇到异常，需要处理:" + e.getMessage());

        }
        return null;
    }

    /**
     * 发起get请求
     *
     * @param okHttpClient
     * @param url
     * @param referer      引用页面
     * @param times        调用次数
     * @return
     */
    protected String redirect(OkHttpClient okHttpClient, String url, String referer, int times) throws IOException {
        if (times >= MAX_RETRY) {
            logger.error("request.fetch.times:{},url:{}", times, url);
            return null;
        }
        if (times >= 1) {
            try {
                blockingQueue.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(okHttpClient.followRedirects() || okHttpClient.followSslRedirects()){
            OkHttpClient okHttpClient1 = okHttpClient.newBuilder().followRedirects(false).followSslRedirects(false).build();
            return redirect(okHttpClient1,url,referer,times);
        }
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        builder.header(REQ_HEAD_UA, UAGenerator.getUA());
        if (StringUtils.isNotEmpty(referer)) {
            builder.header(REQ_HEAD_REFERER, referer);
        }
        //TODO 兼容做一个特殊的处理
        Request getReq = builder.get().build();
        Response response = null;
        try {
            response = okHttpClient.newCall(getReq).execute();
            if (null != response) {
                if (response.isSuccessful()) {
                    if (null != response.body()) {
                        return url;
                    }
                } else {
                    if (response.isRedirect()) {
                        String location = response.header(RESP_HEAD_LOCATION);
                        if (StringUtils.isNotBlank(location)) {
                            if (!location.equals(url)) {
                                return location;
                            } else {
                                return redirect(okHttpClient, location, url, times);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            String proxy = "0.0.0.0:0000";
            if (null != okHttpClient.proxy()
                    && null != okHttpClient.proxy().address()
                    && okHttpClient.proxy().address() instanceof InetSocketAddress) {
                //有代理服务的流程
                InetSocketAddress inetSocketAddress = (InetSocketAddress) okHttpClient.proxy().address();
                proxy = inetSocketAddress.getHostName() + ":" + inetSocketAddress.getPort();
            }
            logger.error("makeGetReq.exception,proxy:{},url:{},times:{},referer:{},exception:{}", proxy, url, times, referer, e);
            throw new IOException("遇到异常，需要处理:" + e.getMessage());

        } finally {
            if (null != response) {
                response.close();
            }
        }
        return null;
    }
}
