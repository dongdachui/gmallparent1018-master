package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * author:atGuiGu-mqx
 * date:2022/4/28 9:04
 * 描述：
 **/
@RestController
@RequestMapping("admin/product")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    //  http://localhost/admin/product/saveSkuInfo
    //  前端传递的数据 Json ---> JavaObject
    //  保存skuInfo
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        //  调用服务层方法.
        manageService.saveSkuInfo(skuInfo);
        //  默认返回.
        return Result.ok();
    }

    //  查询skuInfo
    //  http://localhost/admin/product/list/1/20?category3Id=61
    //  /admin/product/list/{page}/{limit}
//    SkuInfo skuInfo 获取三级分类id。
    @GetMapping("list/{page}/{limit}")
    public Result getSkuInfoList(@PathVariable Long page,
                                 @PathVariable Long limit,
                                 SkuInfo skuInfo){
        //  封装Page 对象
        Page<SkuInfo> skuInfoPage = new Page<>(page, limit);
        //  调用服务层方法.
        IPage<SkuInfo> iPage = this.manageService.getSkuInfoList(skuInfoPage,skuInfo);
        return Result.ok(iPage);
    }

    //  上架
    //  /admin/product/onSale/{skuId}
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        //  调用服务层方法.
        this.manageService.onSale(skuId);
        //  默认返回
        return Result.ok();
    }

    //  下架
    //  /admin/product/cancelSale/{skuId}
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        //  调用服务层方法.
        this.manageService.cancelSale(skuId);
        //  默认返回
        return Result.ok();
    }


}