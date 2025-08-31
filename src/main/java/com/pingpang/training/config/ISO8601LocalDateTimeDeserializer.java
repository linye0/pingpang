package com.pingpang.training.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ISO8601LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 处理 ISO8601 格式，例如 "2025-08-31T09:00:00.000Z"
            if (value.endsWith("Z")) {
                // 将UTC时间转换为服务器本地时区的时间
                return OffsetDateTime.parse(value)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
            }
            // 处理包含时区偏移的格式，例如 "2025-08-31T09:00:00+08:00"
            else if (value.contains("+") || value.matches(".*-\\d{2}:\\d{2}$")) {
                // 将带时区的时间转换为服务器本地时区的时间
                return OffsetDateTime.parse(value)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
            }
            // 处理标准 LocalDateTime 格式，例如 "2025-08-31T09:00:00"
            else if (value.contains("T")) {
                return LocalDateTime.parse(value);
            }
            // 处理传统格式，例如 "2025-08-31 09:00:00"
            else {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            throw new IOException("无法解析日期时间格式: " + value, e);
        }
    }
} 