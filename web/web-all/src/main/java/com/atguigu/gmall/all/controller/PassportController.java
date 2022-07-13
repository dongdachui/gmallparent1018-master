package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * author:atGuiGu-mqx
 * date:2022/5/10 9:37
 * 描述：
 **/
@Controller
public class PassportController {

    //  登录页面控制器！
    //  http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        //  保存一个 originUrl
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        //  返回登录页面.
        return "login";
    }
}