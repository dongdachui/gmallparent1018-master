package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.service.UserAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * author:atGuiGu-mqx
 * date:2022/5/11 15:29
 * 描述：
 **/
@RestController
@RequestMapping("/api/user")
public class UserApiController {

    //  注入服务层对象
    @Autowired
    private UserAddressService userAddressService;

    //  根据用户Id 来获取收货地址列表.
    //  每个人对应很多个收货地址列表.
    @GetMapping("inner/findUserAddressListByUserId/{userId}")
    public List<UserAddress> findUserAddressListByUserId(@PathVariable String userId){
        return userAddressService.findUserAddressListByUserId(userId);
    }

}