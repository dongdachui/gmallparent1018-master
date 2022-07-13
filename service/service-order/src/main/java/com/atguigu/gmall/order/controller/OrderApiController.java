package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * author:atGuiGu-mqx
 * date:2022/5/13 10:28
 * 描述：
 **/
@RestController
@RequestMapping("api/order")
public class OrderApiController {


    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    //  远程调用路径.
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request){
        //  获取用户Id ： 这个用户Id 一定是登录的！ 为什么这么说? 网关做了鉴权.
        String userId = AuthContextHolder.getUserId(request);
        //  声明一个map 集合
        HashMap<String, Object> hashMap = new HashMap<>();
        //  获取用户的收货地址列表.
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        //  获取送货清单： detailArrayList
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        //  声明一个订单明细集合
        //        ArrayList<OrderDetail> orderDetailArrayList = new ArrayList<>();
        //        //  订单相关的表：orderInfo orderDetail
        //        for (CartInfo cartInfo : cartCheckedList) {
        //            //  创建订单明细对象.
        //            OrderDetail orderDetail = new OrderDetail();
        //            //  赋值一个skuId
        //            orderDetail.setSkuId(cartInfo.getSkuId());
        //            orderDetail.setSkuNum(cartInfo.getSkuNum());
        //            orderDetail.setSkuName(cartInfo.getSkuName());
        //            orderDetail.setImgUrl(cartInfo.getImgUrl());
        //            //  表示订单的价格 : 初始化的时候给实时价格！
        //            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
        //            orderDetailArrayList.add(orderDetail);
        //        }
        //  保证原子性
        AtomicInteger totalNum = new AtomicInteger();
        List<OrderDetail> orderDetailArrayList = cartCheckedList.stream().map(cartInfo -> {
            //  创建订单明细对象.
            OrderDetail orderDetail = new OrderDetail();
            //  赋值一个skuId
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            //  表示订单的价格 : 初始化的时候给实时价格！
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            //  总数量：
            totalNum.addAndGet(cartInfo.getSkuNum());
            return orderDetail;
        }).collect(Collectors.toList());
        //  总金额：
        OrderInfo orderInfo = new OrderInfo();
        //  订单明细集合放入orderInfo 中.
        orderInfo.setOrderDetailList(orderDetailArrayList);
        //  计算总金额
        orderInfo.sumTotalAmount();
        //  向map 中保存数据.
        hashMap.put("userAddressList",userAddressList);
        //  能否直接将购物车集合保存, 需要看页面渲染。
        hashMap.put("detailArrayList",orderDetailArrayList);
        hashMap.put("totalNum",totalNum);
        hashMap.put("totalAmount",orderInfo.getTotalAmount());
        //  tradeNo
        hashMap.put("tradeNo",this.orderService.getTradeNo(userId));
        //  返回数据.
        return Result.ok(hashMap);
    }

    //  http://api.gmall.com/api/order/auth/submitOrder?tradeNo=null
    //  tradeNo：流水号.
    //  第一个保存时 页面传递的参数  Json --> JavaObject
    //  第二个服务层方法的返回值
    @PostMapping("auth/submitOrder")
    public Result saveOrderInfo(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        //  获取到用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        //  获取页面传递的流水号
        String tradeNo = request.getParameter("tradeNo");
        //  调用服务层方法.
        Boolean result = this.orderService.checkTradeNo(tradeNo,userId);
        if (!result){
            //  比较失败。
            return Result.fail().message("不能重复无刷新回退提交订单.");
        }
        //  删除缓存的流水号 ,当第二次回退无刷新页面，缓存的流水号就是null。 再提交就比较失败了。
        this.orderService.delTradeNo(userId);

        //  校验下订单的库存数量.
        //  http://localhost:9001/hasStock?skuId=10221&num=2 校验库存.
        //  声明一个集合来存储提示信息！
        List<String> errorList = new ArrayList<>();
        //  创建一个异步编排的集合！
        List<CompletableFuture> futureList = new ArrayList<>();
        //  CompletableFuture.runAsync()  // 没有返回值的 将错误的提示信息放入集合中，不需要返回值.
        //  CompletableFuture.supplyAsync(); // 有返回值 orderInfo.getOrderDetailList() 有三条数据
        for (OrderDetail orderDetail : orderInfo.getOrderDetailList()) {
            //  编写多线程： 校验库存.
            CompletableFuture<Void> stockCompletableFuture = CompletableFuture.runAsync(() -> {
                //  调用校验库存方法.
                boolean exist = this.orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!exist) {
                    //  将错误提示信息添加到集合中.
                    errorList.add(orderDetail.getSkuName() + "库存不足.");
                }
            },threadPoolExecutor);

            //  将线程添加到集合中.
            futureList.add(stockCompletableFuture);
            //  编写多线程校验价格：
            CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
                //  校验价格： 如果价格有变动了，我需要将变动的商品对应价格要做一次更新！  将最新价格给购物车。
                //  获取商品的实时价格. 没有走缓存的.
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                //  订单页面中的商品价格
                BigDecimal orderPrice = orderDetail.getOrderPrice();
                //  购物车的key
                String cartKey = RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
                //  判断 A.compareTo(B)
                //  获取两个数值的差
                BigDecimal price = skuPrice.subtract(orderPrice).abs();
                if (skuPrice.compareTo(orderPrice) == 1) {
                    updateSkuPrice(orderDetail, skuPrice, cartKey);
                    //  说明价格有变动.
                    errorList.add(orderDetail.getSkuName() + "价格涨价" + price + "元");
                    // skuPrice>orderPrice上涨
                } else if (skuPrice.compareTo(orderPrice) == -1) {
                    //  知道谁价格有变动！
                    updateSkuPrice(orderDetail, skuPrice, cartKey);
                    // orderPrice<skuPrice下降
                    errorList.add(orderDetail.getSkuName() + "价格降价" + price + "元");
                } else {
                    //  价格没变.
                }
            },threadPoolExecutor);
            //  将校验价格的线程添加到集合中
            futureList.add(priceCompletableFuture);
        }
        //  多任务组合： 我们现在所有的线程都在futureList集合中,  allof 集合变数组.
        //  int [] arrays = new int [4];
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();

        //  错误信息提示：
        if (errorList.size()>0){
            //  有错误，统一提示错误信息.
            return Result.fail().message(StringUtils.join(errorList,","));
        }
        //  调用服务层方法.
        Long orderId = this.orderService.saveOrderInfo(orderInfo);
        //  返回数据.
        return Result.ok(orderId);
    }

    //  更新购物车价格
    private void updateSkuPrice(OrderDetail orderDetail, BigDecimal skuPrice, String cartKey) {
        //  知道谁价格有变动！
        CartInfo cartInfo = (CartInfo) this.redisTemplate.opsForHash().get(cartKey, orderDetail.getSkuId().toString());
        cartInfo.setSkuPrice(skuPrice);
        //  放入购物车.更新了购物车数据.
        this.redisTemplate.opsForHash().put(cartKey, orderDetail.getSkuId().toString(), cartInfo);
    }

    //  查看我的订单：
    @GetMapping("/auth/{page}/{limit}")
    public Result getOrderPage(@PathVariable Long page,
                               @PathVariable Long limit,
                               HttpServletRequest request){
        //  获取到用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  声明一个 page 对象
        Page<OrderInfo> orderInfoPage = new Page<>(page, limit);

        //  调用服务层方法查询数据：第二个参数？
        IPage<OrderInfo> infoIPage = this.orderService.getOrderPage(orderInfoPage,userId);
        //  返回数据
        return Result.ok(infoIPage);
    }

    //  根据订单Id 查询订单对象。
    @GetMapping("/inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){
        //  调用服务层方法.
        return this.orderService.getOrderInfo(orderId);
    }

    //  拆单接口： http://localhost:8204/api/order/orderSplit?orderId=xxx&wareSkuMap=[{"wareId":"1","skuIds":["21","22"]},{"wareId":"2","skuIds":["24"]}]
    //  参数：orderId，wareSkuMap： [{"wareId":"1","skuIds":["21","22"]},{"wareId":"2","skuIds":["24"]}]
    @PostMapping("orderSplit")
    public String orderSplit(HttpServletRequest request){
        //  获取传递的参数.
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");

        // 调用服务层方法 ,返回子订单集合
        List<OrderInfo> subOrderInfoList = this.orderService.orderSplit(orderId,wareSkuMap);
        //  orderInfo 转换为 Map 变化为Json。
        List<Map> mapList = subOrderInfoList.stream().map(orderInfo -> {
            Map map = this.orderService.initWareOrder(orderInfo);
            return map;
        }).collect(Collectors.toList());
        //  返回Json
        return JSON.toJSONString(mapList);
    }

    //  秒杀订单控制器：
    @PostMapping("/inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo){
        //  调用服务层方法.
        Long orderId = this.orderService.saveOrderInfo(orderInfo);
        //  返回保存订单Id
        return orderId;
    }



}