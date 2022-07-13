package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * author:atGuiGu-mqx
 * date:2022/5/20 14:03
 * 描述：
 **/
@Service
public class SeckillGoodsServiceImpl  implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;


    @Override
    public List<SeckillGoods> findAll() {
        //  hvals key；
        String secKillGoodsKey = RedisConst.SECKILL_GOODS;
        return this.redisTemplate.opsForHash().values(secKillGoodsKey);
    }

    @Override
    public SeckillGoods getSeckillGoodsById(Long skuId) {
        //  hget key field
        String secKillGoodsKey = RedisConst.SECKILL_GOODS;
        SeckillGoods seckillGoods = (SeckillGoods) this.redisTemplate.opsForHash().get(secKillGoodsKey, skuId.toString());
        return seckillGoods;
    }

    @Override
    public void seckillOrder(UserRecode userRecode) {
        /*
        1. 先判断商品的状态
        2. 判断用户是否下过订单    true:  去看我的订单    false: 去下单
        3. 获取队列中的剩余库存    redis -- list     true: 可以秒杀    false: 不可以秒杀 通知其他兄弟节点改状态位
        4. 更新一下商品的剩余库存数量    mysql ,redis 都有存储！
        5. 将预下订单记录存储到缓存
         */
        //  获取状态位
        String status = (String) CacheHelper.get(userRecode.getSkuId().toString());
        if (StringUtils.isEmpty(status) || "0".equals(status)){
            return;
        }

        //  判断用户是否下过订单   setnx 来控制！ key = seckill:user:userId
        String secKillUserKey = RedisConst.SECKILL_USER+userRecode.getUserId();
        Boolean exist = this.redisTemplate.opsForValue().setIfAbsent(secKillUserKey, userRecode.getUserId(), RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        //  exist = true : 表示生效 说明当前用户是第一次秒杀 ， 如果exist = false 说明不生效，当前用户已经在缓存中存在，可能是第二次下单购买.
        if (!exist){
            return;
        }

        //  获取队列中的剩余库存    redis -- list     true: 可以秒杀    false: 不可以秒杀 通知其他兄弟节点改状态位
        //  存储数据的时候 key = seckill:stock:skuId  value = skuId.toString();
        String stockKey = RedisConst.SECKILL_STOCK_PREFIX+userRecode.getSkuId();
        String stockSkuId = (String) this.redisTemplate.opsForList().rightPop(stockKey);
        //  判断是否能够获取到数据  如果为空说明 不可以秒杀
        if (StringUtils.isEmpty(stockSkuId)) {
            //  通知其他兄弟节点改状态位
            this.redisTemplate.convertAndSend("seckillpush",userRecode.getSkuId()+":0");
            return;
        }

        //  更新库存： 同步更新 updateStock(){先更新mysql，在更新redis!} 效率慢。  异步更新：
        this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_ACTIVITY_STOCK,MqConst.ROUTING_ACTIVITY_STOCK,userRecode.getSkuId());

        //  保存预下单数据到缓存.
        OrderRecode orderRecode = new OrderRecode();
        //  记录哪个用户
        orderRecode.setUserId(userRecode.getUserId());
        //  记录用户秒杀的是哪件商品
        orderRecode.setSeckillGoods(this.getSeckillGoodsById(userRecode.getSkuId()));
        //  记录用户购买的件数
        orderRecode.setNum(1);
        //  记录用户下单码
        orderRecode.setOrderStr(MD5.encrypt(userRecode.getUserId()+userRecode.getSkuId()));
        //  将其存储到缓存中. hset key field value;
        String secKillOrderKey = RedisConst.SECKILL_ORDERS;
        this.redisTemplate.opsForHash().put(secKillOrderKey,userRecode.getUserId(),orderRecode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long skuId) {
        //  mysql redis
        //  获取剩余库存数 ： redis-list 的集合长度
        Long count = this.redisTemplate.opsForList().size(RedisConst.SECKILL_STOCK_PREFIX + skuId);
        //  更新mysql；
        //  获取缓存中的数据 : Id 不为空
        SeckillGoods seckillGoods = this.getSeckillGoodsById(skuId);
        seckillGoods.setStockCount(count.intValue());
        this.seckillGoodsMapper.updateById(seckillGoods);

        //  更新redis;  SECKILL_ORDERS ---> SECKILL_GOODS
        this.redisTemplate.opsForHash().put(RedisConst.SECKILL_GOODS,skuId.toString(),seckillGoods);

    }

    @Override
    public Result checkOrder(Long skuId, String userId) {
        /*
            1.  判断用户是否在缓存中存在
            2.  判断用户是否抢单成功
            3.  判断用户是否下过订单
            4.  判断状态位
         */
        String userSecKillKey = RedisConst.SECKILL_USER+userId;
        Boolean exist = this.redisTemplate.hasKey(userSecKillKey);
        //  只有用户Id 存在的情况下才有可能抢购成功.
        if (exist){
            //  如果有预下单记录。
            Boolean result = this.redisTemplate.opsForHash().hasKey(RedisConst.SECKILL_ORDERS, userId);
            //  result = true;  有预下单记录，则证明你抢购成功！
            if (result){
                //  body 可以是缓存的数据 hget key field;
                OrderRecode orderRecode = (OrderRecode) this.redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS, userId);
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }
        //  提示我的订单！ 说明这个用户已经做个预下单了，同时还将数据保存到了数据库中 , 还保存到了缓存中！
        //  预下单： this.redisTemplate.opsForHash().put(seckill:orders,userId,orderRecode);        点击了立即抢购：
        //  真正下单： this.redisTemplate.opsForHash().put(seckill:orders:users,userId,orderId.toString());   点击了提交订单
        Boolean flag = this.redisTemplate.opsForHash().hasKey(RedisConst.SECKILL_ORDERS_USERS, userId);
        //  flag = true;    说明用户是点击过提交订单的人！
        if (flag){
            //  获取缓存中的订单Id
            String orderId = (String) this.redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS_USERS, userId);
            return Result.build(orderId, ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }

        //  获取状态位
        String status = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(status) || "0".equals(status)){
            //  如果状态位是null 或 0 提示请求不合法。
            return Result.build(null,ResultCodeEnum.SECKILL_ILLEGAL);
        }
        //  默认返回
        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }
}
