package com.takesoon.proxy.core.header;

/**
 * UA 生成器接口
 */
public interface IUAGenerator<T> {

    String generator(T data);
}
