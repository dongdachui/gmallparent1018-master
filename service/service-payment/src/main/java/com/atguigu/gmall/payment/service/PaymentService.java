package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/5/16 15:31
 * 描述：
 **/
public interface PaymentService {

    //  保存交易记录.payment_info 数据来源大部分都是 order_info;
    void savePaymentInfo(OrderInfo orderInfo,String paymentType);

    /**
     * 获取交易记录.
     * @param outTradeNo
     * @param paymentType
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String paymentType);

    /**
     * 更新交易记录。
     * @param outTradeNo
     * @param paymentType
     * @param paramsMap
     */
    void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramsMap);

    /**
     * 修改交易记录状态
     * @param outTradeNo
     * @param paymentType
     * @param paymentInfo
     */
    void updatePaymentInfo(String outTradeNo, String paymentType, PaymentInfo paymentInfo);

    /**
     * 关闭交易记录.
     * @param orderId
     */
    void closePayment(Long orderId);
}