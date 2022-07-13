package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

import java.io.FileWriter;
import java.io.IOException;

/**
 * author:atGuiGu-mqx
 * date:2022/5/6 10:19
 * 描述：
 **/
@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private TemplateEngine templateEngine;

    //  www.gmall.com  或 www.gmall.com/index.html 都能访问到首页数据.
    @GetMapping({"/","index.html"})
    public String index(Model model){
        //  远程调用分类数据集合
        Result result = productFeignClient.getBaseCategoryList();
        model.addAttribute("list",result.getData());
        //  后台需要存储一个集合
        return "index/index";
    }


    //  制作一个静态化页面 {输出到某个盘符下}
    @GetMapping("createIndex")
    @ResponseBody
    public Result createIndex(){
        //  远程调用分类数据集合
        Result result = productFeignClient.getBaseCategoryList();
        Context context = new Context();
        context.setVariable("list",result.getData());

        //  创建Write 对象
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("F:\\index.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //  调用模板引擎创建静态化页面
        this.templateEngine.process("/index/index.html",context,fileWriter);
        //  默认返回Ok
        return Result.ok();
    }

}