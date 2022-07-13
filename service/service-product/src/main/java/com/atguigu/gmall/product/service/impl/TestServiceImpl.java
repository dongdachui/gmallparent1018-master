package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import io.swagger.models.auth.In;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * author:atGuiGu-mqx
 * date:2022/5/3 8:59
 * 描述：
 **/
@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /*
   1.  获取缓存中的数据  set num 0 key = num
   2.  判断这个key
       true: 对内容进行+1 ，并放入缓存.
       false: return
    */

    @Override
    public String readLock() {
        //  读写锁对象
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("anyRWLock");
        //  上锁
        rwlock.readLock().lock(10,TimeUnit.SECONDS);
        //  从缓存中读取信息。
        String msg = this.redisTemplate.opsForValue().get("msg");

        //  立刻解锁
        //  rwlock.readLock().unlock();
        return msg;
    }

    @Override
    public String writeLock() {
        //  读写锁对象
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("anyRWLock");
        //  上锁
        rwlock.writeLock().lock(10,TimeUnit.SECONDS);
        String uuid = UUID.randomUUID().toString();
        this.redisTemplate.opsForValue().set("msg",uuid);
        //  解锁
        //  rwlock.writeLock().unlock();
        return "写入完成...";
    }

    @Override
    public void testLock() {
        //  可重入锁对象
        RLock lock = redissonClient.getLock("lock");
        // 上锁
        // lock.lock();
        boolean res = false;
        try {
            res = lock.tryLock(100, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (res){
            //  业务逻辑
            try {
                //  get num
                String num = redisTemplate.opsForValue().get("num");
                //  判断
                if (StringUtils.isEmpty(num)){
                    return;
                }
                //  对内容进行+1 ，并放入缓存.
                int number = Integer.parseInt(num);
                //  set num value
                redisTemplate.opsForValue().set("num",String.valueOf(++number));
            } finally {
                //  解锁
                lock.unlock();
            }
        }

        //  如果这个结果测出是 5000 则不需要写else ! 如果不是5000 有可能需要写 else !  调一下 最大等到时间。
    }

    //    @Override
    //    public void testLock() {
    //        /*
    //        0.  先获取到分布式锁！
    //            获取到锁：
    //            1.  获取缓存中的数据  set num 0 key = num
    //            2.  判断这个key
    //               true: 对内容进行+1 ，并放入缓存.
    //               false: return
    //            获取锁失败：
    //                重试：
    //        */
    //
    //        //  setnx key value
    //        //  Boolean flag = this.redisTemplate.opsForValue().setIfAbsent("lock", "ok");
    //        //  set lock ok ex 3 nx
    //        //  Boolean flag = this.redisTemplate.opsForValue().setIfAbsent("lock", "ok",3,TimeUnit.SECONDS);
    //        //  设置uuid 防止误删锁！
    //        String uuid = UUID.randomUUID().toString();
    //        Boolean flag = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid,3,TimeUnit.SECONDS);
    //        //  判断
    //        if (flag){
    //            //  获取到锁：
    //            //  get num
    //            String num = redisTemplate.opsForValue().get("num");
    //
    //            //  判断
    //            if (StringUtils.isEmpty(num)){
    //                return;
    //            }
    //
    //            //  对内容进行+1 ，并放入缓存.
    //            int number = Integer.parseInt(num);
    //            //  set num value
    //            redisTemplate.opsForValue().set("num",String.valueOf(++number));
    //            //  expire lock 10;
    //            //  this.redisTemplate.expire("lock",10, TimeUnit.SECONDS);
    //            //  释放锁！
    //            //            if (uuid.equals(redisTemplate.opsForValue().get("lock"))){
    //            //                //  index1 进来了。 还没有执行del 命令 锁开了。
    //            //                //  index2
    //            //                //  cpu 将执行权限交给了index1 ,这时候执行删除del . 删除的是index2 的锁！
    //            //                redisTemplate.delete("lock");
    //            //            }
    //
    //            //  使用lua 脚本删除！
    //            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    //
    //            //  执行lua 脚本
    //            DefaultRedisScript redisScript = new DefaultRedisScript();
    //            redisScript.setScriptText(script);
    //            redisScript.setResultType(Long.class);
    //            //  第二个参数 key
    //            //  第三个参数 value
    //            this.redisTemplate.execute(redisScript, Arrays.asList("lock"),uuid);
    //
    //        } else {
    //            //   没有获取到锁！
    //            try {
    //                Thread.sleep(100);
    //            } catch (InterruptedException e) {
    //                e.printStackTrace();
    //            }
    //            //  重试
    //            testLock();
    //        }
    //    }

    //    @Override
        //    public synchronized void testLock() {
        //        /*
        //        1.  获取缓存中的数据  set num 0 key = num
        //        2.  判断这个key
        //            true: 对内容进行+1 ，并放入缓存.
        //            false: return
        //         */
        //
        //        //  get num
        //        String num = redisTemplate.opsForValue().get("num");
        //
        //        //  判断
        //        if (StringUtils.isEmpty(num)){
        //            return;
        //        }
        //
        //        //  对内容进行+1 ，并放入缓存.
        //        int number = Integer.parseInt(num);
        //        //  set num value
        //        redisTemplate.opsForValue().set("num",String.valueOf(++number));
        //
        //    }
}