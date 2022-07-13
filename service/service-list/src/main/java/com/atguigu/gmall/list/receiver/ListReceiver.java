package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * author:atGuiGu-mqx
 * date:2022/5/14 15:30
 * 描述：
 **/
@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    //  监听消息：
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upperGoods(Long skuId, Message message, Channel channel) throws IOException {
        //  判断
        try {
            if (skuId!=null){
                //  调用商品上架的方法.
                searchService.onSale(skuId);
            }
            //  手动确认消息.
        } catch (Exception e) {
            e.printStackTrace();
            //  第一种：
            //  nack: 重回队列.? 几次? 如果不控制器，会一直监听，一直重回，需要借助redis 来控制器重回次数！
            //  redis: count == 3;  重回了三次还没有被消费，则不重回对了. int count = 0 ; count++;
            //  channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            //  第二种：
            //  日志记录表：skuId ，的那个消息出错了.   insert into logs values (24,'upperGoods');
        }
        //  不重队列。
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  商品下架：
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void lowerGoods(Long skuId, Message message, Channel channel) throws IOException {
        //  判断
        try {
            if (skuId!=null){
                //  调用商品上架的方法.
                searchService.cancelSale(skuId);
            }
            //  手动确认消息.
        } catch (Exception e) {
            e.printStackTrace();
            //  日志记录表：skuId ，的那个消息出错了.   insert into logs values (24,'upperGoods');

        }
        //  不重队列。
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}