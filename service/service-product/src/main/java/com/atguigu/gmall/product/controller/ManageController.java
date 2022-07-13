package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * author:atGuiGu-mqx
 * date:2022/4/23 10:35
 * 描述：
 **/
@RestController // @Controller + @ResponseBody 返回Json 数据 vue 获取后台数据的时候，都要是json 数据格式！
@RequestMapping("/admin/product")
//@CrossOrigin
public class ManageController {

    @Autowired
    private ManageService manageService;

    // 获取一级分类数据  /admin/product/getCategory1
    @GetMapping("getCategory1")
    public Result getCategory1(){
        //  查询所有的一级分类数据返回给页面！
        List<BaseCategory1>  category1List = manageService.getCategory1();
        //  默认返回成功。
        return Result.ok(category1List);
    }

    //  根据一级分类Id 来获取二级分类数据.
    //  /admin/product/getCategory2/{category1Id}
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id){
        //  调用服务层方法.
        List<BaseCategory2> baseCategory2List = manageService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }

    //  根据二级分类Id 来获取 三级分类数据
    //  /admin/product/getCategory3/{category2Id}
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id){
        //  调用服务层方法.
        List<BaseCategory3> baseCategory3List = manageService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }

    //  根据分类Id来获取平台属性数据。平台属性和平台属性值
    //  /admin/product/attrInfoList/{category1Id}/{category2Id}/{category3Id}
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result getAttrInfoList(@PathVariable Long category1Id,
                                  @PathVariable Long category2Id,
                                  @PathVariable Long category3Id){

        //  调用服务层方法.
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrInfoList(category1Id,category2Id,category3Id);
        //  返回数据
        return Result.ok(baseAttrInfoList);
    }

    //  保存平台属性 / 修改平台属性值
    //  /admin/product/saveAttrInfo
    //  接收到Json 数据变为 Java Object
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        //  调用服务层方法.
        this.manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    //  根据平台属性Id 获取到平台属性值集合
    //  /admin/product/getAttrValueList/{attrId}
    //回显平台属性
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId){
        //  调用服务层方法
        //  List<BaseAttrValue> baseAttrValueList = this.manageService.getAttrValueList(attrId);
        //  根据attrId 先查询到平台属性， 如果有平台属性了，则回显平台属性值集合！
        BaseAttrInfo baseAttrInfo = this.manageService.getBaseAttrInfo(attrId);
        //  返回属性值集合：
        return Result.ok(baseAttrInfo.getAttrValueList());
    }


}