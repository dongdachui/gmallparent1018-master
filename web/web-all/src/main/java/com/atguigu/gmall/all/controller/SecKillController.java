package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/5/20 14:15
 * 描述：
 **/
@Controller
public class SecKillController {

    @Autowired
    private ActivityFeignClient activityFeignClient;

    //  http://activity.gmall.com/seckill.html
    @GetMapping("seckill.html")
    public String secKillList(HttpServletRequest request){
        //  后台${list}
        Result result = activityFeignClient.findAll();
        //  result.getData() = List<SeckillGoods>
        request.setAttribute("list",result.getData());
        //  返回秒杀列表页面
        return "seckill/index";
    }

    //  http://activity.gmall.com/seckill/24.html
    //  秒杀详情控制器！
    @GetMapping("seckill/{skuId}.html")
    public String secKillItem(@PathVariable Long skuId,HttpServletRequest request){
        //  ${item}
        Result result = this.activityFeignClient.getSeckillGoods(skuId);
        //  result.getData() = SeckillGoods
        request.setAttribute("item",result.getData());
        //  返回详情页面
        return "seckill/item";
    }

    //  /seckill/queue.html?skuId=xxx&skuIdStr=xxx 这个控制器！
    //  来到排队页面！
    @GetMapping("/seckill/queue.html")
    public String secKillQueue(HttpServletRequest request){
        //  存储数据
        request.setAttribute("skuId",request.getParameter("skuId"));
        request.setAttribute("skuIdStr",request.getParameter("skuIdStr"));
        //  获取到下单码，来到排队页面.
        return "seckill/queue";
    }

    //  来到去下单页面！
    @GetMapping("/seckill/trade.html")
    public String trade(Model model){
        //  后台存储：userAddressList， detailArrayList，totalNum，totalAmount
        //        Result<Map> result = this.activityFeignClient.trade();
        //        //  存储map 数据.
        //        model.addAllAttributes(result.getData());
        //
        //        // model.addAttribute(); 也可以存储单个值.
        //        //  返回去下单页面.
        //        return "seckill/trade";

        //  如果在控制器中想要判断 是否可以展示正确页面.
        Result<Map> result = this.activityFeignClient.trade();
        //  判断返回是不是 200
        if (result.isOk()){
            model.addAllAttributes(result.getData());
            return "seckill/trade";
        }else {
            model.addAttribute("message","展示失败.");
            //  存储一个message
            return "seckill/fail";
        }
    }
}