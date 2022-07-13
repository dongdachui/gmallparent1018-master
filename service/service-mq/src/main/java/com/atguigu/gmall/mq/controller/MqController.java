package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.MacSpi;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * author:atGuiGu-mqx
 * date:2022/5/14 14:27
 * 描述：
 **/
@RestController
@RequestMapping("mq")
public class MqController {

    @Autowired
    private RabbitService rabbitService;


    @Autowired
    private RabbitTemplate rabbitTemplate;


    @GetMapping("sendConfirm")
    public Result sendConfirm(){
        //  发送消息
        rabbitService.sendMsg("exchange.confirm666","routingKey.confirm","atguigu");
        return Result.ok();
    }

    //  编写控制器发送一个消息.
    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle(){
        //  是否延迟了10秒钟
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //  发送消息.
        rabbitService.sendMsg(DeadLetterMqConfig.exchange_dead,DeadLetterMqConfig.routing_dead_1,"ok");
        System.out.println("发送消息时间：\t"+simpleDateFormat.format(new Date()));
        return Result.ok();
    }


    //  基于延迟插件发送消息
    @GetMapping("delayMsg")
    public Result delayMsg(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //  发送消息.
        //  rabbitService.sendMsg()
        //        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay,DelayedMqConfig.routing_delay,"ok",(messag)->{
        //            //  设置消息的过期时间 10 秒
        //            messag.getMessageProperties().setDelay(10000);
        //            System.out.println("发送消息时间：\t"+simpleDateFormat.format(new Date()));
        //            return messag;
        //        });

        rabbitService.sendDelayMsg("DelayedMqConfig.exchange_delay",DelayedMqConfig.routing_delay,"来人了，开始接客了",10);
        return Result.ok();
    }

}