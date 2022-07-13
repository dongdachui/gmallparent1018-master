package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * author:atGuiGu-mqx
 * date:2022/5/4 9:22
 * 描述：
 **/
@Aspect  // 利用注解开启aop
@Component // 注入到spring容器
public class GmallCacheAspect {
    /*
    通知：
    前置通知：
    后置通知：
    环绕通知：{前置，后置，执行方式体...}
    异常通知：
     */

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate redisTemplate;

    //  为什么返回Object ?  这个注解放到哪个方法上，你知道么？不知道！ 用Object 接收所有返回的数据.
    //  定义方法
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)") // 环绕通知
    public Object cahceAspectGmall(ProceedingJoinPoint point) throws Throwable {
        //  声明一个对象
        Object object = new Object();
        /*
            1.  先获取到缓存中的数据
                    object = get(key)

            2.  判断object!
         */
        //  先有key 缓存的key 由 GmallCache 的前缀+方法请求的参数！   SkuValueIdsMap:spuId
        //  先获取到注解！  注解是使用在方法上的！
        MethodSignature methodSignature = (MethodSignature) point.getSignature();
        GmallCache gmallCache = methodSignature.getMethod().getAnnotation(GmallCache.class);
        //  获取到了前缀
        String prefix = gmallCache.prefix();

        //  获取请求参数：
        Object[] args = point.getArgs();

        //  组成缓存的key
        //  String key = prefix+ Arrays.asList(args).toString()+gmallCache.suffix();
        String key = prefix+ Arrays.asList(args).toString();
        try {
            //  获取缓存的数据
            object = getRedisData(key,methodSignature);

            //  判断缓存的数据!
            if (object==null){
                //  缓存中的数据是为空！加分布式锁 获取数据库中的数据并放入缓存！
                String lockKey = key+":lock";
                RLock lock = redissonClient.getLock(lockKey);
                boolean result = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);

                //  判断result
                if (result){
                    try {
                        //  获取到了锁，才能查询数据库并将数据放入缓存.
                        object = point.proceed(point.getArgs()); // 表示执行带有GmallCache注解的方法体！
                        //  判断object
                        if (object==null){
                            //  声明一个Object 对象
                            Object o = new Object();
                            //  存储数据的时候，统一将o 变为字符串
                            this.redisTemplate.opsForValue().set(key, JSON.toJSONString(o),RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            return o;
                        }
                        //  不为空！
                        this.redisTemplate.opsForValue().set(key,JSON.toJSONString(object),RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                        return object;
                    } finally {
                        //  解锁
                        lock.unlock();
                    }
                }else {
                    //  睡眠自旋
                    Thread.sleep(200);
                    return cahceAspectGmall(point);
                }
            }else {
                return object;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        //  数据库兜底
        return point.proceed(point.getArgs());
    }

    //  从缓存中获取数据.
    private Object getRedisData(String key,MethodSignature methodSignature) {
        //  get key 存储的时候都是String 类型
        String o = (String) this.redisTemplate.opsForValue().get(key);
        //  判断
        if (!StringUtils.isEmpty(o)) {
            //  返回数据：这个时候，可以具体判断出当前缓存中存储的数据类型是什么！
            //  SkuInfo getSkuInfo(skuId);  Map getSkuValueIdsMap(Long spuId) ;
            //  渲染的时候，我需要将Json 数据，转换为方法的返回值类型！
            return JSON.parseObject(o,methodSignature.getReturnType());
        }
        //  默认返回
        return null;
    }

}