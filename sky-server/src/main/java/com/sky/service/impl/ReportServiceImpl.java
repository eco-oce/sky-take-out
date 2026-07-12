package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
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
}
