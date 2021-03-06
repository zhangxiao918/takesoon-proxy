package com.takesoon.proxy.core;

import com.takesoon.proxy.core.impl.ProxyPool;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ProxyPoolTest extends TestBase {

    private static final String AUTO_HOME_CLUB_URL_PREFIX = "https://club.autohome.com.cn";

    @Autowired
    private ProxyPool proxyPool;

    @Override
    public void init() {
        Assert.assertNotNull(proxyPool);
    }

    @Test
    public void getOkhttpClient() {
        Assert.assertNotNull(proxyPool);
        OkHttpClient okHttpClient = proxyPool.getOkHttpClient();
        Assert.assertNotNull(okHttpClient);
        try {
            get(okHttpClient, "https://www.taobao.com/", "", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        okHttpClient = proxyPool.getOkHttpClient();
        try {
            get(okHttpClient, "https://www.tmall.com/", "", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        okHttpClient = proxyPool.getOkHttpClient();
        try {
            get(okHttpClient, "http://hws.m.taobao.com/cache/wdetail/5.0?id=562407494306", "", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        okHttpClient = proxyPool.getOkHttpClient();
        try {
            get(okHttpClient, "https://uland.taobao.com/coupon/edetail?activityId=14f21952a20d4292af81e0a1a402ece9&itemId=524999421670", "", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        okHttpClient = proxyPool.getOkHttpClient();
        try {
            get(okHttpClient, "http://pub.alimama.com/", "", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        okHttpClient = proxyPool.getOkHttpClient();
        try {
            get(okHttpClient, "https://www.taobao.com/", "", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        okHttpClient = proxyPool.getOkHttpClient();
        try {
            get(okHttpClient, "http://www.xcar.com.cn/", "https://www.baidu.com/link?url=qVuG2cKevujSNCSVtK1uSqm-a6dXFde30QDrQBW4TZxVWF9rsZyiSTWU8nhbZJPf&wd=&eqid=d45524d80002a729000000045a703c4c", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        okHttpClient = proxyPool.getOkHttpClient();
        try {
            get(okHttpClient, "http://newcar.xcar.com.cn/3068/", "http://www.xcar.com.cn/", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        okHttpClient = proxyPool.getOkHttpClient();
        try {
            get(okHttpClient, "http://newcar.xcar.com.cn/3068/config.htm", "http://newcar.xcar.com.cn/3068/", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        okHttpClient = proxyPool.getOkHttpClient();
        try {
            get(okHttpClient, "http://newcar.xcar.com.cn/3068/video.htm", "http://newcar.xcar.com.cn/3068/config.htm", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //http://reg.xcar.com.cn/register.php
//        okHttpClient = proxyPool.getOkHttpClient();
        try {
            get(okHttpClient, "http://reg.xcar.com.cn/register.php", "http://newcar.xcar.com.cn/3068/video.htm", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void autoHomeBbsIndex() throws IOException {
        Assert.assertNotNull(proxyPool);
        OkHttpClient okHttpClient = proxyPool.getOkHttpClient();
        Assert.assertNotNull(okHttpClient);
        String body = null;
        try {
            body = get(okHttpClient, "https://www.autohome.com.cn/", "https://www.autohome.com.cn", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (StringUtils.isBlank(body)) {
            System.err.println("未抓取到内容");
            return;
        }
        Document document = Jsoup.parse(body);
        String title = document.title();
        if (StringUtils.equals(title, "用户访问安全认证")) {
            System.err.println("0\t需要验证" + okHttpClient.proxy().toString());
            System.out.println(body);
            try {
                Thread.sleep(60 * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String bbsUrl = "https://club.autohome.com.cn/bbs/forum-c-3788-1.html";
        body = get(okHttpClient, bbsUrl, "https://www.autohome.com.cn/beijing/", 0);
        Assert.assertNotNull(body);
        document = Jsoup.parse(body);
        title = document.title();
        if (StringUtils.equals(title, "用户访问安全认证")) {
            System.err.println("1\t需要验证" + okHttpClient.proxy().toString());
            try {
                Thread.sleep(60 * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Elements elements = document.select("div#subcontent > dl.list_dl > dt > a[href]");
        Assert.assertNotNull(elements);
        int j = 0;
        do {
            int i = 0;
            for (Element element : elements) {
                String href = element.attr("href");
                if (StringUtils.isNotBlank(href)) {
                    System.out.println(i + "\t\t" + href);
                    String strFormat = "%s_%s";
                    if (!StringUtils.startsWith(href, "https://")
                            || !StringUtils.startsWith(href, "http://")) {
                        href = AUTO_HOME_CLUB_URL_PREFIX + href;
                        body = get(okHttpClient, href, bbsUrl, 0);
                        document = Jsoup.parse(body);
                        title = document.title();
                        if (StringUtils.equals(title, "用户访问安全认证")) {
                            System.err.println("j=" + j + ",i=" + i + " 需要验证" + okHttpClient.proxy().toString());
                            try {
                                Thread.sleep(60 * 1000L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            bbsUrl = href;
                            continue;
                        }
                        System.out.println(String.format(strFormat,title,href));
                        try {
                            Thread.sleep(750);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                okHttpClient = proxyPool.getOkHttpClient();
                i++;
            }
            j++;
        } while (j < 5);
    }

    @Test
    public void redirect() {
        String url = "https://www.autohome.com.cn/";
        Assert.assertNotNull(proxyPool);
        for (int i = 0; i < 2; i++) {
            OkHttpClient okHttpClient = proxyPool.getOkHttpClient();
            Assert.assertNotNull(okHttpClient);
            try {
                String redirectUrl = redirect(okHttpClient, url, null, 0);
                if (!redirectUrl.startsWith("http:") || !redirectUrl.startsWith("https:")) {
                    redirectUrl = ("https:" + redirectUrl);
                }
                String proxy = "";
                if (null != okHttpClient.proxy()
                        && null != okHttpClient.proxy().address()
                        && okHttpClient.proxy().address() instanceof InetSocketAddress) {
                    //有代理服务的流程
                    InetSocketAddress inetSocketAddress = (InetSocketAddress) okHttpClient.proxy().address();
                    proxy = inetSocketAddress.getHostName() + ":" + inetSocketAddress.getPort();
                }
                Assert.assertNotNull(redirectUrl);
                System.out.println("redirectUrl=" + redirectUrl + ",proxy:" + proxy);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
