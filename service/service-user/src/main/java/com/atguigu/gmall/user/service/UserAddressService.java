package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;

import java.util.List;

/**
 * author:atGuiGu-mqx
 * date:2022/5/11 15:32
 * 描述：
 **/
public interface UserAddressService {
    /**
     * 根据userId 获取收货地址列表.
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(String userId);
}
