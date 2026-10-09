package com.supermall.utils;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * 图片上传校验（类型白名单 + 大小上限）—— 两个板块共用。
 *
 * <p>为什么要有它：头像那两条链路（外卖 {@code UserFileServiceImpl} / 电商 {@code MallFileServiceImpl#uploadAvatar}）
 * 一开始就做了校验，但**商品图**上传忘了做 —— 实测可以把带 `onload=alert(1)` 的 `.svg`
 * 传上 OSS 并拿到公网 URL（存储型 XSS 的经典入口）。这里把规则收敛成一份。
 *
 * <p>取向：**返回错误信息而不是抛异常** —— 两个板块的业务异常类型不同
 * （外卖 {@code ProductBusinessException} / 电商 {@code MallBusinessException}），
 * 让调用方用自己的异常抛，全局处理器照旧转成 {@code Result.error(msg)}。
 *
 * <p>校验三件事（顺序刻意从便宜到贵）：
 * <ol>
 *   <li>空文件；</li>
 *   <li>`Content-Type` 必须是 `image/*`（挡住把 .html/.svg 当图片传）；</li>
 *   <li>扩展名在白名单内（挡住 .svg / .html / .jsp：OSS 上看是图片、浏览器里可能是页面）；</li>
 *   <li>大小不超过上限。</li>
 * </ol>
 *
 * <p>⚠️ `Content-Type` 是客户端声明的，不能单独作为依据；扩展名白名单是第二道闸。
 * 真要"内容级"校验（读魔数）得引入 imageio/tika，本项目按演示范围不做。
 */
public final class ImageFileValidator {

    /** 商品图上限（与 multipart 的 max-file-size 对齐） */
    public static final long MAX_PRODUCT_IMAGE_BYTES = 5L * 1024 * 1024;
    /** 头像上限 */
    public static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;

    /** 允许的图片扩展名（不带点、小写） */
    public static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private ImageFileValidator() {
    }

    /**
     * 校验一张图片。
     *
     * @param file     上传的文件
     * @param maxBytes 大小上限（字节）
     * @return {@code null} 表示通过；否则返回**可直接展示给用户**的错误信息
     */
    public static String check(MultipartFile file, long maxBytes) {
        if (file == null || file.isEmpty()) {
            return "上传的文件为空";
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return "只支持图片文件（jpg / jpeg / png / gif / webp / bmp）";
        }
        String ext = extName(file);
        if (!ALLOWED_EXT.contains(ext)) {
            return "图片格式不支持（只允许 jpg / jpeg / png / gif / webp / bmp）";
        }
        if (file.getSize() > maxBytes) {
            return "图片不能超过 " + (maxBytes / 1024 / 1024) + "MB";
        }
        return null;
    }

    /** 扩展名（不带点、小写）；取不到就返回空串（会被白名单挡掉） */
    public static String extName(MultipartFile file) {
        String original = file == null ? null : file.getOriginalFilename();
        if (original != null) {
            int i = original.lastIndexOf('.');
            if (i > -1 && i < original.length() - 1) {
                return original.substring(i + 1).toLowerCase(Locale.ROOT);
            }
        }
        return "";
    }
}
