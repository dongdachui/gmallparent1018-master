package com.atguigu.gmall.task.config;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * author:atGuiGu-mqx
 * date:2022/5/20 9:40
 * 描述：
 **/
@EnableScheduling
@Component
public class ScheduledTask {

    @Autowired
    private RabbitService rabbitService;

    //  定义定时任务的规则.  每隔10秒钟触发一次定时任务.
    @Scheduled(cron = "0/10 * * * * ?")
    public void test(){
        //  System.out.println("来人了，开始接客吧.");
        //  本质将秒杀数据放入到缓存.
        //   目的：解耦  秒杀模块与定时任务模块.
        rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_1,"ok");
    }

    //  编写一个定时任务： 每天晚上18点钟删除数据. 18 点钟以后没有秒杀场景！
    @Scheduled(cron = "* * 18 * * ?")
    public void clearRedisData(){
        //  System.out.println("来人了，开始接客吧.");
        //  本质将秒杀数据放入到缓存.
        //   目的：解耦  秒杀模块与定时任务模块.
        rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_18,"del...");
    }
}