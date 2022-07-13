package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;

public interface UserService {

    //  user_info ---> userInfo

    /**
     * 登录接口
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 登录接口
     * @param userName
     * @param password
     * @return
     */
    // UserInfo login(String userName,String password);


}
