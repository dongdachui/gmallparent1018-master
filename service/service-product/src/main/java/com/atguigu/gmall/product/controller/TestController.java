package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * author:atGuiGu-mqx
 * date:2022/5/3 8:57
 * 描述：
 **/
@RestController
@RequestMapping("admin/product/test")
public class TestController {

    @Autowired
    private TestService testService;

    @GetMapping("testLock")
    public Result testLock(){
        //  调用服务层方法
        testService.testLock();
        return Result.ok();
    }

    //  读写锁：
    @GetMapping("read")
    public Result readLock(){
        //  调用服务层方法
        String msg = testService.readLock();
        return Result.ok(msg);
    }

    @GetMapping("write")
    public Result writeLock(){
        //  调用服务层方法
        String msg = testService.writeLock();
        return Result.ok(msg);
    }

}