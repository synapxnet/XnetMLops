package com.synapxnet.mlopsdppservice.service;

import com.synapxnet.mlopsdppservice.entity.ScheduleConfig;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class ScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);
    private final ObjectMapper objectMapper;

    public ScheduleService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将scheduleConfig转换为cron表达式
     */
    public String convertToCronExpression(String scheduleConfigJson) {
        try {
            ScheduleConfig config = objectMapper.readValue(scheduleConfigJson, ScheduleConfig.class);

            if (config.getCronExpression() != null && !config.getCronExpression().isEmpty()) {
                // 如果已经提供了cron表达式，直接使用
                return config.getCronExpression();
            }

            return generateCronFromConfig(config);

        } catch (Exception e) {
            logger.error("解析scheduleConfig失败: {}", e.getMessage());
            return ""; // 返回空字符串表示不启用调度
        }
    }

    /**
     * 根据配置生成cron表达式
     */
    private String generateCronFromConfig(ScheduleConfig config) {
        String intervalType = config.getIntervalType();

        if (intervalType == null) {
            return "";
        }

        switch (intervalType) {
            case "daily":
                return generateDailyCron(config);
            case "weekly":
                return generateWeeklyCron(config);
            case "hourly":
                return generateHourlyCron(config);
            case "monthly":
                return generateMonthlyCron(config);
            case "interval":
                return generateIntervalCron(config);
            default:
                return ""; // 默认不启用
        }
    }

    /**
     * 生成每日执行的cron表达式
     * 格式: 分钟 小时 * * *
     */
    private String generateDailyCron(ScheduleConfig config) {
        if (config.getDailyTime() == null || config.getDailyTime().isEmpty()) {
            return "0 0 * * *"; // 默认每天0点执行
        }

        String[] timeParts = config.getDailyTime().split(":");
        if (timeParts.length != 2) {
            return "0 0 * * *";
        }

        try {
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            return String.format("%d %d * * *", minute, hour);
        } catch (NumberFormatException e) {
            return "0 0 * * *";
        }
    }

    /**
     * 生成每周执行的cron表达式
     * 格式: 分钟 小时 * * 周几(0-6, 0=周日)
     */
    private String generateWeeklyCron(ScheduleConfig config) {
        String time = config.getWeeklyTime() != null ? config.getWeeklyTime() : "00:00";
        List<String> days = config.getWeeklyDays();

        String[] timeParts = time.split(":");
        int hour = 0;
        int minute = 0;

        if (timeParts.length == 2) {
            try {
                hour = Integer.parseInt(timeParts[0]);
                minute = Integer.parseInt(timeParts[1]);
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }

        if (days == null || days.isEmpty()) {
            // 默认周一执行
            return String.format("%d %d * * 1", minute, hour);
        }

        // 将周几转换为cron格式 (0=周日, 1=周一, ..., 6=周六)
        StringBuilder daysStr = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            if (i > 0) daysStr.append(",");
            String day = days.get(i);
            switch (day.toLowerCase()) {
                case "sunday": case "sun": daysStr.append("0"); break;
                case "monday": case "mon": daysStr.append("1"); break;
                case "tuesday": case "tue": daysStr.append("2"); break;
                case "wednesday": case "wed": daysStr.append("3"); break;
                case "thursday": case "thu": daysStr.append("4"); break;
                case "friday": case "fri": daysStr.append("5"); break;
                case "saturday": case "sat": daysStr.append("6"); break;
                default: daysStr.append(day);
            }
        }

        return String.format("%d %d * * %s", minute, hour, daysStr.toString());
    }

    /**
     * 生成每小时执行的cron表达式
     * 格式: 分钟 * * * *
     */
    private String generateHourlyCron(ScheduleConfig config) {
        String minuteStr = config.getHourlyMinute();
        int minute = 0;

        if (minuteStr != null && !minuteStr.isEmpty()) {
            try {
                minute = Integer.parseInt(minuteStr);
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }

        return String.format("%d * * * *", minute);
    }

    /**
     * 生成每月执行的cron表达式
     * 格式: 分钟 小时 日 * *
     */
    private String generateMonthlyCron(ScheduleConfig config) {
        String time = config.getDailyTime() != null ? config.getDailyTime() : "00:00";
        String[] timeParts = time.split(":");
        int hour = 0;
        int minute = 0;

        if (timeParts.length == 2) {
            try {
                hour = Integer.parseInt(timeParts[0]);
                minute = Integer.parseInt(timeParts[1]);
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }

        return String.format("%d %d 1 * *", minute, hour); // 默认每月1号执行
    }

    /**
     * 生成间隔执行的cron表达式
     */
    private String generateIntervalCron(ScheduleConfig config) {
        Integer duration = config.getIntervalDuration();
        if (duration == null || duration <= 0) {
            duration = 1;
        }

        String unit = config.getIntervalUnit();
        if (unit == null) {
            unit = "hours";
        }

        switch (unit.toLowerCase()) {
            case "minutes":
                return String.format("*/%d * * * *", duration);
            case "hours":
                return String.format("0 */%d * * *", duration);
            case "days":
                return String.format("0 0 */%d * *", duration);
            default:
                return String.format("0 */%d * * *", duration);
        }
    }

    /**
     * 检查调度是否激活
     */
    public boolean isScheduleActive(String scheduleConfigJson) {
        try {
            ScheduleConfig config = objectMapper.readValue(scheduleConfigJson, ScheduleConfig.class);
            return config.getIsActive() != null && config.getIsActive();
        } catch (Exception e) {
            logger.error("检查调度状态失败: {}", e.getMessage());
            return false;
        }
    }
}
