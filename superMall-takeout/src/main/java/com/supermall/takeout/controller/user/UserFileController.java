package com.supermall.takeout.controller.user;

import com.supermall.context.BaseContext;
import com.supermall.result.Result;
import com.supermall.takeout.service.UserFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户端 - 头像上传。认证：请求头 {@code token}。
 *
 * <p>用法：先上传拿到 URL，再调 {@code PUT /api/user/profile} 把 URL 存进 {@code avatar}。
 * 分两步是为了与商家端 / 电商买家保持一致（库里只存 URL，不存二进制）。
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户端-头像上传")
@Slf4j
@RequiredArgsConstructor
public class UserFileController {

    private final UserFileService userFileService;

    @PostMapping("/avatar")
    @Operation(summary = "上传头像到 OSS（≤2MB 的图片），返回可公网访问的 URL",
            description = "上传成功后还需调 PUT /api/user/profile 保存 avatar 字段")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(userFileService.uploadAvatar(file, BaseContext.getCurrentId()));
    }
}
