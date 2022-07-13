package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * author:atGuiGu-mqx
 * date:2022/5/20 14:07
 * 描述：
 **/
@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsApiController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderFeignClient orderFeignClient;



    //  定义秒杀列表的控制器
    @GetMapping("/findAll")
    public Result findAll(){
        //  调用服务层方法
        List<SeckillGoods> seckillGoodsList = seckillGoodsService.findAll();
        //  返回数据
        return Result.ok(seckillGoodsList);
    }

    //  根据skuId 来获取到秒杀详情数据
    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable Long skuId){
        //  调用服务层方法.
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoodsById(skuId);
        //  返回数据
        return Result.ok(seckillGoods);
    }

    //  生成下单码：
    // /api/activity/seckill/auth/getSeckillSkuIdStr/{skuId}
    @GetMapping("auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId, HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  判断必须在秒杀开始之后，结束之前生成下单码！
        SeckillGoods seckillGoods = this.seckillGoodsService.getSeckillGoodsById(skuId);
        //  判断有秒杀对象
        if (seckillGoods!=null){
            //  获取到当前系统时间
            Date currentTime = new Date();
            if (DateUtil.dateCompare(seckillGoods.getStartTime(),currentTime) &&
                DateUtil.dateCompare(currentTime,seckillGoods.getEndTime())){
                //  生成下单码： md5(userId)
                String skuIdStr = MD5.encrypt(userId);
                return Result.ok(skuIdStr);
            }
        }
        return Result.fail().message("生成下单码失败.");
    }

    //  秒杀下单控制器！
    //  this.api_name + '/auth/seckillOrder/' + skuId + '?skuIdStr=' + skuIdStr
    @PostMapping("/auth/seckillOrder/{skuId}")
    public Result secKillOrder(@PathVariable Long skuId,HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  skuIdStr 校验下单码.
        String skuIdStr = request.getParameter("skuIdStr");
        //  md5(userId)
        if (!skuIdStr.equals(MD5.encrypt(userId))){
            //  返回信息提示
            return Result.fail().message("校验下单码失败.");
        }
        //  判断状态位 map 中！ skuId:0 skuId:1
        String status = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(status)){ // null
            //  返回信息提示
            return Result.fail().message("状态位是null.");
        }else if ("0".equals(status)){
            //  返回信息提示
            return Result.fail().message("商品已售罄");
        }else {
            //  说明可以秒杀. 1  将userId 与秒杀商品信息放入队列中.
            //  记录谁要秒杀哪个商品！
            UserRecode userRecode = new UserRecode();
            userRecode.setUserId(userId);
            userRecode.setSkuId(skuId);

            //  放入到队列中. 向队列发送消息！ 15672 管控台 暂时还不会有这个队列！ 因为我们还没有设置绑定关系！
            this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_SECKILL_USER,MqConst.ROUTING_SECKILL_USER,userRecode);
            //  正确返回
            return Result.ok();
        }
    }

    //  检查订单状态.
    @GetMapping("/auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable Long skuId,HttpServletRequest request){
        //  调用服务层方法;  提示我的订单 ，必须是当前用户已经下过订单，如果用户下过订单则在缓存中一定会存在key！
        //  根据上述描述，必须要获取到用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  返回检查结果.
        return this.seckillGoodsService.checkOrder(skuId,userId);
    }

    //  秒杀订单页面
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  userAddressList， detailArrayList，totalNum，totalAmount
        HashMap<String, Object> hashMap = new HashMap<>();
        //  获取收货地址列表：
        List<UserAddress> userAddressList = this.userFeignClient.findUserAddressListByUserId(userId);

        //  送货清单：秒杀商品 - 发布一个商品 获取到当前秒杀商品是谁？  我们将秒杀商品存在预下单中.
        //  this.redisTemplate.opsForHash().put(secKillOrderKey,userRecode.getUserId(),orderRecode)
        OrderRecode orderRecode = (OrderRecode) this.redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS, userId);
        //  判断是否有预下单记录.
        if (orderRecode==null){
            return Result.fail().message("没有预下单记录");
        }
        //  获取到了秒杀商品.
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();
        //  detailArrayList表示订单明细的集合：
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();
        //  创建一个订单明细
        OrderDetail orderDetail = new OrderDetail();
        //  给订单明细赋值：
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setSkuNum(orderRecode.getNum());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        //  此处需要给当前商品的秒杀价格！
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        detailArrayList.add(orderDetail);

        //  存储数据.
        hashMap.put("userAddressList",userAddressList);
        hashMap.put("detailArrayList",detailArrayList);
        hashMap.put("totalNum",orderRecode.getNum());
        hashMap.put("totalAmount",seckillGoods.getCostPrice());
        //  返回数据.
        return Result.ok(hashMap);
    }

    //  秒杀订单控制器：  保存到数据库andredis.
    //  将Json 变为javaObject.
    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        //  获取到userId
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        //  调用服务层方法,返回订单Id.
        //  秒杀保存订单与普通的保存订单有区别?  共同的特点：可以远程调用service-order 的保存方法.
        Long orderId = this.orderFeignClient.submitOrder(orderInfo);
        if (orderId == null){
            return Result.fail().message("提交订单失败.");
        }
        //  写入真实的订单到缓存.
        //  this.redisTemplate.opsForHash().put(seckill:orders:users,userId,orderId.toString());   点击了提交订单
        this.redisTemplate.opsForHash().put(RedisConst.SECKILL_ORDERS_USERS,userId,orderId.toString());
        //  有了真正的订单，此时可以将预下单数据删除了.
        this.redisTemplate.opsForHash().delete(RedisConst.SECKILL_ORDERS,userId);
        //  window.location.href = 'http://payment.gmall.com/pay.html?orderId=' + response.data.data
        //  返回订单Id;
        return Result.ok(orderId);
    }
}