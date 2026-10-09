package com.supermall.takeout.dto;

import com.supermall.dto.CaptchaAware;
import com.supermall.validation.EmailAccount;
import com.supermall.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 商家入驻申请（自助提交，平台审核）。
 *
 * <p>提交后落 {@code merchant} 表：{@code auditStatus=0 待审核}、{@code status=0 未营业}。
 * 审核通过前不能登录商家端。
 */
@Data
@Schema(description = "商家入驻申请")
public class MerchantApplyDTO implements Serializable, CaptchaAware {

    @NotBlank(message = "邮箱不能为空")
    @EmailAccount
    @Schema(description = "登录邮箱（即账号）", example = "newshop@163.com")
    private String username;

    @NotBlank(message = "密码不能为空")
    @StrongPassword
    @Schema(description = "登录密码", example = "Mall1234")
    private String password;

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称最长 100 个字符")
    @Schema(description = "店铺名称", example = "川味小馆")
    private String shopName;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 50, message = "联系人最长 50 个字符")
    @Schema(description = "联系人姓名", example = "张三")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    @Schema(description = "联系电话", example = "13900000009")
    private String phone;

    @NotBlank(message = "店铺地址不能为空")
    @Size(max = 255, message = "店铺地址最长 255 个字符")
    @Schema(description = "店铺地址（取餐点）", example = "北京市朝阳区望京街 9 号")
    private String address;

    @Size(max = 500, message = "店铺简介最长 500 个字符")
    @Schema(description = "店铺简介")
    private String description;

    @Schema(description = "营业执照 / 门店照 URL（先调 /api/merchant/file/upload 上传）")
    private String licenseImage;

    @Schema(description = "图形验证码标识")
    private String captchaKey;

    @Schema(description = "图形验证码")
    private String captchaCode;
}
