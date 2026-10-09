package com.supermall.takeout.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OrderPageQueryDTO implements Serializable {

    @Min(value = 1, message = "页码不能小于1")
    private int page = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    private int pageSize = 10;

    private String number;

    private String phone;

    private String status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 用户端查询时由服务端写入当前用户 id */
    private Long userId;

    /** 商家端查询时由服务端写入当前商家 id */
    private Long merchantId;
}
