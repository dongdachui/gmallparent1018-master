package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/5/16 9:31
 * 描述：
 **/
@Configuration
public class DeadLetterMqConfig {

    //  声明一些变量：
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";


    //  创建交换机：
    @Bean
    public DirectExchange exchange(){
        //  返回交换机
        return new DirectExchange(exchange_dead,true,false);
    }
    //  创建队列：
    @Bean
    public Queue queue1(){
        HashMap<String, Object> map = new HashMap<>();
        //  设置交换机与其他队列的绑定关系.
        map.put("x-dead-letter-exchange",exchange_dead);
        map.put("x-dead-letter-routing-key",routing_dead_2);
        //  设置消息的TTL 10 秒钟
        map.put("x-message-ttl",10000);
        //  返回队列
        return new Queue(queue_dead_1,true,false,false,map);
    }

    //  设置绑定关系：
    @Bean
    public Binding binding1(){
        //  通过routing-key1 绑定队列1
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }

    //  创建一个队列2
    @Bean
    public Queue queue2(){
        //  返回队列
        return new Queue(queue_dead_2,true,false,false);
    }

    //  设置一个绑定关系.
    @Bean
    public Binding binding2(){
        //  通过routing-key2 绑定队列2
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }
}