package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/5/13 11:41
 * 描述：
 **/
public interface OrderService extends IService<OrderInfo> {

    /**
     * 保存订单接口
     * @param orderInfo
     * @return 订单Id
     */
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 校验库存.
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 创建流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     * @param tradeNo   页面提交的流水号
     * @param userId    获取缓存的流水号
     * @return
     */
    Boolean checkTradeNo(String tradeNo,String userId);

    /**
     * 删除流水号
     * @param userId
     */
    void delTradeNo(String userId);

    /**
     * 查询我的订单
     * @param orderInfoPage
     * @param userId
     * @return
     */
    IPage<OrderInfo> getOrderPage(Page<OrderInfo> orderInfoPage, String userId);

    /**
     * 取消订单
     * @param orderId
     */
    void execExpiredOrder(Long orderId);

    /**
     * 根据订单Id，修改订单状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);


    /**
     * 根据订单Id 查询订单对象
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 订单发送消息给库存.
     * @param orderId
     */
    void sendOrderStatus(Long orderId);

    /**
     * 将orderInfo 变为map
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单方法。
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);

    /**
     * 根据flag 进行关闭交易
     * @param orderId
     * @param flag
     */
    void execExpiredOrder(Long orderId, String flag);
}
