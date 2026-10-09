package com.supermall.mall.controller;

import com.supermall.context.BaseContext;
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
 * 商城买家 - 图片上传（评价晒图）。认证：请求头 token。
 *
 * <p>与后台上传共用 {@link MallFileService}，只是鉴权是买家、单次上限 9 张
 * （与评价晒图上限一致）。用法：先上传拿到 URL，再把 URL 数组放进
 * {@code MallReviewItemDTO.images} / {@code MallReviewFollowDTO.images} 提交。
 */
@RestController
@RequestMapping("/api/mall/user/file")
@Tag(name = "电商-买家文件上传")
@Slf4j
@RequiredArgsConstructor
public class MallUserFileController {

    private static final int MAX_BATCH = 9;

    private final MallFileService mallFileService;

    @PostMapping("/upload")
    @Operation(summary = "上传单张晒图到 OSS，返回 URL")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "name", required = false) String name) {
        return Result.success(mallFileService.upload(file, name));
    }

    @PostMapping("/upload-batch")
    @Operation(summary = "批量上传晒图（最多 9 张），返回 URL 列表（顺序与入参一致）")
    public Result<List<String>> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        return Result.success(mallFileService.uploadBatch(files, MAX_BATCH));
    }

    @PostMapping("/avatar")
    @Operation(summary = "上传买家头像到 OSS（≤2MB 图片），返回可公网访问的 URL",
            description = "上传成功后还需调 PUT /api/mall/user/profile 保存 avatar 字段")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(mallFileService.uploadAvatar(file, BaseContext.getCurrentId()));
    }
}
