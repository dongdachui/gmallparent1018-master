package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * author:atGuiGu-mqx
 * date:2022/5/11 9:28
 * 描述：
 **/
@RestController
@RequestMapping("api/cart/")
public class CartApiController {

    @Autowired
    private CartService cartService;

    //  http://api.gmall.com/api/cart/addToCart/24/2
    @GetMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request){

        //  获取到用户Id：userId = 1 userTempId = uu7711www; 如果同时存在以登录为准.
        String userId = AuthContextHolder.getUserId(request);
        //  添加购物车时 ： 可以登录，也可以未登录. 未登录的时候没有userId 的，因此：创建一个临时用户Id！
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }

        //  调用添加购物车方法.
        cartService.addToCart(skuId,userId,skuNum);
        //  默认返回
        return Result.ok();
    }

    //  /api/cart/cartList
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request){
        //  获取用户Id，获取临时用户Id
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        //  购物车列表.
        List<CartInfo> cartInfoList = cartService.getCartList(userId,userTempId);
        //  返回购物车集合列表
        return Result.ok(cartInfoList);
    }

    //  编写控制器：
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){

        //  获取登录用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  获取未登录userId
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }
        //  调用服务层方法. 改变购物车集合中某个商品的状态.
        this.cartService.checkCart(skuId,userId,isChecked);
        //  默认返回
        return Result.ok();
    }

    //  删除购物项：
    @DeleteMapping("/deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  判断用户Id 为空
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }
        //  调用服务层方法.
        this.cartService.deleteCart(skuId,userId);
        //  默认返回
        return Result.ok();
    }

    //  获取送货清单！
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCheckCartList(@PathVariable String userId){
        //  调用服务层方法.
        return cartService.getCartCheckedList(userId);
    }

}