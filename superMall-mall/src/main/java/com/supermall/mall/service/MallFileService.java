package com.supermall.mall.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图片上传（到 OSS，库里只存 URL）。
 *
 * <p>后台（商品多图）与买家（评价晒图）共用同一套实现，只是鉴权与单次上限不同：
 * 后台最多 10 张、买家最多 9 张（与评价晒图上限一致）。
 */
public interface MallFileService {

    /** 上传单张，返回公网 URL；{@code name} 为空则用 UUID 生成文件名 */
    String upload(MultipartFile file, String name);

    /**
     * 上传买家头像，返回公网 URL。
     *
     * <p>与 {@link #upload} 的区别：会校验「图片类型 + 大小 ≤ 2MB」，
     * 且按用户分目录 {@code supermall/avatars/{userId}/}，便于单独清理。
     */
    String uploadAvatar(MultipartFile file, Long userId);

    /**
     * 批量上传，返回 URL 列表（顺序与入参一致）。
     *
     * @param max 单次上限，超过抛业务异常
     */
    List<String> uploadBatch(MultipartFile[] files, int max);
}
