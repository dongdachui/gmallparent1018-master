package com.atguigu.gmall.product;

import com.atguigu.gmall.common.constant.RedisConst;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.atguigu.gmall"})
@EnableDiscoveryClient
public class ServiceProductApplication implements CommandLineRunner {

   @Autowired
   private RedissonClient redissonClient;

   public static void main(String[] args) {
      SpringApplication.run(ServiceProductApplication.class, args);
   }

   // 当应用程序加载的时候 ，设置一个误判率！ 类似于init 初始化方法.
   @Override
   public void run(String... args) throws Exception {
      // 设置布隆过滤器的误判率  redissonClient 集成了布隆过滤器
      RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
      // 已知误判率p, 数据规模n, 求二进制的个数m，哈希函数的个数k。
      // 第一个参数数据规模，第二个参数 误判率
      bloomFilter.tryInit(1000000,0.01);
   }
}
