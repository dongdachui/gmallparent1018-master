package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * author:atGuiGu-mqx
 * date:2022/5/13 11:42
 * 描述：
 **/
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper,OrderInfo> implements OrderService  {

    //  本质：向数据库表中添加数据.order_info order_detail
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Value("${ware.url}")
    private String wareUrl; // wareUrl=http://localhost:9001


    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        //  http://localhost:9001/hasStock?skuId=10221&num=2
        //  远程调用库存系统. http://localhost:9001
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        //  返回 1 有库存  0  没有库存.
        return "1".equals(result);
    }

    @Override
    public String getTradeNo(String userId) {
        //  声明一个流水号
        String tradeNo = UUID.randomUUID().toString();
        //  将流水号放入缓存.
        String tradeNoKey = "tradeNo:"+userId;
        //  给上一个过期时间.
        this.redisTemplate.opsForValue().set(tradeNoKey,tradeNo, RedisConst.TRADENO_KEY_TIMEOUT, TimeUnit.SECONDS);
        //  返回流水号
        return tradeNo;
    }

    @Override
    public Boolean checkTradeNo(String tradeNo, String userId) {
        //  获取到缓存的流水号
        String tradeNoKey = "tradeNo:"+userId;
        String redisTradeNo = (String) this.redisTemplate.opsForValue().get(tradeNoKey);
        //        if (tradeNo.equals(redisTradeNo)){
        //            return true;
        //        }else {
        //            return false;
        //        }
        //  返回比较结果
        return tradeNo.equals(redisTradeNo);
    }

    @Override
    public void delTradeNo(String userId) {
        //  获取缓存的key
        String tradeNoKey = "tradeNo:"+userId;
        this.redisTemplate.delete(tradeNoKey);
    }

    /**
     * 取消订单方法.
     * @param orderId
     */
    @Override
    public void execExpiredOrder(Long orderId) {
        //  本质：关闭订单 CLOSED CLOSED
        //        OrderInfo orderInfo = new OrderInfo();
        //        orderInfo.setId(orderId);
        //        orderInfo.setOrderStatus(OrderStatus.CLOSED.name());
        //        orderInfo.setProcessStatus(ProcessStatus.CLOSED.name());
        //        this.orderInfoMapper.updateById(orderInfo);
        //  后续业务代码中会有很多根据订单Id 来修改订单状态的，因此我们将根据订单Id 修改状态再提出一个方法。
        this.updateOrderStatus(orderId,ProcessStatus.CLOSED);

        //  发送一个信息给paymentInfo. 发送消息的主体：根据消费者的需求来判断的。
        this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);

    }

    /**
     * 发送消息给库存系统
     * @param orderId
     */
    @Override
    public void sendOrderStatus(Long orderId) {
        //  发送消息给库存，所以要更改订单状态.
        this.updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);

        //  根据订单Id 来获取订单对象
        OrderInfo orderInfo = this.getOrderInfo(orderId);

        //  7个字段属于订单，1个字段属于订单明细. 将这个8个字段看做 map 的key 然后将map 转换json 字符串，在发送给库存系统.
        Map map = this.initWareOrder(orderInfo);
        //  发送消息
        this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK, JSON.toJSONString(map));
    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        //  声明子订单集合
        ArrayList<OrderInfo> orderInfoArrayList = new ArrayList<>();
        /*
        1.  先获取到原始订单。
        2.  将wareSkuMap 变为能操作的对象  [{"wareId":"1","skuIds":["21","22"]},{"wareId":"2","skuIds":["24"]}]
        3.  创建新的子订单并且给子订单赋值
        4.  将子订单添加到集合中
        5.  将子订单保存到orderInfo,orderDetail 中
        6.  改变原始订单状态  SPLIT
         */
        OrderInfo orderInfoOrigin = this.getOrderInfo(Long.parseLong(orderId));
        //  将wareSkuMap 变为能操作的对象
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        //  循环遍历
        for (Map map : mapList) {
            //  获取仓库Id
            String wareId = (String) map.get("wareId");
            //  获取仓库Id 对应的商品集合数据
            List<String> skuIdList = (List<String>) map.get("skuIds");
            //  创建新的子订单并且给子订单赋值
            OrderInfo subOrderInfo = new OrderInfo();
            //  属性拷贝
            BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
            //  为了防止主键冲突，将id 设置为null
            subOrderInfo.setId(null);
            //  设置parentOrderId
            subOrderInfo.setParentOrderId(Long.parseLong(orderId));
            //  设置仓库Id
            subOrderInfo.setWareId(wareId);
            //  声明一个订单明细集合
            ArrayList<OrderDetail> orderDetailArrayList = new ArrayList<>();
            //  子订单金额需要重新计算。
            //  [{"wareId":"1","skuIds":["21","22"]}, {"wareId":"2","skuIds":["24"]}] mapList.size() = 2;
            //  1 = {"wareId":"1","skuIds":["21","22"]}  2 = {"wareId":"2","skuIds":["24"]}
            //  先获取到拆单之前的订单明细。 orderDetailList [21,22,24]
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            for (OrderDetail orderDetail : orderDetailList) {
                for (String skuId : skuIdList) {
                    if (orderDetail.getSkuId().longValue()==Long.parseLong(skuId)){
                        //  记录当前的订单明细集合.
                        orderDetailArrayList.add(orderDetail);
                    }
                }
            }
            //  将子订单明细集合放入子订单中  子订单1 21 2199*2+ 1999*1  22   子订单2 24 3499
            subOrderInfo.setOrderDetailList(orderDetailArrayList);
            subOrderInfo.sumTotalAmount();
            //  将子订单添加到集合中
            orderInfoArrayList.add(subOrderInfo);
            //  保存子订单
            this.saveOrderInfo(subOrderInfo);
        }
        //  修改子订单状态.
        this.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.SPLIT);
        //  返回子订单集合
        return orderInfoArrayList;
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        //  后续业务代码中会有很多根据订单Id 来修改订单状态的，因此我们将根据订单Id 修改状态再提出一个方法。
        this.updateOrderStatus(orderId,ProcessStatus.CLOSED);
        //  判断是否要关闭paymentInfo
        if ("2".equals(flag)){
            //  发送一个信息给paymentInfo. 发送消息的主体：根据消费者的需求来判断的。
            this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);

        }

    }

    /**
     * 将orderInfo 转换为map 集合.
     * @param orderInfo
     * @return
     */
    public Map initWareOrder(OrderInfo orderInfo) {
        //  什么map集合来接收orderInfo 字段.
        HashMap<String, Object> hashMap = new HashMap<>();
        //  7个字段，orderDetail 1 个 变为hashMap
        hashMap.put("orderId",orderInfo.getId());
        hashMap.put("consignee",orderInfo.getConsignee());
        hashMap.put("consigneeTel",orderInfo.getConsigneeTel());
        hashMap.put("orderComment",orderInfo.getOrderComment());
        hashMap.put("orderBody",orderInfo.getTradeBody());
        hashMap.put("deliveryAddress",orderInfo.getDeliveryAddress());
        hashMap.put("paymentWay","2");
        //  添加仓库Id 地址。
        hashMap.put("wareId",orderInfo.getWareId());
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //  订单明细;  details:[{skuId:101,skuNum:1,skuName:’小米手64G’},{skuId:201,skuNum:1,skuName:’索尼耳机’}]
        /*
          第一种：自定义一个class 来存储这个三个属性；
          class Param{
            private Long skuId;
            private Integer skuNum;
            private String skuName;
          }
          第二种： 可以 将这个三个属性看做是map的key；
          map.put("skuId","101");
          map.put("skuNum",1);
          map.put("skuName","小米手64G");
          class === map
         */
        //  参数是谁：主要看orderDetailList 集合的泛型
        List<HashMap<String, Object>> arrayList = orderDetailList.stream().map(orderDetail -> {
            //  声明一个map 来存储订单明细相关数据
            HashMap<String, Object> map = new HashMap<>();
            map.put("skuId", orderDetail.getSkuId());
            map.put("skuNum", orderDetail.getSkuNum());
            map.put("skuName", orderDetail.getSkuName());
            //  返回map 集合
            return map;
        }).collect(Collectors.toList()); // 将返回的map 组装成集合。
        hashMap.put("details",arrayList);

        //  返回map
        return hashMap;
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        //  先查询订单对象
        OrderInfo orderInfo = this.orderInfoMapper.selectById(orderId);
        //  getOrderInfo 与  getById() 区别： getOrderInfo 需要获取到订单明细.
        if (orderInfo!=null){
            QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
            orderDetailQueryWrapper.eq("order_id",orderId);
            List<OrderDetail> orderDetailList = this.orderDetailMapper.selectList(orderDetailQueryWrapper);
            orderInfo.setOrderDetailList(orderDetailList);
        }
        //  返回
        return orderInfo;
    }

    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        // processStatus =  ProcessStatus.CLOSED   processStatus.getOrderStatus().name() = OrderStatus.CLOSED.name()
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name()); // processStatus.name() = ProcessStatus.CLOSED.name()
        this.orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public IPage<OrderInfo> getOrderPage(Page<OrderInfo> orderInfoPage, String userId) {
        //  查询我的订单：orderInfo orderDetail
        IPage<OrderInfo> infoIPage = orderInfoMapper.selectOrderPage(orderInfoPage,userId);
        //  获取到订单状态：中文
        infoIPage.getRecords().stream().forEach(orderInfo -> {
            orderInfo.setOrderStatusName(OrderStatus.getStatusNameByStatus(orderInfo.getOrderStatus()));
        });
        //  返回数据。
        return infoIPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrderInfo(OrderInfo orderInfo) {
        //  order_info 会返回一个订单Id IdType.AUTO
        //  orderInfo 缺少的字段：total_amount,order_status，user_id,out_trade_no,trade_body,operate_time,
        //  expire_time,process_status
        //  总金额：调用方法计算
        orderInfo.sumTotalAmount();
        //  订单状态：order_status
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //  user_id
        //  对接支付时的交易编号,这个交易编号不能重复，必须保证它的唯一性 ：out_trade_no
        // String outTradeNo = "ATGUIGU"+ UUID.randomUUID().toString();
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //  订单主体介绍或订单描述 ： trade_body
        orderInfo.setTradeBody("ATGUIGU---Pay");
        //  操作时间
        orderInfo.setOperateTime(new Date());
        //  订单过期时间：expire_time 默认每个订单的过期时间为1天
        Calendar calendar = Calendar.getInstance();
        //  在当前系统时间上 +1天
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //  订单进度状态：
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        orderInfoMapper.insert(orderInfo);

        //  order_detail 用户购买的所有商品都存储在订单明细中
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            //  order_id 前端页面没有传递orderId  orderid = null;
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }

        //  在下订单的时候，要发送延迟消息 ！ 根据发送的内容进行取消订单操作.
        this.rabbitService.sendDelayMsg(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,MqConst.ROUTING_ORDER_CANCEL,orderInfo.getId(),MqConst.DELAY_TIME);
        //  返回订单Id
        return orderInfo.getId();
    }
}