package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * author:atGuiGu-mqx
 * date:2022/5/16 14:29
 * 描述：
 **/
@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    //  http://payment.gmall.com/pay.html?orderId=46
    @GetMapping("pay.html")
    public String pay(HttpServletRequest request){
        //  获取到订单Id
        String orderId = request.getParameter("orderId");
        //  orderInfo
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));
        request.setAttribute("orderInfo",orderInfo);
        //  返回支付信息展示页面
        return "payment/pay";
    }

    //  pay/success.html
    @GetMapping("pay/success.html")
    public String paySuccess(){
        //  返回支付成功页面
        return "payment/success";
    }
}