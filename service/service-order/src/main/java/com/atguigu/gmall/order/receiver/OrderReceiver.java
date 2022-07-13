package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.order.ReceiverExLogs;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.mapper.ReceiverExLogsMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * author:atGuiGu-mqx
 * date:2022/5/16 10:55
 * 描述
 **/
@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    @Autowired
    private ReceiverExLogsMapper receiverExLogsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    //  监听取消订单的消息.
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel) throws IOException {
        //  是否会有一个疑问?  能不能统一调用三个关闭就行了.   在关闭的一瞬间，这个用户付款了。
        //  orderInfo -- paymentInfo -- checkAliPay -- closeAliPay
        try {
            //  判断orderId
            if (orderId!=null){
                //  抛出异常.
                //  int i = 1/0;
                //  实现取消订单 , 判断当前的支付状态. 只有订单状态是未支付才能取消。
                //  声明key
                String orderCancelKey = "orderCancel:"+orderId;
                //  判断当前key 是否存在   value = 0 : 表示消费未成功  value = 1: 表示消费成功
                Boolean exist = this.redisTemplate.opsForValue().setIfAbsent(orderCancelKey, "0", 10, TimeUnit.MINUTES);
                //  第一次执行的时候 true: 表示执行成功 你是第一个消费者！
                //  第二次执行的时候 false: 表示key 已经存在过，但是他一定消费成功了么？不确定的
                if (!exist){
                    //  根据key 来获取  value = 0 : 表示消费未成功  value = 1: 表示消费成功
                    String status = (String) this.redisTemplate.opsForValue().get(orderCancelKey);
                    if ("1".equals(status)){
                        //  手动确认有人消费过了.
                        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                        return;
                    }else {
                        //  消费的业务逻辑.
                    }
                }
                OrderInfo orderInfo = orderService.getById(orderId);
                if (orderInfo!=null && "UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())){
                    //  判断当前是否在电商系统中产生了交易记录.
                    PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    //  有电商系统交易记录，同时是未付款状态.
                    if (paymentInfo!=null && "UNPAID".equals(paymentInfo.getPaymentStatus())){
                        //  判断用户是否与支付宝产生了交易记录.
                        Boolean result = this.paymentFeignClient.checkPayment(orderId);
                        //  判断
                        if (result){
                            //  有交易记录,说明用户扫码了二维码.
                            Boolean flag = this.paymentFeignClient.closePay(orderId);
                            //  判断
                            if(flag){
                                //  flag = true; 说明关闭成功。未付款。
                                //  62 行关闭了Alipay, 关闭 orderInfo + paymentInfo;
                                orderService.execExpiredOrder(orderId,"2");
                            }else {
                                //  flag = false; 说明关闭失败，付款了。 自动走异步回调！
                            }
                        }else {
                            //  没交易记录 , 说明没有扫码二维码.
                            //  需要关闭 orderInfo + paymentInfo;
                            orderService.execExpiredOrder(orderId,"2");
                        }
                    }else {
                        //  调用取消订单方法. 只需要关闭orderInfo ，不需要关闭paymentInfo;
                        orderService.execExpiredOrder(orderId,"1");
                    }
                }
                //  如果消费成功：
                this.redisTemplate.opsForValue().set(orderCancelKey,"1");
                //  手动确认
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            }
        } catch (Exception e) {
            //  记录哪条消息消费异常.
            String uuid = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");

            //  在进行数据插入的时候：能否插入多条数据进去！
            //  orderId = 101   orderId = 101   orderId = 101
            QueryWrapper<ReceiverExLogs> receiverExLogsQueryWrapper = new QueryWrapper<>();
            receiverExLogsQueryWrapper.eq("order_id", orderId);
            Integer count = receiverExLogsMapper.selectCount(receiverExLogsQueryWrapper);
            if (count>0){
                return;
            }
            //  创建对象
            ReceiverExLogs receiverExLogs = new ReceiverExLogs();
            receiverExLogs.setOrderId(orderId);
            receiverExLogs.setContent("取消订单业务");
            receiverExLogs.setExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL);
            receiverExLogs.setRoutingKey(MqConst.ROUTING_ORDER_CANCEL);
            receiverExLogs.setMessage(orderId.toString());
            receiverExLogs.setMessageId(uuid); // 消息对应的Id
            receiverExLogsMapper.insert(receiverExLogs);
            e.printStackTrace();
        }finally {
            //  手动确认
            //  channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }

    }

    //  监听支付发送过来的消息.
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void updateOrderStatus(Long orderId,Message message,Channel channel){
        try {
            //  判断orderId
            if (orderId!=null){
                //  更新订单状态.
                this.orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                //  发送一个消息给库存系统.
                this.orderService.sendOrderStatus(orderId);
            }
        } catch (Exception e) {
            //  异常信息表.
            e.printStackTrace();
        }
        //  手动确认.
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }

    //  监听库存系统发过来的减库存消息。  也可以写队列因为 队列  中 queue.ware.order 已经存在.
    //    @RabbitListener(queues = MqConst.QUEUE_WARE_ORDER)
    //    public void queueWareOrder(String wareJson ,Message message,Channel channel){
    //
    //    }
    //  如果队列中不存在 bindings 就需要写绑定！
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void queueWareOrder(String wareJson ,Message message,Channel channel){
        try {
            //   wareJson = {"orderId":"70","status":"DEDUCTED"}
            //  将wareJson 字符串变为能操作对象.
            if (!StringUtils.isEmpty(wareJson)){
                Map map = JSON.parseObject(wareJson, Map.class);
                String orderId = (String) map.get("orderId");
                String status = (String) map.get("status");

                //  判断状态.
                if ("DEDUCTED".equals(status)){
                    //  扣减成功. 修改订单状态.
                    this.orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);
                }else {
                    //  扣减失败. 修改订单状态。 订单状态是已支付，进度状态是超卖.
                    this.orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
                    //  记录日志信息，或消费异常信息表； 记住那个订单Id 减库存异常了.
                    //  如果发生了超卖，如何操作?
                    //  1.  补货：{再次手动更新一下库存消息}    2. 人工客服:  调用退款接口了。
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        //  手动确认.
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}