package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.model.order.ReceiverExLogs;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * author:atGuiGu-mqx
 * date:2022/5/14 14:28
 * 描述：
 **/
@Component
public class ConfirmReceiver {


    //  如何监听：
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm",durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm"),
            key = {"routingKey.confirm"}
    ))
    public void getMsg(String messages, Message message, Channel channel){
        try {
            //  接收到消息
            System.out.println("接收消息："+messages);

            //  int i = 1/0;
            //  System.out.println("接收消息："+new String(message.getBody()));
            //  消息的 确认  false: 表示单个消息的确认，true: 表示批量确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) { //IoException
            //  第三个参数表示重回队列.
            //  channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);

            e.printStackTrace();
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  监听消息：
    @SneakyThrows
    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    public void getDeadLetterMessage(String msg,Message message,Channel channel){
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //  接收消息.
            System.out.println("接收到的消息：\t"+msg);
            System.out.println("接收消息时间：\t"+simpleDateFormat.format(new Date()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //  手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  监听基于插件的延迟消息.
    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    public void getDelayMsg(String msg,Message message,Channel channel) throws IOException {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //  接收消息.
            System.out.println("接收到的消息：\t"+msg);
            System.out.println("接收消息时间：\t"+simpleDateFormat.format(new Date()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}