package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * author:atGuiGu-mqx
 * date:2022/4/26 10:31
 * 描述：
 **/
@Configuration
public class CorsConfig {

    //  制作一个配置类
    @Bean
    public CorsWebFilter corsWebFilter(){
        //  创建 CorsConfiguration 对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");    //  请求url
        corsConfiguration.addAllowedMethod("*");    //  请求方法
        corsConfiguration.addAllowedHeader("*");    //  请求头
        corsConfiguration.setAllowCredentials(true);    //  允许携带cookie
        //  创建CorsConfigurationSource接口的实现对象
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        //  设置过滤的路径，以及条件参数
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**",corsConfiguration);
        //  返回对象
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }
}