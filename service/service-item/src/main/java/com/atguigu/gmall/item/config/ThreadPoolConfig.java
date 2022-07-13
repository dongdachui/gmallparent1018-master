package com.atguigu.gmall.item.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * author:atGuiGu-mqx
 * date:2022/5/4 15:23
 * 描述：
 **/
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){

        //  创建线程池并注入到spirng 容器中.
        return new ThreadPoolExecutor(
                5, //  核心线程个数 ? cpu 密集型 ：n+1  io 密集型 2n n：服务器的核数
                100, //  最大线程个数
                3, //  空线程存活时间
                TimeUnit.SECONDS, // 空闲线程存活时间单位
                new ArrayBlockingQueue<>(3) // 默认线程工厂，拒绝策略.
        );
    }
}