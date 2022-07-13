package com.atguigu.gmall.common.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.model.GmallCorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * author:atGuiGu-mqx
 * date:2022/5/14 14:25
 * 描述：封装发送消息的内容.
 **/
@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    //  发送消息方法
    public Boolean sendMsg(String exchange,String routingKey ,Object msg){
        //  创建一个对象 ：存储发送消息的载体：
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        //  给id 赋值
        String uuId = UUID.randomUUID().toString().replace("-","");
        gmallCorrelationData.setId(uuId);
        //  赋值交换机
        gmallCorrelationData.setExchange(exchange);
        //  赋值路由键
        gmallCorrelationData.setRoutingKey(routingKey);
        //  赋值发送消息
        gmallCorrelationData.setMessage(msg);

        //  将gmallCorrelationData 存储到缓存中. 缓存的key ：
        //  101 ： {msg01}  102: {msg02}
        this.redisTemplate.opsForValue().set(uuId, JSON.toJSONString(gmallCorrelationData),10, TimeUnit.MINUTES);
        //  发送消息
        //  this.rabbitTemplate.convertAndSend(exchange,routingKey,msg);
        //  第四个对象： CorrelationData 消息的载体并有一个id！
        this.rabbitTemplate.convertAndSend(exchange,routingKey,msg,gmallCorrelationData);
        return true;
    }

    //  发送一个延迟消息方法.
    public Boolean sendDelayMsg(String exchange,String routingKey ,Object msg, int delayTime){
        //  创建一个对象 ：存储发送消息的载体：
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        //  给id 赋值
        String uuId = UUID.randomUUID().toString().replace("-","");
        gmallCorrelationData.setId(uuId);
        //  赋值交换机
        gmallCorrelationData.setExchange(exchange);
        //  赋值路由键
        gmallCorrelationData.setRoutingKey(routingKey);
        //  赋值发送消息
        gmallCorrelationData.setMessage(msg);
        //  赋值延迟
        gmallCorrelationData.setDelay(true);
        //  设置延迟时间
        gmallCorrelationData.setDelayTime(delayTime);
        //  需要将数据放入缓存.
        this.redisTemplate.opsForValue().set(uuId,JSON.toJSONString(gmallCorrelationData),10,TimeUnit.MINUTES);
        rabbitTemplate.convertAndSend(exchange,routingKey,msg,(message)->{
            //  设置消息的过期时间
            message.getMessageProperties().setDelay(1000*delayTime);
            return message;
        },gmallCorrelationData);
        return true;
    }
}