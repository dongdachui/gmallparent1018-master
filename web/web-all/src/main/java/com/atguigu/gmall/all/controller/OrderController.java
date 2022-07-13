package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/5/13 10:18
 * 描述：
 **/
@Controller
public class OrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    //  http://order.gmall.com/trade.html
    @GetMapping("trade.html")
    public String trade(Model model, HttpServletRequest request){
        //  userAddressList: 用户收货地址列表 detailArrayList：订单明细  totalNum：商品件数  totalAmount：订单总金额 , tradeNo: 流水号
        Result<Map<String, Object>> result = orderFeignClient.trade();
        //  存储单个值
        //  model.addAttribute("list","集合");
        //  request.setAttribute("name","华子");
        model.addAllAttributes(result.getData());
        //  返回视图
        return "order/trade";
    }

    //  http://order.gmall.com/myOrder.html
    //  我的订单：
    @GetMapping("myOrder.html")
    public String myOrder(){
        //  不需要存储数据，因为它是通过  /api/order/auth/${page}/${limit}
        //  返回我的订单页面视图名称
        return "order/myOrder";
    }
}