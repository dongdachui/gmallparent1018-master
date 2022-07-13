package com.atguigu.gmall.common.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.model.GmallCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import springfox.documentation.spring.web.json.Json;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * author:atGuiGu-mqx
 * date:2022/5/14 15:08
 * 描述：消息发送确认. 确认消息是否到交换机，消息是否到队列
 **/
@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ReturnCallback,RabbitTemplate.ConfirmCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;


    //  初始化操作. 需要将 rabbitTemplate 与这个类发生有关系.
    @PostConstruct
    public void init(){
        rabbitTemplate.setReturnCallback(this);
        rabbitTemplate.setConfirmCallback(this);
    }

    /**
     * 确认消息是否到交换机
     * @param correlationData 这个对象中 有个Id ，同时有Message
     * @param ack
     * @param code
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String code) {
        //  判断消息是否到交换机.
        if (ack) {
            //  消息到了交换机
            System.out.println("发送到交换机成功.");
        }else {
            //  消息没有到交换机
            System.out.println("发送到交换机失败."+code);
            //  调用重试方法.
            this.retryMsg(correlationData);
        }
    }


    /**
     * 确认消息是否到队列，如果消息没有到队列，才会执行这个方法！
     * @param message
     * @param replyCode   应答码
     * @param replyText    应答码所对应的消息是啥
     * @param exchange
     * @param routingKey
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);

        //  需要获取到一个 CorrelationData 对象！ 消息没有到队列时，才会执行这个方法。
        //  获取缓存中的数据. 有我们想要的对象.
        //  获取缓存的key 才能获取到value    CorrelationData.id = message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");
        String uuid = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");
        //  因为在存储的时候：this.redisTemplate.opsForValue().set(gmallCorrelationData.getId(), JSON.toJSONString(gmallCorrelationData),10, TimeUnit.MINUTES);
        //  存的key uuid  value = JSON.toJSONString(gmallCorrelationData)
        String jsonStr = (String) this.redisTemplate.opsForValue().get(uuid);
        //  将字符串在转换为对象：
        GmallCorrelationData gmallCorrelationData = JSON.parseObject(jsonStr, GmallCorrelationData.class);
        //  调用重发方法.
        this.retryMsg(gmallCorrelationData);
    }

    /**
     * 重试机制：
     * @param correlationData
     */
    private void retryMsg(CorrelationData correlationData) {
        //  核心代码：
        //  需要将这个对象CorrelationData 转换为
        GmallCorrelationData gmallCorrelationData = (GmallCorrelationData) correlationData;
        //  来实现重试机制：
        //  获取重试的次数
        int retryCount = gmallCorrelationData.getRetryCount();
        if (retryCount>=3){
            //  重试超过了3次还没有发送成功的话，那么可以不用重复发送了.
            log.error("重试次数已过,发送消息失败"+ JSON.toJSONString(gmallCorrelationData));
            //  可以将这个重试发送消息写入 重发消息记录表中！  insert into t_name value();
        }else {
            //  次数累加
            retryCount+=1;
            gmallCorrelationData.setRetryCount(retryCount);
            System.out.println("重试次数：\t"+retryCount);
            //  更新缓存中的gmallCorrelationData！
            this.redisTemplate.opsForValue().set(gmallCorrelationData.getId(), JSON.toJSONString(gmallCorrelationData),10, TimeUnit.MINUTES);
            //  判断是否是延迟消息
            if (gmallCorrelationData.isDelay()){
                //  表示是延迟消息
                this.rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(),gmallCorrelationData.getRoutingKey(),gmallCorrelationData.getMessage(),(message)->{
                    //  正常：10秒钟
                    //  message.getMessageProperties().setDelay(gmallCorrelationData.getDelayTime()*1000);
                    //  测试：10毫秒：
                    message.getMessageProperties().setDelay(gmallCorrelationData.getDelayTime());
                    return message;
                },gmallCorrelationData);
            }else {
                //  表示普通消息
                this.rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(),gmallCorrelationData.getRoutingKey(),gmallCorrelationData.getMessage(),gmallCorrelationData);
            }
        }


    }

}