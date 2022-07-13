package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * author:atGuiGu-mqx
 * date:2022/5/9 14:44
 * 描述：
 **/
@Service
public class UserServiceImpl implements UserService {

    //  注入mapper 层.
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {
        //  select * from user_info where login_name = ? and passwd = ?
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        //  admin
        userInfoQueryWrapper.eq("login_name",userInfo.getLoginName());
        //  111111 ---> 加密之后的数据。 对其进行了md5加密
        String passwd = userInfo.getPasswd();
        String newPassword = DigestUtils.md5DigestAsHex(passwd.getBytes());
        //  96e79218965eb72c92a549dd5a330112
        userInfoQueryWrapper.eq("passwd",newPassword);
        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        //  判断
        if (info!=null){
            return info;
        }
        return null;
    }
}