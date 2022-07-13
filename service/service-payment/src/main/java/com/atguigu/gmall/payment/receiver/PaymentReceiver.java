package com.atguigu.gmall.payment.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.payment.service.PaymentService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * author:atGuiGu-mqx
 * date:2022/5/18 14:09
 * 描述：
 **/
@Component
public class PaymentReceiver {


    @Autowired
    private PaymentService paymentService;


    //  监听订单发送的取消订单业务消息。
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_CLOSE,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE),
            key = {MqConst.ROUTING_PAYMENT_CLOSE}
    ))
    public void closePayment(Long orderId, Message message, Channel channel){
        try {
            //  判断
            if (orderId!=null){
                //  调用关闭交易记录.
                this.paymentService.closePayment(orderId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  手动确认消息.
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);


    }
}