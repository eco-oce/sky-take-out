package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 定时任务1：自动处理15分钟未支付的超时订单
     * cron表达式：0 * * * * ?  每分钟0秒执行一次（每分钟扫描一次超时订单）
     */
    @Scheduled(cron ="0 * * * * ?") //每分钟触发一次
    public void processTimeoutOrder(){
        log.info("定时处理超时订单：{}", LocalDateTime.now());

        // 计算超时临界点：当前时间 减去15分钟
        // plusMinutes(-15)等价于minusMinutes(15)，代表15分钟前的时间
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        // 调用Mapper动态查询：
        // 条件1：订单状态=待付款(PENDING_PAYMENT)
        // 条件2：下单时间 < 15分钟前（下单超过15分钟未支付）
        //select * from orders where status = ? and order_time <(当前时间-15分钟)
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);

        if(ordersList != null && ordersList.size()>0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 定时任务2：每日凌晨1点处理长期处于派送中的订单，自动标记为已完成
     * cron表达式：0 0 1 * * ?  每天凌晨1点整执行一次
     */
    @Scheduled(cron = "0 0 1 * * ?") //每天凌晨一点触发一次
    public void processDeliveryOrder(){
        log.info("处理一直处于配送中的订单,{}", LocalDateTime.now());

        // 计算时间临界点：当前时间减去60分钟（1小时前）
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);

        //select * from orders where status=Orders.DELIVERY_IN_PROGRESS
        List<Orders> ordersList=orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS,time);
        if(ordersList!=null && ordersList.size()>0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }

}
