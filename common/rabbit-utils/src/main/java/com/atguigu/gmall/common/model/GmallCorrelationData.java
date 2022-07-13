package com.atguigu.gmall.common.model;

import lombok.Data;
import org.springframework.amqp.rabbit.connection.CorrelationData;

/**
 * author:atGuiGu-mqx
 * date:2022/5/23 9:35
 * 描述：
 **/
@Data
public class GmallCorrelationData extends CorrelationData {
    //  定义一些字段：
    //  消息主体
    private Object message;
    //  交换机
    private String exchange;
    //  路由键
    private String routingKey;
    //  是否是延迟消息
    private boolean isDelay = false;
    //  重试次数
    private int retryCount = 0;
    //  延迟时间
    private int delayTime = 10;

}