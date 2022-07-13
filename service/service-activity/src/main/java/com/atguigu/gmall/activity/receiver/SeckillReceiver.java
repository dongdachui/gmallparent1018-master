package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * author:atGuiGu-mqx
 * date:2022/5/20 10:13
 * 描述：
 **/
@Component
public class SeckillReceiver {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    //  监听定时任务发送过来的消息.
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importDataToRedis(Message message, Channel channel){
        try {
            //  根据业务查询哪些是属于秒杀商品.    new Date();  stock_count > 0; status 1:表示审核通过.
            //  select date_format(start_time,'%Y-%m-%d')  from seckill_goods;
            QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
            seckillGoodsQueryWrapper.eq("status",1);
            seckillGoodsQueryWrapper.gt("stock_count",0);
            //  系统时间也应该获取年月日：  select date_format(start_time,'%Y-%m-%d') startTime  from seckill_goods;
            seckillGoodsQueryWrapper.eq("date_format(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
            //  此时查询到了当天要秒杀的商品.
            List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
            //  判断集合
            if (!CollectionUtils.isEmpty(seckillGoodsList)){
                //  将数据放入缓存.
                for (SeckillGoods seckillGoods : seckillGoodsList) {
                    //  存储到缓存； 数据类型. Hash  hset key field value
                    //  存储秒杀商品的 key  =  seckill:goods  field = skuId; value = seckillGoods
                    String secKillGoodsKey = RedisConst.SECKILL_GOODS;
                    //  缓存中有这个数据，则跳过不存储，如果没有才会将数据存储到缓存。 避免出现数据覆盖.
                    Boolean exist = this.redisTemplate.opsForHash().hasKey(secKillGoodsKey, seckillGoods.getSkuId().toString());
                    if (exist){
                        //  如果存在当前秒杀商品，则跳过； 不能写break;  return;
                        continue;
                    }
                    //  存储数据
                    this.redisTemplate.opsForHash().put(secKillGoodsKey,seckillGoods.getSkuId().toString(),seckillGoods);
                    //  存储一个商品的剩余数量.  存储到 list 中.  目的：防止超卖！
                    //  10个剩余商品数量.
                    for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                        //  key = seckill:stock:skuId  value = skuId.toString();
                        this.redisTemplate.opsForList().leftPush(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId(),seckillGoods.getSkuId().toString());
                    }

                    //  将每个商品的状态位定义为 1  publish seckillpush 24:1
                    this.redisTemplate.convertAndSend("seckillpush",seckillGoods.getSkuId()+":1");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //  手动确认消息.
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  监听秒杀时传递的消息： this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_SECKILL_USER,MqConst.ROUTING_SECKILL_USER,userRecode);
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void secKillUser(UserRecode userRecode , Message message, Channel channel){
        try {
            //  判断
            if (userRecode!=null){
                //  业务逻辑:
                seckillGoodsService.seckillOrder(userRecode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  手动确认：
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }

    //  更新库存：         this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_ACTIVITY_STOCK,MqConst.ROUTING_ACTIVITY_STOCK,userRecode.getSkuId());
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ACTIVITY_STOCK,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_ACTIVITY_STOCK),
            key = {MqConst.ROUTING_ACTIVITY_STOCK}
    ))
    public void upateStock(Long skuId,Message message, Channel channel){
        try {
            //  判断
            if(skuId!=null){
                //  更新：mysql；redis
                seckillGoodsService.updateStock(skuId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //  手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  监听秒杀结束消息.
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.QUEUE_TASK_18}
    ))
    public void clearRedisData(Message message,Channel channel){
        try {
            //  删除秒杀商品数据. seckill:goods
            this.redisTemplate.delete(RedisConst.SECKILL_GOODS);
            //  注意预下单数据：    用户有可能走到 【 抢购成功   去下单】 不点击了！ 此时有预下单数据，并没有真正下单数据.
            this.redisTemplate.delete(RedisConst.SECKILL_ORDERS);
            //  删除真正下单的数据. seckill:orders:users
            this.redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);

            //  商品剩余库存数. seckill:stock:skuId
            //  如何获取到skuId? 查询数据库中有哪些秒杀商品.
            //  结束日期：必须是今天的 {相等 , 大于 , 小于};  审核状态是：1
            QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
            //  seckillGoodsQueryWrapper.eq("end_time",new Date());  不合适!
            seckillGoodsQueryWrapper.le("end_time",new Date());
            seckillGoodsQueryWrapper.eq("status",1);
            //  注意查询的条件范围.
            List<SeckillGoods> secKillGoodsList = this.seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
            for (SeckillGoods seckillGoods : secKillGoodsList) {
                //  seckill:stock:24
                this.redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId());
            }
            //  用户下单时的key！ seckill:user:2 ，这个不用手动删除，让它自生自灭！因为这个哥们有过期时间!
        } catch (Exception e) {
            e.printStackTrace();
        }

        //  消息确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }

}