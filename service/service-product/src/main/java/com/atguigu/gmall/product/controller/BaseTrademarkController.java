package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * author:atGuiGu-mqx
 * date:2022/4/26 14:48
 * 描述：
 **/
@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    //  获取品牌列表
    //  /admin/product/baseTrademark/{page}/{limit}
    @GetMapping("{page}/{limit}")
    public Result getBaseTradeMarkList(@PathVariable Long page,
                                       @PathVariable Long limit){
        //  封装分页数据
        Page<BaseTrademark> baseTrademarkPage = new Page<>(page,limit);
        //  调用服务层方法
        IPage<BaseTrademark> iPage = baseTrademarkService.getBaseTradeMarkList(baseTrademarkPage);
        return Result.ok(iPage);
    }

    //  /admin/product/baseTrademark/save
    //  Json ---> JavaObject
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        //  调用方法。
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    //  /admin/product/baseTrademark/remove/{id}
    @DeleteMapping("remove/{id}")
    public Result removeById(@PathVariable Long id){
        //  调用服务层方法
        this.baseTrademarkService.removeById(id);
        return Result.ok();
    }

    //  回显品牌数据： /admin/product/baseTrademark/get/{id}
    @GetMapping("/get/{id}")
    public Result getById(@PathVariable Long id){
        //  调用服务层方法
        BaseTrademark baseTrademark = this.baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    //  修改数据： /admin/product/baseTrademark/update
    //  Json ---> JavaObject
    @PutMapping("update")
    public Result updateBaseTradeMark(@RequestBody BaseTrademark baseTrademark){
        //  调用方法。
        this.baseTrademarkService.updateById(baseTrademark);

        return Result.ok();
    }
}