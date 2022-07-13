package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * author:atGuiGu-mqx
 * date:2022/5/9 15:14
 * 描述：
 **/
@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    //  接收用户登录的参数！
    //  /api/user/passport/login
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request){
        //  登录方法
        UserInfo info = userService.login(userInfo);
        //  登录成功之后才会生成token
        if (info!=null){
            //  声明一个map 集合
            HashMap<String, Object> hashMap = new HashMap<>();
            //  声明token 在cookie 中，并且不重复！
            String token = UUID.randomUUID().toString();
            //  将token 放入map 中。并将map 放入ok中!
            hashMap.put("token",token);
            hashMap.put("nickName",info.getNickName());

            //  还需要做什么事? 需要将用户信息保存到缓存。 考虑使用数据类型. String
            //  loginKey = user:login:userId 如果用户userId 当缓存的key? 那么你必然要执行 select * from user_info where uname = ? and pwd =?;
            //  loginKey = user:login:token token 直接存储到cookie 中.
            String loginKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
            //  如果token 被调用了？ 添加一个ip 地址。 验证用户是否登录的时候， 判断服务器ip 地址与 缓存的ip 是地址是否一致。
            String ip = IpUtil.getIpAddress(request);
            //  声明一个对象
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId",info.getId().toString());
            jsonObject.put("ip",ip);
            //  存储用户信息只需要一个userId 即可！
            this.redisTemplate.opsForValue().set(loginKey,jsonObject.toJSONString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

            //  ok():200;
            return Result.ok(hashMap);
        }
        //  登录失败提示信息。
        return Result.fail().message("登录失败.");
    }

    //  退出功能：http://api.gmall.com/api/user/passport/logout
    //  获取请求头中的token！
    @GetMapping("logout")
    public Result logout(HttpServletRequest request,@RequestHeader String token){
        //  只需要删除缓存就可以了， 关于cookie中的数据，是有js 代码实现的！
        //  token 从哪里获取?  在cookie 中存储！ 在点击登录的时候有一个异步拦截器：将token 放入了请求头中.
        String token1 = request.getHeader("token");
        System.out.println("token1:\t"+token1);
        System.out.println("token:\t"+token);

        //  组成缓存的key
        String loginKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;

        this.redisTemplate.delete(loginKey);
        //  默认返回。
        return Result.ok();
    }



}