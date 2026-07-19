package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内的营业额数据
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放从begin到end范围内的每天日期
        List<LocalDate> dateList =new ArrayList<>();
        dateList.add(begin);

        // 只要当前日期不等于结束日期，就继续向后+1天
        while (!begin.equals(end)) {
            //日期计算，计算指定日期的后一天对应的日期
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每一天对应的营业额，顺序和dateList一一对应
        List<Double> turnoverList = new ArrayList<>();

        for (LocalDate date : dateList) {
            //查询date日期对应的营业额数据（状态为已完成的订单金额合计）,将日期转为完整时分秒时间
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);    //当天 00:00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);      //当天 23:59:59.999999999

            //封装查询条件：当日时间段 + 订单状态=已完成
            Map map=new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            //select sum(amount) form orders where order_time > beginTime and order_time < endTime and status = 5
            Double turnover=orderMapper.sumByMap(map);
            turnover=turnover == null ? 0.0 :turnover;
            turnoverList.add(turnover);
        }

        //封装返回结果, StringUtils.join：集合使用逗号拼接成字符串，满足前端echarts折线图格式要求
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end之间每天对应的日期
        List<LocalDate> dateList =new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)) {
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的新增用户数量 select count(id) from user where create_time>begin and create_time<end
        List<Integer> newUserList = new ArrayList<>();
        //存放每天的总用户数量 select count(id) from user where create_time< end
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map=new HashMap();
            map.put("end",endTime);
            //总用户数量
            Integer totalUser=userMapper.countByMap(map);

            map.put("begin",beginTime);
            //新增用户数量
            Integer newUser=userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        //封装结果数据
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();

    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end之间每天对应的日期
        List<LocalDate> dateList =new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)) {
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的订单总数
        List<Integer> orderCountList = new ArrayList<>();
        //存放每天的有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();

        //编辑datelist集合，查询每天的有效订单数和订单总数
        for (LocalDate date : dateList) {
            //查询每天订单总数 select count(id) from orders where order_time > ? and order_time < ?
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            //查询每天有效订单数 select count(id) from orders where order_time > ? and order_time < ? and order_status=5
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        //计算时间区间内的订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //计算时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        //计算订单完成率
        Double orderCompletionRate=0.0;
        if(totalOrderCount != 0){
            orderCompletionRate=validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }


    /**
     * 根据条件统计订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end,Integer status) {
        Map map=new HashMap();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);

        return orderMapper.countByMap(map);
    }
}
