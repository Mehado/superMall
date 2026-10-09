package com.supermall.mall.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.utils.ImageFileValidator;
import com.supermall.mall.service.MallFileService;
import com.supermall.properties.AliOssProperties;
import com.supermall.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传实现：直接进 OSS，返回可公网访问的 URL。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallFileServiceImpl implements MallFileService {

    private final AliOssUtil aliOssUtil;
    private final AliOssProperties ossProperties;

    @Override
    public String upload(MultipartFile file, String name) {
        // 类型白名单 + 大小上限：上传接口不能只靠 multipart 的总大小限制
        String bad = ImageFileValidator.check(file, ImageFileValidator.MAX_PRODUCT_IMAGE_BYTES);
        if (bad != null) {
            throw new MallBusinessException(bad);
        }
        String objectName = ossProperties.getDir()
                + (hasText(name) ? name.trim() : UUID.randomUUID() + extOf(file));
        try {
            String url = aliOssUtil.upload(file.getBytes(), objectName, file.getContentType());
            log.info("[mall] 上传图片：{} -> {}", file.getOriginalFilename(), url);
            return url;
        } catch (IOException e) {
            log.error("[mall] 上传图片失败：{}", file.getOriginalFilename(), e);
            throw new MallBusinessException("图片上传失败，请稍后重试");
        }
    }

    /** 头像大小上限：2MB */
    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_EXT =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new MallBusinessException(MessageConstant.AVATAR_EMPTY);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new MallBusinessException(MessageConstant.AVATAR_TYPE_INVALID);
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new MallBusinessException(MessageConstant.AVATAR_TOO_LARGE);
        }
        // 说明：头像这条链路本来就有校验；上面的 ImageFileValidator 是给商品图补的同一套规则
        // 注意：extOf 返回的是带点的（".png"，给 upload 拼文件名用），
        // 这里白名单与拼名都需要不带点的扩展名
        String ext = extNameOf(file);
        if (!ALLOWED_AVATAR_EXT.contains(ext)) {
            throw new MallBusinessException(MessageConstant.AVATAR_TYPE_INVALID);
        }

        String objectName = ossProperties.getAvatarDir() + userId + "/" + UUID.randomUUID() + "." + ext;
        try {
            String url = aliOssUtil.upload(file.getBytes(), objectName, contentType);
            log.info("[mall] 买家上传头像：userId={}, {} -> {}", userId, file.getOriginalFilename(), url);
            return url;
        } catch (IOException e) {
            log.error("[mall] 买家头像上传失败：userId={}, file={}", userId, file.getOriginalFilename(), e);
            throw new MallBusinessException(MessageConstant.AVATAR_UPLOAD_FAILED);
        }
    }

    @Override
    public List<String> uploadBatch(MultipartFile[] files, int max) {
        if (files == null || files.length == 0) {
            throw new MallBusinessException("上传的文件为空");
        }
        if (files.length > max) {
            throw new MallBusinessException("一次最多上传 " + max + " 张");
        }
        List<String> urls = new ArrayList<>(files.length);
        for (MultipartFile f : files) {
            urls.add(upload(f, null));
        }
        return urls;
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String extOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null) {
            int i = original.lastIndexOf('.');
            if (i > -1) {
                return original.substring(i).toLowerCase(Locale.ROOT);
            }
        }
        String ct = file.getContentType();
        if (ct != null && ct.contains("/")) {
            return "." + ct.substring(ct.indexOf('/') + 1).toLowerCase(Locale.ROOT);
        }
        return ".jpg";
    }

    /** 扩展名（不带点，小写） */
    private String extNameOf(MultipartFile file) {
        String ext = extOf(file);
        return ext.startsWith(".") ? ext.substring(1) : ext;
    }
}
