package com.atguigu.gmall.order.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.impl.OrderDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/5/13 11:13
 * 描述：
 **/
@FeignClient(value = "service-order",fallback = OrderDegradeFeignClient.class)
public interface OrderFeignClient {

    //  发布数据接口到feign 上. 订单显示页面！
    //  Result trade(HttpServletRequest request)
    //  远程调用的时候细节：微服务之间通过feign 远程调用 是不传递 header 信息的！ 如果不传递头文件信息，就获取不到userId. 写一个拦截器！
    @GetMapping("/api/order/auth/trade")
    Result<Map<String, Object>> trade();


    //  根据订单Id 查询订单对象
    @GetMapping("/api/order/inner/getOrderInfo/{orderId}")
    OrderInfo getOrderInfo(@PathVariable Long orderId);

    /**
     * 秒杀订单.
     * @param orderInfo
     * @return
     */
    @PostMapping("/api/order/inner/seckill/submitOrder")
    Long submitOrder(@RequestBody OrderInfo orderInfo);
}