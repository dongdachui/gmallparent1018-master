package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * author:atGuiGu-mqx
 * date:2022/5/11 15:32
 * 描述：
 **/
@Service
public class UserAddressServiceImpl implements UserAddressService {

    //  调用mapper 层
    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        //  select * from user_address where user_id = ?;
        return userAddressMapper.selectList(new QueryWrapper<UserAddress>().eq("user_id",userId));
    }
}