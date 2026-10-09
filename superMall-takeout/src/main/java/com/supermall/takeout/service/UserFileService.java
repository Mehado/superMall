package com.supermall.takeout.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 用户端文件上传（目前只有头像）。
 *
 * <p>惯例与商家端一致：**先上传拿到 OSS URL，再把 URL 存进 {@code sys_user.avatar}**
 * （保存走 {@code PUT /api/user/profile}）。库里不存二进制。
 */
public interface UserFileService {

    /**
     * 上传头像到 OSS，返回公网 URL。
     *
     * @param userId 当前登录用户 id，用于按用户分目录：{@code supermall/avatars/{userId}/xxx}
     */
    String uploadAvatar(MultipartFile file, Long userId);
}
