package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface BaseTrademarkService extends IService<BaseTrademark> {
    /**
     * 查询品牌分页数据
     * @param baseTrademarkPage
     * @return
     */
    IPage<BaseTrademark> getBaseTradeMarkList(Page<BaseTrademark> baseTrademarkPage);

}
