package com.supermall.mall.controller;

import com.supermall.mall.service.MallFileService;
import com.supermall.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 运营后台 - 图片上传（商品多图）。认证：请求头 mall-admin-token。
 *
 * <p>图片直接进 OSS（目录 {@code supermall/mall/}），返回可公网访问的 URL；
 * 库里只存 URL，不存二进制。用法：先上传拿到 URL，再把 URL 数组放进
 * {@code MallProductSaveDTO.images} 提交即可。
 */
@RestController
@RequestMapping("/api/mall/admin/file")
@Tag(name = "电商后台-文件上传")
@Slf4j
@RequiredArgsConstructor
public class MallAdminFileController {

    private static final int MAX_BATCH = 10;

    private final MallFileService mallFileService;

    @PostMapping("/upload")
    @Operation(summary = "上传单张图片到 OSS，返回 URL")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "name", required = false) String name) {
        return Result.success(mallFileService.upload(file, name));
    }

    @PostMapping("/upload-batch")
    @Operation(summary = "批量上传图片（最多 10 张），返回 URL 列表（顺序与入参一致）")
    public Result<List<String>> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        return Result.success(mallFileService.uploadBatch(files, MAX_BATCH));
    }
}
