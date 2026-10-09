package com.supermall.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 收货地址（电商 P3）。
 *
 * <p>省市区有两种传法，服务端二选一：
 * <ol>
 *   <li><b>传 {@code areaCode}（推荐）</b>：服务端按静态区划数据校验它是叶子节点，
 *       并回填 province / city / district 名称 —— 前端做三级联动只传末级 code 即可，
 *       不用担心名称写错。</li>
 *   <li><b>不传 areaCode，直接传 province / city / district</b>：兼容手工录入
 *       （以及外卖侧创建的老地址），三者必须齐全。</li>
 * </ol>
 */
@Data
@Schema(description = "收货地址")
public class MallAddressDTO implements Serializable {

    @Schema(description = "地址 id；新增时不用传，修改时必传")
    private Long id;

    @NotBlank(message = "收货人不能为空")
    @Schema(description = "收货人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String consignee;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "省市区末级编码（区县 code，如 110101）；传了它就不需要传下面三个")
    private String areaCode;

    @Schema(description = "省（传 areaCode 时可不传，由服务端回填）")
    private String province;

    @Schema(description = "市（传 areaCode 时可不传，由服务端回填）")
    private String city;

    @Schema(description = "区/县（传 areaCode 时可不传，由服务端回填）")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Schema(description = "详细地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String detail;

    @Schema(description = "标签：家 / 公司 / 学校")
    private String label;

    @Schema(description = "1 设为默认地址 0 否；第一条地址自动成为默认")
    private Integer isDefault;
}
