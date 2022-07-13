package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * author:atGuiGu-mqx
 * date:2022/5/11 14:44
 * 描述：
 **/
@Controller
public class CartController {

    @Autowired
    private ProductFeignClient productFeignClient;

    //  http://cart.gmall.com/addCart.html?skuId=23&skuNum=1
    @GetMapping("addCart.html")
    public String addCart(HttpServletRequest request){
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        //  后台：skuInfo，skuNum
        SkuInfo skuInfo = productFeignClient.getSkuInfo(Long.parseLong(skuId));
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        //  返回添加购物车成功页面.
        return "cart/addCart";
    }

    //  购物车列表控制器：
    @GetMapping("cart.html")
    public String cartList(){

        //  返回购物车列表页面
        return "cart/index";
    }

}