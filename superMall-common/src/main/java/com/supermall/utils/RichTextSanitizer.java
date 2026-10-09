package com.supermall.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.util.regex.Pattern;

/**
 * 富文本 / 纯文本的 XSS 清洗器（Jsoup Safelist）。
 *
 * <p>为什么需要：商品详情、评价内容、店铺简介这类字段是**用户/运营输入的 HTML**，
 * 直接入库就是存储型 XSS 的温床。本项目当前靠前端 `{{ }}` 插值转义（不会执行脚本），
 * 但只要哪天有人把展示改成 `v-html`，或者数据被同步到 App/小程序/Nginx 直出页，
 * 就会立刻变成真实漏洞 —— 所以在**入库前**统一清洗，比指望每一处渲染都小心可靠。
 *
 * <p>两个口径，别用混：
 * <ul>
 *   <li>{@link #richText(String)} —— 富文本（商品详情）。保留一小撮安全的排版标签，
 *       其余标签与属性丢弃；`script/style/iframe` 这类连内容一起丢。</li>
 *   <li>{@link #plainText(String)} —— 纯文本（评价内容/店铺简介/昵称/退款说明）。
 *       本身不该有标签：只有"看起来真的写了标签"时才清洗，避免把
 *       `全场满 100 &lt; 200 减 30` 这种正常文字里的 `&lt;` 变成实体。</li>
 * </ul>
 *
 * <p>都是**幂等**的：清洗过的文本再清洗一次结果一样（可以放心在 save/update 两处都调）。
 *
 * <p>⚠️ 只管"标签与协议白名单"，不负责长度校验：长度限制交给各自的 `@Size` / 业务校验。
 */
public final class RichTextSanitizer {

    private RichTextSanitizer() {
    }

    private static final Document.OutputSettings OUTPUT =
            new Document.OutputSettings().prettyPrint(false);

    /**
     * 富文本白名单：
     * 段落 / 换行 / 加粗 / 斜体 / 下划线 / 删除线 / 列表 / 标题 / 引用 / 分割线 / 代码，
     * 外加 `a[href]` 与 `img[src]`。
     * <p>刻意**不允许** class / style / id / target 等属性 —— 免得被拿来挂样式钓鱼或跳转劫持。
     *
     * <p>地址（href / src）不用 Jsoup 的"协议白名单"，而是用**危险协议黑名单**（见 {@link #BAD_SCHEME}）。
     * 原因：白名单会把**相对路径**一起拒掉，而本项目的图片惯例正是 `/seed/c-001.jpg` 这种相对地址；
     * 黑名单既挡住 `javascript:` / `data:`，又不会误伤站内相对资源。
     */
    private static final Safelist RICH = Safelist.relaxed()
            .removeTags("span", "table", "thead", "tbody", "tr", "td", "th", "col", "colgroup")
            .removeAttributes(":all", "class", "id", "style", "target", "rel")
            .removeProtocols("a", "href", "ftp", "http", "https", "mailto")
            .removeProtocols("img", "src", "http", "https")
            .addTags("hr", "code", "pre");

    /** 危险协议：命中就把该属性整个摘掉（先去掉空白/控制字符，防 `java&#9;script:` 这类绕过） */
    private static final Pattern BAD_SCHEME =
            Pattern.compile("^\\s*(javascript|vbscript|data|file|blob)\\s*:", Pattern.CASE_INSENSITIVE);

    /** 看起来像 HTML 标签（`<a href=...>` / `</p>` / `<br/>`）—— 正常文字里的 "a < b" 不会命中 */
    private static final String TAG_LIKE = "<\\s*/?\\s*[a-zA-Z][^>]*>";

    /**
     * 清洗富文本：保留白名单标签，其余丢弃（`script` / `style` 等连内容一起丢）。
     *
     * @return 清洗后的 HTML；入参为 null / 空白时原样返回（保持"没填"的语义）
     */
    public static String richText(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        // ① 标签 / 属性白名单（prettyPrint=false 保持输出紧凑）
        String cleaned = Jsoup.clean(html, "", RICH, OUTPUT);
        // ② 地址再走一遍危险协议黑名单
        Document doc = Jsoup.parseBodyFragment(cleaned);
        doc.outputSettings(OUTPUT);
        for (Element el : doc.select("[href]")) {
            dropBadScheme(el, "href");
        }
        for (Element el : doc.select("[src]")) {
            dropBadScheme(el, "src");
        }
        return doc.body().html();
    }

    /**
     * 清洗纯文本：本身不该有标签，所以"只有真的写了标签"才动手 ——
     * 否则 `5 &lt; 10` 这种正常文字会被 Jsoup 转义成 `5 &amp;lt; 10`（前端再转义一次就露出来了）。
     *
     * <p>`<b>好</b>用` → `好用`（标签剥掉、内容保留）；
     * 而 `<script>alert(1)</script>` → **空字符串**（script / style 连内容一起丢，不留 `alert(1)` 残渣）。
     */
    public static String plainText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        if (!text.matches("(?s).*" + TAG_LIKE + ".*")) {
            return text;     // 没有标签：一个字符都不动
        }
        return Jsoup.clean(text, "", Safelist.none(), OUTPUT).trim();
    }

    /** 地址以危险协议开头就摘掉这个属性（相对路径 / http(s) 都放行） */
    private static void dropBadScheme(Element el, String attr) {
        String value = el.attr(attr).replaceAll("[\\s\\u0000-\\u001f]", "");
        if (BAD_SCHEME.matcher(value).find()) {
            el.removeAttr(attr);
        }
    }
}
