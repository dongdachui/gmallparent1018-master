package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/5/16 15:34
 * 描述：
 **/
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;


    /**
     * 
     * @param orderInfo     订单对象
     * @param paymentType   PaymentType.ALIPAY.name()
     */
    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {

        //  当前交易记录表中能否有两条一模一样的数据.
        //   order_id payment_type 这个两个字段，起到一个联合主键的作用！
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id",orderInfo.getId());
        paymentInfoQueryWrapper.eq("payment_type", paymentType);

        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        if (paymentInfoQuery!=null){
            return;
        }

        //  创建一个paymentInfo 对象
        PaymentInfo paymentInfo = new PaymentInfo();
        //  赋值操作
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());  // 后续生成二维码的时候 ，千万别给真实数据 。orderInfo.getTotalAmount() 9896
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());

        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {
        //  select * from payment_info where out_trade_no = ? and payment_type = ?;
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type",paymentType);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        return paymentInfo;
    }

    @Override
    public void updatePaymentInfo(String outTradeNo, String paymentType, PaymentInfo paymentInfo) {
        UpdateWrapper<PaymentInfo> paymentInfoUpdateWrapper = new UpdateWrapper<>();
        paymentInfoUpdateWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoUpdateWrapper.eq("payment_type",paymentType);
        paymentInfoMapper.update(paymentInfo,paymentInfoUpdateWrapper);
    }

    @Override
    public void closePayment(Long orderId) {
        //
        //  必须在 payment_info 存在orderId 才会关闭. 如果这个表 都存在 orderId ，就不需要关闭.
        //  更新payment_status 状态
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
        //  更新条件
        UpdateWrapper<PaymentInfo> paymentInfoUpdateWrapper = new UpdateWrapper<>();
        paymentInfoUpdateWrapper.eq("order_id",orderId);
        this.paymentInfoMapper.update(paymentInfo,paymentInfoUpdateWrapper);
    }

    /**
     * 支付成功，修改交易记录的状态.
     * @param outTradeNo
     * @param paymentType
     * @param paramsMap
     */
    @Override
    public void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramsMap) {
        //  查询paymentInfo
        PaymentInfo paymentInfoQuery = this.getPaymentInfo(outTradeNo, paymentType);
        if (paymentInfoQuery==null){
            return;
        }

        //  第一个异步回调进来.....  执行修改.
        //  update payment_info set trade_no =?, payment_status=?, callback_time=?, callback_content=? where out_trade_no = ? and payment_type = ? and is_deleted = 0;
        try {
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setTradeNo(paramsMap.get("trade_no"));
            paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setCallbackContent(paramsMap.toString());

            UpdateWrapper<PaymentInfo> paymentInfoUpdateWrapper = new UpdateWrapper<>();
            paymentInfoUpdateWrapper.eq("out_trade_no",outTradeNo);
            paymentInfoUpdateWrapper.eq("payment_type",paymentType);
            this.paymentInfoMapper.update(paymentInfo,paymentInfoUpdateWrapper);

            //  发送消息通知订单要修改订单状态.
            this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());
        } catch (Exception e) {
            //  如果有异常. 由于网络抖动导致修改失败.
            this.redisTemplate.delete(paramsMap.get("notify_id"));
            e.printStackTrace();
        }
    }
}