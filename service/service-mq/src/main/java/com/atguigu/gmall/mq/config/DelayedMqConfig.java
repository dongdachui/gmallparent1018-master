package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * author:atGuiGu-mqx
 * date:2022/5/16 10:22
 * 描述：
 **/
@Configuration
public class DelayedMqConfig {

    //  声明变量
    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    //  设置交换机
    @Bean
    public CustomExchange delayExchange(){
        //  定义map
        HashMap<String, Object> map = new HashMap<>();
        //  设置key  将消息暂存到交换机上。
        map.put("x-delayed-type","direct");
        //  设置交换机的类型 必须是 x-delayed-message
        return new CustomExchange(exchange_delay,"x-delayed-message",true,false,map);
    }

    //  设置队列
    @Bean
    public Queue delayQueue(){
        //  返回
        return new Queue(queue_delay_1,true,false,false);
    }

    //  设置一个绑定关系.
    @Bean
    public Binding delayBinding(){
        //  返回绑定
        //  BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(routing_delay).noargs();
    }



}