package com.takesoon.proxy.core.header.impl;

import com.takesoon.proxy.core.header.IUAGenerator;

/**
 * UA生成器
 */
public class UAGenerator implements IUAGenerator<String> {
    private static final String DEFAULT_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36";

    /**
     * 浏览器数组
     */
    private static String[] BROWSERS = {
            "Mozilla", ""
    };

    public static String getUA() {
        return DEFAULT_UA;
    }

    @Override
    public String generator(String data) {
        return DEFAULT_UA;
    }
}
