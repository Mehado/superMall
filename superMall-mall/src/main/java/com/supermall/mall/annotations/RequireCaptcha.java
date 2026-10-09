package com.supermall.mall.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要校验图形验证码的方法，配合 MallCaptchaAspect 使用。
 *
 * <p>为什么不直接复用外卖的 {@code com.supermall.takeout.annotations.RequireCaptcha}：
 * 那个类在 {@code superMall-takeout} 模块里，电商依赖不到它。
 * 让电商依赖外卖的启动模块会把两个板块耦合成"一个挂了另一个也起不来"，
 * 所以这里是【有意复制】的 4 个小文件之一（另一个是验证码画图工具）。
 *
 * <p>被标注的方法必须有一个实现了 {@code com.supermall.dto.CaptchaAware} 的参数
 * （这个接口在 pojo 里，两边共享，所以电商标注外卖的 DTO 也能被切面识别）。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireCaptcha {
}
