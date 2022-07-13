package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.response.AlipayTradePagePayResponse;

/**
 * author:atGuiGu-mqx
 * date:2022/5/17 9:25
 * 描述：
 **/
public interface AlipayService {

    //  生成二维码！ 获取到二维码的表单.
    //  http://api.gmall.com/api/payment/alipay/submit/47
    String createAliPay(Long orderId) throws AlipayApiException;

    /**
     * 退款接口.
     * @param orderId
     * @return
     */
    Boolean refund(Long orderId);

    /**
     * 关闭支付宝交易记录.
     * @param orderId
     * @return
     */
    Boolean closeAliPay(Long orderId);

    /**
     * 查询交易记录接口
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);


}