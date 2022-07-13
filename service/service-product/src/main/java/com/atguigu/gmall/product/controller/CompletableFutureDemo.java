package com.atguigu.gmall.product.controller;

import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * author:atGuiGu-mqx
 * date:2022/5/4 11:56
 * 描述：
 **/
public class CompletableFutureDemo {

    @SneakyThrows
    public static void main(String[] args) {

        //        Integer i = 1232;
        //        Integer j = 1232;
//        BigDecimal i = new BigDecimal(121);
//        BigDecimal j = new BigDecimal(1288);
//        System.out.println(i==j);
//        System.out.println(i.equals(j));
//        System.out.println(i.compareTo(j));

//        System.out.println(Instant.now());
//        System.out.println(new Date());
//        System.out.println(LocalDateTime.now());
//        LocalDateTime localDateTime = LocalDateTime.now();
//        System.out.println(localDateTime.plusHours(1));
//        System.out.println(localDateTime.minusHours(1));



        //        //  创建对象
        //        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
        //            System.out.println("无返回.");
        //        });
        //
        //        //  有返回值！
        //        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
        //            //  手动制作一个运行时异常！
        //            int i = 1/0;
        //            return 1024;
        //        }).whenComplete((t,u)->{  // 接收线程的正常或异常的处理结果.
        //            System.out.println("t:\t"+t);   //t:表示上一个线程执行的结果 t 1024  有了异常，返回值null
        //            System.out.println("u:\t"+u);   //u:异常信息.  java.util.concurrent.CompletionException: java.lang.ArithmeticException: / by zero
        //
        //        }).exceptionally((h)->{ //  处理异常的方法.
        //            System.out.println("h:\t"+h); // h:	java.util.concurrent.CompletionException: java.lang.ArithmeticException: / by zero
        //            return 404;
        //        });
        //
        //        // System.out.println(voidCompletableFuture.get()); // null
        //        System.out.println(integerCompletableFuture.get()); // 1024 ; 异常返回404！


        //  串行化 and 并行化
        //        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
        //            return 1024;
        //        }).thenApply((f)->{
        //            //  接收上一个返回结果，并消费处理。同时有返回值
        //            System.out.println("f:\t"+f);
        //            return f*2;
        //        }).whenComplete((t,u)->{
        //            System.out.println("t:\t"+t);
        //            System.out.println("u:\t"+u);
        //        }).exceptionally((t)->{
        //            return 404;
        //        });
        //        //  获取数据.
        //        System.out.println(integerCompletableFuture.get());
        //      回顾JUC 线程池。

//        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
//                3, //  核心线程个数
//                5, //  最大线程个数
//                3, //  空线程存活时间
//                TimeUnit.SECONDS, // 空闲线程存活时间单位
//                new ArrayBlockingQueue<>(3) // 默认线程工厂，拒绝策略.
//        );
//
//
//
//        //        //  并行化：
//        CompletableFuture<String> completableFutureA = CompletableFuture.supplyAsync(() -> {
//            return "hello";
//        },threadPoolExecutor);
//        //  thenAcceptAsync方法 异步：有可能是自己执行，也有可能是线程池中的其他线程执行。
//        CompletableFuture<Void> completableFutureB = completableFutureA.thenAcceptAsync((c) -> {
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println(c + ":\tB");
//        },threadPoolExecutor);
//
//        //  并行化
//        CompletableFuture<Void> completableFutureC = completableFutureA.thenAcceptAsync((c) -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println(c + ":\tC");
//        },threadPoolExecutor);
//
//        System.out.println(completableFutureB.get());
//        System.out.println(completableFutureC.get());

    }
}