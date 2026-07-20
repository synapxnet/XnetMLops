package com.synapxnet.mlopsmtpservice.entity;

import lombok.Data;
import java.util.List;

@Data
public class ScheduleConfig {
    private Boolean isActive;
    private String dailyTime;          // 格式: "HH:mm"
    private List<String> dateRange;    // 日期范围
    private String offsetTime;         // 偏移时间
    private List<String> weeklyDays;   // 周几执行
    private String weeklyTime;         // 周执行时间
    private String hourlyMinute;       // 每小时的第几分钟
    private String intervalType;       // daily, weekly, hourly, monthly
    private String intervalUnit;       // hours, minutes, days
    private String cronExpression;     // 手动设置的cron表达式
    private Integer intervalDuration;  // 间隔时长
}