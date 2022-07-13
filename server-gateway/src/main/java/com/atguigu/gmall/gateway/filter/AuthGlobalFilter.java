package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * author:atGuiGu-mqx
 * date:2022/5/10 10:48
 * 描述：
 **/
@Component
public class AuthGlobalFilter  implements GlobalFilter {

    @Value("${authUrls.url}")
    private String authUrls;  //  trade.html,myOrder.html,list.html

    @Autowired
    private RedisTemplate redisTemplate;

    //  匹配对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //  1.  判断用户访问的url 路径！
        ServerHttpRequest request = exchange.getRequest();
        exchange.getRequest().getQueryParams().getFirst("");
        String path = request.getURI().getPath();  // /api/product/inner/getSkuInfo/24

        //  判断用户访问的url 是否是 内部数据接口
        if (antPathMatcher.match("/**/inner/**",path)){
            //  获取一个响应对象
            ServerHttpResponse response = exchange.getResponse();
            //  停止运行,给信息提示!
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //  获取用户Id
        String userId = this.getUserId(request);
        String userTempId = this.getUserTempId(request);

        //  判断用户Id
        if ("-1".equals(userId)){
            //  获取一个响应对象
            ServerHttpResponse response = exchange.getResponse();
            //  停止运行,给信息提示!
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //  2.  判断用户访问的url 是否包含 auth ！
        if (antPathMatcher.match("/api/**/auth/**",path)){
            //  判断用户是否登录
            if (StringUtils.isEmpty(userId)){
                //  获取一个响应对象
                ServerHttpResponse response = exchange.getResponse();
                //  停止运行,给信息提示!
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }

        //  3.  看用户访问的业务是否需要登录！  authUrls=trade.html,myOrder.html,list.html
        //  path = 用户访问的url
        //  分割字符串.   trade.html,myOrder.html,list.html
        String[] split = authUrls.split(",");
        if (split!=null && split.length>0){
            for (String url : split) {
                //  用户访问的url 中 存在 要拦截的业务控制器 ！ 并且 用户是未登录状态！
                if (path.indexOf(url)!=-1 && StringUtils.isEmpty(userId)){
                    //  页面跳转：
                    //  获取一个响应对象
                    ServerHttpResponse response = exchange.getResponse();
                    //  http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
                    //  设置状态码  SEE_OTHER(303, "See Other"),
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    //  设置url   http://passport.gmall.com/login.html?originUrl= ?
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://passport.gmall.com/login.html?originUrl="+request.getURI());
                    //  重定向
                    return response.setComplete();
                }
            }
        }

        //  将获取到的用户Id 传递到后台去！ 将userId 放入请求头中！
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId) ){
            //  判断userId
            if (!StringUtils.isEmpty(userId)){
                //  将userId 存储到header 中.
                //  ServerHttpRequest request
                request.mutate().header("userId", userId).build();
            }
            //  判断userTempId
            if (!StringUtils.isEmpty(userTempId)){
                //  将userTempId 存储到header 中.
                //  ServerHttpRequest request
                request.mutate().header("userTempId", userTempId).build();
            }
            //  request 设置好的userId 变为 exchange 对象。
            return chain.filter(exchange.mutate().request(request).build());
        }

        //  默认返回.
        return chain.filter(exchange);
    }

    /**
     * 获取临时用户Id
     * @param request
     * @return
     */
    private String getUserTempId(ServerHttpRequest request) {
        //  有可能存在cookie ，或 存在在请求头：登录拦截时才会放入请求头！
        String userTempId = "";
        HttpCookie httpCookie = request.getCookies().getFirst("userTempId");
        if (httpCookie!=null){
            userTempId=httpCookie.getValue();
        } else {
            List<String> stringList = request.getHeaders().get("userTempId");
            if (!CollectionUtils.isEmpty(stringList)){
                userTempId = stringList.get(0);
            }
        }
        //  返回临时用户Id
        return userTempId;
    }

    /**
     * 获取用户Id方法
     * @param request
     * @return
     */
    private String getUserId(ServerHttpRequest request) {
        //  用户Id 在缓存中存储。
        //  获取到缓存的 String loginKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;  所以要获取到token！
        String token = "";
        //  token 可能存储在 cookie 中，也有可能存在header 中.
        HttpCookie httpCookie = request.getCookies().getFirst("token");
        if (httpCookie!=null){
            token = httpCookie.getValue();
        }else {
            List<String> stringList = request.getHeaders().get("token");
            if (!CollectionUtils.isEmpty(stringList)){
                //  因为token 在cookie 中存储的时候，只有一个数据。所以在取数据的时候也只有一条记录
                token = stringList.get(0);
            }
        }

        //  判断token
        if (!StringUtils.isEmpty(token)){
            //  组成缓存的key！
            String loginKey = "user:login:"+token;
            //  根据key 来获取缓存的数据
            String strJson = (String) this.redisTemplate.opsForValue().get(loginKey);
            //  判断获取出来的数据.
            if (!StringUtils.isEmpty(strJson)){
                //  数据类型转换.
                JSONObject jsonObject = JSONObject.parseObject(strJson);
                if (jsonObject!=null){
                    //  要校验缓存的ip 地址与当前的ip 地址是否一致！
                    String ip = (String) jsonObject.get("ip");
                    if (ip.equals(IpUtil.getGatwayIpAddress(request))){
                        //  ip 相等
                        String userId = (String) jsonObject.get("userId");
                        //  返回userId
                        return userId;
                    }else {
                        return "-1";
                    }
                }
            }
        }
        return "";
    }

    /**
     * 输出信息提示
     * @param response
     * @param resultCodeEnum
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        //  将信息提示输出到页面.
        //  只输出一个字符串信息. String message = resultCodeEnum.getMessage();
        //  ResultCodeEnum 对象转换为 Result
        Result<Object> result = Result.build(null, resultCodeEnum);
        //  将这个对象转换为字符串.
        String str = JSON.toJSONString(result);
        //  设置输出内容的数据格式：
        response.getHeaders().add("Content-Type","application/json;charset=UTF-8");
        //  将输出的数据转换为DataBuffer
        DataBuffer wrap = response.bufferFactory().wrap(str.getBytes());
        //  DataBuffer wrap = response.bufferFactory().wrap(resultCodeEnum.getMessage().getBytes());
        //  返回  Mono 抽象类：
        return response.writeWith(Mono.just(wrap));
    }
}