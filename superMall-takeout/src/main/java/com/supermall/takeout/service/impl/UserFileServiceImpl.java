package com.supermall.takeout.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.properties.AliOssProperties;
import com.supermall.takeout.exception.ProductBusinessException;
import com.supermall.takeout.service.UserFileService;
import com.supermall.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 用户端文件上传实现（头像）。
 *
 * <p>校验顺序：非空 → 类型是图片 → 大小 ≤ 2MB → 扩展名在白名单里。
 * 前三条是硬约束（避免把非图片 / 巨图写进 OSS），最后一条只是规范化文件名。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserFileServiceImpl implements UserFileService {

    /** 头像大小上限：2MB */
    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private final AliOssUtil aliOssUtil;
    private final AliOssProperties ossProperties;

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new ProductBusinessException(MessageConstant.AVATAR_EMPTY);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ProductBusinessException(MessageConstant.AVATAR_TYPE_INVALID);
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ProductBusinessException(MessageConstant.AVATAR_TOO_LARGE);
        }
        String ext = extOf(file);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ProductBusinessException(MessageConstant.AVATAR_TYPE_INVALID);
        }

        String objectName = ossProperties.getAvatarDir() + userId + "/" + UUID.randomUUID() + "." + ext;
        try {
            String url = aliOssUtil.upload(file.getBytes(), objectName, contentType);
            log.info("用户上传头像：userId={}, {} -> {}", userId, file.getOriginalFilename(), url);
            return url;
        } catch (IOException e) {
            log.error("用户头像上传失败：userId={}, file={}", userId, file.getOriginalFilename(), e);
            throw new ProductBusinessException(MessageConstant.AVATAR_UPLOAD_FAILED);
        }
    }

    /** 取扩展名（不带点，小写）；识别不出来时按 content-type 猜，再兜底 jpg */
    private String extOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null) {
            int i = original.lastIndexOf('.');
            if (i > -1) {
                return original.substring(i + 1).toLowerCase(Locale.ROOT);
            }
        }
        String ct = file.getContentType();
        if (ct != null && ct.contains("/")) {
            return ct.substring(ct.indexOf('/') + 1).toLowerCase(Locale.ROOT);
        }
        return "jpg";
    }
}
