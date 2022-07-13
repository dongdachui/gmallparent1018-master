package com.atguigu.gmall.payment.client;

import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.impl.PaymentDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * author:atGuiGu-mqx
 * date:2022/5/18 15:28
 * 描述：
 **/
@FeignClient(value = "service-payment",fallback = PaymentDegradeFeignClient.class)
public interface PaymentFeignClient {

    //  发布数据接口:

    /**
     * 关闭支付宝交易记录.
     * @param orderId
     * @return
     */
    @GetMapping("api/payment/alipay/closePay/{orderId}")
    Boolean closePay(@PathVariable Long orderId);

    /**
     * 查询支付宝交易记录.
     * @param orderId
     * @return
     */
    @GetMapping("api/payment/alipay/checkPayment/{orderId}")
    Boolean checkPayment(@PathVariable Long orderId);

    /**
     * 查询是否有本地交易记录.
     * @param outTradeNo
     * @return
     */
    @GetMapping("api/payment/alipay/getPaymentInfo/{outTradeNo}")
    PaymentInfo getPaymentInfo(@PathVariable String outTradeNo);

}
