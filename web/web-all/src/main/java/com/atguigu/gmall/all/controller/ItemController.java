package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/4/29 14:03
 * 描述：
 **/
@Controller
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;

    //  http://item.gmall.com/24.html
    //  http://localhost:8300/24.html
    //  编写商品详情页面访问的url！
    @GetMapping("{skuId}.html")
    public String item(@PathVariable Long skuId, Model model){
        //  远程调用
        Result<Map> result = itemFeignClient.getItem(skuId);
        //  存储数据
        model.addAllAttributes(result.getData());
        //  model.addAttribute(result.getData());

        //  返回商品想详情页面
        return "item/item";
    }
}