package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * author:atGuiGu-mqx
 * date:2022/5/10 15:39
 * 描述：
 **/
@Service
public class CartServiceImpl implements CartService {




    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;


    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        /*
        1.  购物车中没有要添加的商品，则直接添加！
        2.  购物车中有要添加的商品，则数量相加！
        3.  每次添加商品的时候，都是默认选中状态！
        4.  根据修改时间进行排序！ 每添加一个商品之后，都要重新设置一下修改时间！
         */
        //  数据存储在 redis ：选用的数据类型！String 【Hash】 List Set ZSet 以及购物项对应的实体类！
        //  存储数据：hset key field value  获取数据：hget key field  删除数据：hdel key field  获取数据：hvals key
        //  key = user:userId:cart  field = skuId  value = CartInfo
        //  定义购物车的key ， 定义为一个方法。
        String cartKey = getCartKey(userId);
        //  根据cartKey 来获取缓存的数据  hget key field
        CartInfo cartInfoExist = (CartInfo) this.redisTemplate.opsForHash().get(cartKey, skuId.toString());
        //  判断 cartInfoExist 是否存在
        if (cartInfoExist!=null){
            //  添加的商品在购物车中存在.
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //  每次添加商品的时候，都是默认选中状态！CartInfo 实体类中 isChecked 表示是否选中 默认值1  1：表示选中，0：表示未选中
            if (cartInfoExist.getIsChecked().intValue()!=1){
                cartInfoExist.setIsChecked(1);
            }
            //  设置修改时间
            cartInfoExist.setUpdateTime(new Date());
            //  有关于购物车中商品的价格 ： 商品的实时价格 skuPrice
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));

            //  会将修改之后的对象放入缓存. hset key field value
            //  this.redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);
        } else {
            //  添加的商品在购物车中不存在.
            //  CartInfo cartInfo = new CartInfo();
            cartInfoExist = new CartInfo();
            //  远程调用方法有可能会查询缓存，
            SkuInfo skuInfo = this.productFeignClient.getSkuInfo(skuId);
            //  进行赋值  数据来源于谁？ skuInfo
            cartInfoExist.setUserId(userId);
            cartInfoExist.setSkuId(skuId);
            //  放入购物车时的价格. 商品详情显示的数据传递给购物车.
            //  cartPrice : 表示放入购物车时的价格。
            cartInfoExist.setCartPrice(skuInfo.getPrice()); // 这种情况：有可能价格不是最新的.
            //  cartInfo.setCartPrice(productFeignClient.getSkuPrice(skuId)); // 一定是最新的
            cartInfoExist.setSkuNum(skuNum);
            cartInfoExist.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoExist.setSkuName(skuInfo.getSkuName());
            //  cartInfo.setCartPrice(skuInfo.getPrice()); // 这种情况：有可能价格不是最新的.
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            cartInfoExist.setCreateTime(new Date());
            cartInfoExist.setUpdateTime(new Date());

            //  将对象放入缓存. hset key field value
            //  this.redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfo);
        }

        //  添加到缓存
        this.redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);

    }

    /**
     * 购物车列表
     * @param userId
     * @param userTempId
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {

        //  ----------------以下是属于未登录的情况  userTempId 不是空 userId 是空-------------------------------------------
        //  声明一个未登录购物车集合列表
        List<CartInfo> cartInfoNoLoginList = new ArrayList<>();
        //  判断  单独测试 ： 登录 与 未登录 ，暂时不考虑合并的状况！
        if (!StringUtils.isEmpty(userTempId)){
            //  查询临时购物车
            //  获取购物车的key
            String cartKey = this.getCartKey(userTempId);
            //  每次只获取一条数据 hget key field  hvals key 使用这条命令查询！
            cartInfoNoLoginList = this.redisTemplate.opsForHash().values(cartKey);
        }

        //  将未登录购物车集合列表进行排序并返回.
        //  如果不加 这个 判断 && StringUtils.isEmpty(userId) userTempId = 111 userId = 1 也会走！ 那么就不会走合并购物车了。
        if (!CollectionUtils.isEmpty(cartInfoNoLoginList) && StringUtils.isEmpty(userId)){
            cartInfoNoLoginList.sort((o1,o2)->{
                //  按照更新时间进行排序.
                //  第一个，第二个表示传入的日期，第三个参数：表示根据那个时间单位进行比较.
                return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(), Calendar.SECOND);
            });
            //  返回未登录集合列表.
            return cartInfoNoLoginList;
        }

        //------------------------当用户Id 不为空------------------------------------------
        if (!StringUtils.isEmpty(userId)){
            /*
            demo:
                登录：
                    17  1
                    18  1

                未登录：
                    17  1
                    18  1
                    19  2

                 合并：
                    17  2
                    18  2
                    19  2
             */
            //  查询登录购物车
            //  获取购物车的key
            String cartKey = this.getCartKey(userId);
            //  根据cartKey  来获取数据. hset key field value
            //  跟key 来获取 key field value  的对象 ！BoundHashOperations<H, HK, HV>
            BoundHashOperations<String,String,CartInfo> boundHashOperations = this.redisTemplate.boundHashOps(cartKey);
            //  boundHashOperations.hasKey(); 判断 key 中是否有对应的skuId
            //  未登录购物车集合数据不为空. 此时才会产生合并！
            if (!CollectionUtils.isEmpty(cartInfoNoLoginList)){
                cartInfoNoLoginList.forEach(cartInfoNoLogin -> {
                    //  判断登录购物车中是否包含未登录购物车中的skuId  17 18
                    if (boundHashOperations.hasKey(cartInfoNoLogin.getSkuId().toString())){ // 判断 key 中是否有对应的skuId
                        //  hget key field 获取登录对象
                        CartInfo cartInfoLogin = boundHashOperations.get(cartInfoNoLogin.getSkuId().toString());
                        //  未登录的数据 与 登录的数量加在一起！
                        cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum());
                        //  更新updateTime
                        cartInfoLogin.setUpdateTime(new Date());

                        //  选中状态判断：
                        if (cartInfoNoLogin.getIsChecked().intValue()==1 && cartInfoLogin.getIsChecked().intValue()==0){
                            cartInfoLogin.setIsChecked(1);
                        }
                        //  保存到缓存.
                        this.redisTemplate.boundHashOps(cartKey).put(cartInfoLogin.getSkuId().toString(),cartInfoLogin);
                    }else {
                        // 19  直接保存到缓存.
                        //  因为未登录时，userId 是临时的。要合并，需要将临时用户Id 变为登录的用户Id
                        //  111  ---> 1
                        cartInfoNoLogin.setUserId(userId);
                        cartInfoNoLogin.setCreateTime(new Date());
                        cartInfoNoLogin.setUpdateTime(new Date());
                        this.redisTemplate.boundHashOps(cartKey).put(cartInfoNoLogin.getSkuId().toString(),cartInfoNoLogin);
                    }
                });
            }

            //  返回合并之后的数据 同时将未登录购物车数据删除.
            this.redisTemplate.delete(this.getCartKey(userTempId));

            //  获取到合并之后的数据  hvals key;
            List<CartInfo> listCartInfoLogin = boundHashOperations.values();
            // List<CartInfo> listCartInfoLogin = this.redisTemplate.opsForHash().values(cartKey);

            //  将登录之后的集合数据进行排序：
            listCartInfoLogin.sort((o1,o2)->{
                //  按照更新时间进行排序.
                //  第一个，第二个表示传入的日期，第三个参数：表示根据那个时间单位进行比较.
                return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(), Calendar.SECOND);
            });
            //  返回登录购物车集合数据.
            return listCartInfoLogin;

            //  双重for 循环判断合并!
            //  List<CartInfo> cartInfoLoginList = this.redisTemplate.opsForHash().values(cartKey);
            //  登录与未登录进行遍历判断！ 未登录集合有数据. 双重for 循环判断合并!
            //            if (!CollectionUtils.isEmpty(cartInfoNoLoginList)){
            //                for (CartInfo cartInfo : cartInfoNoLoginList) {
            //                    for (CartInfo info : cartInfoLoginList) {
            //                          17 18
            //                        if (cartInfo.getSkuId().equals(info.getSkuId())){
            //                            //  有相同的商品
            //                              num + num;
            //                        }else {
            //                          19
            //                            //  没有相同的商品
            //                        }
            //                    }
            //                }
            //            }else {
            //                // 登录数据
            //                return cartInfoLoginList;
            //            }
        }
        //  返回结果集。
        return new ArrayList<>();
    }

    @Override
    public void checkCart(Long skuId, String userId, Integer isChecked) {
        //  获取到缓存中购物车的key
        String cartKey = this.getCartKey(userId);
        //  修改数据了. 获取到当前skuId 对应的cartInfo
        CartInfo cartInfo = (CartInfo) this.redisTemplate.opsForHash().get(cartKey, skuId.toString());
        //  CartInfo cartInfo = this.redisTemplate.boundHashOps(cartKey).get(skuId.toString());
        if (cartInfo!=null){
            cartInfo.setIsChecked(isChecked);

            //  不需要：cartInfo.setUpdateTime(new Date());
            //  将修改之后的数据保存到缓存中.
            this.redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfo);
        }

    }

    @Override
    public void deleteCart(Long skuId, String userId) {
        //  获取到缓存的购物车 key
        String cartKey = this.getCartKey(userId);
        //  执行删除命令 不能使用这个命令 del key 而是使用 hdel key field
        //  this.redisTemplate.delete(cartKey);
        this.redisTemplate.opsForHash().delete(cartKey,skuId.toString());

    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        //  根据userId 获取到购物车的key
        String cartKey = this.getCartKey(userId);
        List<CartInfo> cartInfoList = this.redisTemplate.opsForHash().values(cartKey);
        //  List<CartInfo> cartList = new ArrayList<>();
        //  找选中的状态的购物项
        //        if (!CollectionUtils.isEmpty(cartInfoList)){
        //            cartInfoList.forEach(cartInfo -> {
        //                //  选中状态。
        //                if (cartInfo.getIsChecked().intValue()==1) {
        //                    //  我想要获取的数据.
        //                    cartList.add(cartInfo);
        //                }
        //            });
        //        }
        //  判断
        if (!CollectionUtils.isEmpty(cartInfoList)){
            List<CartInfo> cartList  = cartInfoList.stream().filter(cartInfo -> {
                //  只留下选中状态为 1 的数据
                return cartInfo.getIsChecked().intValue() == 1;
            }).collect(Collectors.toList());
            return cartList;
        }
        //  如果没有返回空集合
        return new ArrayList<>();
    }

    private String getCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
    }
}