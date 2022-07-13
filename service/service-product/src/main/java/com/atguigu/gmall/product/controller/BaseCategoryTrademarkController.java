package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * author:atGuiGu-mqx
 * date:2022/4/26 15:38
 * 描述：
 **/
@RestController
@RequestMapping("/admin/product/baseCategoryTrademark")
public class BaseCategoryTrademarkController {

    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;

    //  根据三级分类Id 查询品牌列表！
    //  /admin/product/baseCategoryTrademark/findTrademarkList/{category3Id}
    @GetMapping("/findTrademarkList/{category3Id}")
    public Result findTrademarkList(@PathVariable Long category3Id){
        //  调用服务层方法获取数据.
        List<BaseTrademark> baseTrademarkList = baseCategoryTrademarkService.getTrademarkList(category3Id);
        //  返回数据.
        return Result.ok(baseTrademarkList);
    }

    //  根据三级分类Id 查询可选的品牌列表
    //  /admin/product/baseCategoryTrademark/findCurrentTrademarkList/{category3Id}
    @GetMapping("/findCurrentTrademarkList/{category3Id}")
    public Result findCurrentTrademarkList(@PathVariable Long category3Id){
        //  调用服务层方法.
        List<BaseTrademark> baseTrademarkList =  baseCategoryTrademarkService.getCurrentTrademarkList(category3Id);
        //  默认返回数据
        return Result.ok(baseTrademarkList);
    }

    //  保存品牌Id与分类Id 的关系！
    //  /admin/product/baseCategoryTrademark/save
    //  前端vue 保存数据的时候，传递的格式Json ---> JavaObject
    @PostMapping("save")
    public Result save(@RequestBody CategoryTrademarkVo categoryTrademarkVo){
        //  调用服务层方法.
        baseCategoryTrademarkService.save(categoryTrademarkVo);
        //  默认返回.
        return Result.ok();
    }

    //  删除分类与品牌的绑定关系！
    //  /admin/product/baseCategoryTrademark/remove/{category3Id}/{trademarkId}
    @DeleteMapping("remove/{category3Id}/{trademarkId}")
    public Result removeById(@PathVariable Long category3Id,
                             @PathVariable Long trademarkId){

        //  调用删除方法
        baseCategoryTrademarkService.deleteById(category3Id,trademarkId);
        return Result.ok();
    }

}