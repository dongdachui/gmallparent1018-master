package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/5/9 9:28
 * 描述：
 **/
@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;

    /*
        http://list.gmall.com/list.html?category3Id=61&order=xx:xx
        http://list.gmall.com/list.html?keyword=手机
        springmvc 对象传值 : 只要你url 后的参数名称 与 实体类的属性名称一致，会自动映射到实体类中！
    */
    //  编写商品检索控制器！
    @GetMapping("list.html")
    public String list(SearchParam searchParam, Model model){
        //  后台要存储页面渲染的key！
        //  searchParam trademarkParam propsParamList trademarkList attrsList orderMap urlParam goodsList pageNo totalPages
        //  调用检索的方法.
        Result<Map> result = this.listFeignClient.list(searchParam);
        //   SearchResponseVo = result.getData();
        //  看 SearchResponseVo 的属性.  trademarkList attrsList goodsList pageNo totalPages
        //  实体类与map 可以互换  feignClient 远程调用：底层封装的是LinkedHashMap!
        //  还需要存储： trademarkParam  propsParamList orderMap urlParam
        //  orderMap： 表示排序
        HashMap<String,String> orderMap = this.makeOrderMap(searchParam.getOrder());

        //  propsParamList 表示平台属性面包屑：
        List<Map> propsParamList = this.makePropsParamList(searchParam.getProps());
        //  trademarkParam 表示品牌的面包屑：
        String trademarkParam = this.makeTradeMarkParam(searchParam.getTrademark());
        //  urlParam ：用来存储用户通过哪些属性进行检索的参数！
        String urlParam = this.makeUrlParam(searchParam);
        model.addAttribute("urlParam",urlParam);
        model.addAttribute("trademarkParam",trademarkParam);
        model.addAttribute("propsParamList",propsParamList);
        model.addAttribute("orderMap",orderMap);
        model.addAllAttributes(result.getData());
        model.addAttribute("searchParam",searchParam);

        //  返回视图
        return "list/index";
    }

    /**
     * 制作排序
     * @param order
     * @return
     */
    private HashMap<String, String> makeOrderMap(String order) {
        //  声明一个map 集合
        HashMap<String, String> map = new HashMap<>();
        //  判断:页面url 获取的 order=1:desc order=1:asc  order=2:desc order=2:asc
        if (!StringUtils.isEmpty(order)){
            //  分割数据
            String[] split = order.split(":");
            if (split!=null && split.length==2){
                //  1: 表示综合排序 2：表示价格排序
                map.put("type",split[0]);
                map.put("sort",split[1]);
            }
        }else {
            //  默认排序规则： 按照综合进行降序排列！
            map.put("type","1");
            map.put("sort","desc");
        }
        return map;
    }

    /**
     * 平台属性面包屑.
     * @param props
     * @return
     */
    private List<Map> makePropsParamList(String[] props) {
        //  声明一个集合
        List<Map> list = new ArrayList<>();
        //  判断 &props=23:8G:运行内存&props=24:256G:机身内存
        if (props!=null && props.length>0){
            for (String prop : props) {
                //  prop = 23:8G:运行内存  prop = 24:256G:机身内存
                //  封装平台属性名：平台属性值名
                String[] split = prop.split(":");
                if (split!=null && split.length==3){ // 因为：分割之后数组长度的3
                    Map map = new HashMap();
                    map.put("attrId",split[0]);
                    map.put("attrValue",split[1]);
                    map.put("attrName",split[2]);
                    //  需要将map 添加到集合
                    list.add(map);
                }
            }
        }
        return list;
    }

    /**
     * 制作品牌的面包屑
     * 不管你在哪地方，都可以通过面包屑（导航）返回原地
     * @param trademark
     * @return
     */
    private String makeTradeMarkParam(String trademark) {
        //  判断  trademark=3:华为
        if (!StringUtils.isEmpty(trademark)){
            //  分割字符串
            String[] split = trademark.split(":");
            if (split!=null && split.length==2){
                return "品牌:"+split[1];
            }
        }
        return null;
    }

    //  获取拼接url参数.
    private String makeUrlParam(SearchParam searchParam) {
        //  声明一个字符串拼接对象
        StringBuilder sb = new StringBuilder();
        //  判断是否根据关键词检索
        //  http://list.gmall.com/list.html?keyword=手机
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            //  sb = keyword=手机
            sb.append("keyword=").append(searchParam.getKeyword());
        }

        //  判断用户是否根据分类Id 进行检索的
        //  http://list.gmall.com/list.html?category3Id=61
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
            //  sb = category3Id=61
            sb.append("category3Id=").append(searchParam.getCategory3Id());
        }
        //  http://list.gmall.com/list.html?category2Id=13
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
            //  sb = category2Id=13
            sb.append("category2Id=").append(searchParam.getCategory2Id());
        }
        //  http://list.gmall.com/list.html?category1Id=2
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //  sb = category1Id=2
            sb.append("category1Id=").append(searchParam.getCategory1Id());
        }

        //  还有可能根据品牌Id 检索。 trademark=3:华为
        //  http://list.gmall.com/list.html?category3Id=61&trademark=3:华为
        //  http://list.gmall.com/list.html?keyword=手机&trademark=3:华为
        if (!StringUtils.isEmpty(searchParam.getTrademark())){
            if (sb.length()>0) {
                sb.append("&trademark=").append(searchParam.getTrademark());
            }
        }

        //  参数拼接的时候第一个参数是在? 后面的，不需要加& 如果参数大于1个，则需要在拼接的时候添加&
        //  平台属性属性值过滤.
        String[] props = searchParam.getProps();
        //  判断  http://list.gmall.com/list.html?category3Id=61&trademark=3:华为&props=23:4G:运行内存&props=123:9.5以上:屏幕尺寸
        if (props!=null && props.length>0){
            //  props 表示一个数组，
            for (String prop : props) {
                if (sb.length()>0) {
                    sb.append("&props=").append(prop);
                }
            }
        }
        //  返回数据. list.html?keyword=手机
        return "list.html?"+sb.toString();
    }

}