package com.takesoon.proxy.config;


import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

/**
 * 代理配置类
 *
 * @author bluestome
 */
@Configuration
@ComponentScan(basePackageClasses = ProxyConfigScanner.class)
public class ProxyConfig {

    public static final String PROFILE_FILE_PATH = "env/proxy-config-%s.properties";

    public static final String PROFILE_NAME_DEV = "dev";
    public static final String PROFILE_NAME_TEST = "test";
    public static final String PROFILE_NAME_PROD = "prod";

    @Profile(PROFILE_NAME_PROD)
    @Configuration
    @PropertySource("classpath:env/proxy-config-prod.properties")
    static class ConfigProd {
    }

    @Profile(PROFILE_NAME_TEST)
    @Configuration
    @PropertySource("classpath:env/proxy-config-test.properties")
    static class ConfigTest {
    }

    @Profile(PROFILE_NAME_DEV)
    @Configuration
    @PropertySource("classpath:env/proxy-config-dev.properties")
    static class ConfigDev {
    }

    @Profile(PROFILE_NAME_DEV)
    @Bean(name = "propertyPlaceholderConfigurer")
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurerDev() {
        PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
        String path = String.format(PROFILE_FILE_PATH, PROFILE_NAME_DEV);
        ClassPathResource resource = new ClassPathResource(path);
        ppc.setLocation(resource);
        return ppc;
    }

    @Profile(PROFILE_NAME_TEST)
    @Bean(name = "propertyPlaceholderConfigurer")
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurerTest() {
        PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
        String path = String.format(PROFILE_FILE_PATH, PROFILE_NAME_TEST);
        ClassPathResource resource = new ClassPathResource(path);
        ppc.setLocation(resource);
        return ppc;
    }

    @Profile(PROFILE_NAME_PROD)
    @Bean(name = "propertyPlaceholderConfigurer")
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurerProd() {
        PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
        String path = String.format(PROFILE_FILE_PATH, PROFILE_NAME_PROD);
        ClassPathResource resource = new ClassPathResource(path);
        ppc.setLocation(resource);
        return ppc;
    }


}
