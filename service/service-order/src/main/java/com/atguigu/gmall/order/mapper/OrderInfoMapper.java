package com.atguigu.gmall.order.mapper;

import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * author:atGuiGu-mqx
 * date:2022/5/13 11:44
 * 描述：
 **/
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    /**
     * 查看我的订单
     * @param orderInfoPage
     * @param userId
     * @return
     */
    IPage<OrderInfo> selectOrderPage(Page<OrderInfo> orderInfoPage, @Param("userId") String userId);
}
