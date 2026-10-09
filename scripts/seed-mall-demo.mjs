// scripts/seed-mall-demo.mjs
// 电商板块「演示种子数据」生成器：抓真图 → 写 SQL → 可选导入 MySQL。
//
// 图源支持两种（--source）：
//   pexels（默认，需要 PEXELS_API_KEY）—— 走 Pexels 官方 API，按关键词搜竖构图照片，
//        再用它 CDN 的 fit=crop 直接裁成目标比例；Pexels License 免费商用、无需署名。
//   loremflickr（备用）—— 免 key，但许可状态不一、关键词命中率差。
//
// 为什么不去爬京东/Pexels 网页：两家都对非浏览器请求返回 403/风控跳转。
// Pexels 提供了官方 API 就是给这种用途的，走它既稳定也合规。
//
// 用法（在仓库根目录跑）：
//   PEXELS_API_KEY=xxx node scripts/seed-mall-demo.mjs                 # 抓缺失的图 + 生成 SQL
//   PEXELS_API_KEY=xxx node scripts/seed-mall-demo.mjs --apply         # 再导入数据库
//   PEXELS_API_KEY=xxx node scripts/seed-mall-demo.mjs --apply --force # 图全部重抓
//   node scripts/seed-mall-demo.mjs --source loremflickr               # 换回备用图源
//   node scripts/seed-mall-demo.mjs --apply --img-prefix https://<bucket>.<endpoint>/supermall/seed/
//                                                                      # 写进 DB/SQL 的图片地址直接用 OSS 绝对 URL
//                                                                      # （等价于跑完 seeder 再跑 migrate-seed-images-to-oss.mjs）
//
// 幂等：图片已存在就跳过（--force 覆盖）；SQL 开头只删「种子 id 区间」，不碰真实数据。
// 搜索结果是带缓存的（.seed-cache/），重跑不会重复消耗 API 配额。

import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

/* ────────────────────────── 配置 ────────────────────────── */

const ROOT = path.resolve(import.meta.dirname, '..')
const OUT_IMG = path.join(ROOT, 'frontend/public/seed')
const OUT_SQL = path.join(ROOT, 'sql/seed-mall-demo.sql')
/**
 * 写进 DB / SQL 的图片地址前缀。
 *   · 本地开发：默认 `/seed/`（图放 frontend/public/seed/，跟着前端部署）
 *   · 迁移 / 上云：传 `--img-prefix https://<bucket>.<endpoint>/supermall/seed/`
 *     （图先在 OSS 上；也可以用 scripts/migrate-seed-images-to-oss.mjs 事后批量改）
 */
const IMG_PREFIX = (argOf('--img-prefix') || process.env.SEED_IMG_PREFIX || '/seed/').replace(/\/?$/, '/')
const CACHE_DIR = path.join(ROOT, '.seed-cache')

const argv = process.argv.slice(2)
const has = (f) => argv.includes(f)
const argOf = (f) => {
  const i = argv.indexOf(f)
  return i >= 0 ? argv[i + 1] : ''
}

const PEXELS_API_KEY = process.env.PEXELS_API_KEY || argOf('--key') || ''
const SOURCE = argOf('--source') || (PEXELS_API_KEY ? 'pexels' : 'loremflickr')
const APPLY = has('--apply')
const FORCE = has('--force')
const CONCURRENCY = Number(process.env.SEED_CONCURRENCY || 5)

const MYSQL = process.env.MYSQL_CLI || 'D:/mysql/mysql-9.5.0-winx64/bin/mysql'
const MYSQL_ARGS = [
  '--ssl-mode=DISABLED', '--default-character-set=utf8mb4',
  '-h', process.env.MYSQL_HOST || '127.0.0.1',
  '-P', process.env.MYSQL_PORT || '3306',
  '-u', process.env.MYSQL_USER || 'root',
  `-p${process.env.MYSQL_PASSWORD || 'Ming1234'}`,
  '-D', process.env.MYSQL_DATABASE || 'supermall'
]

/** id 区间：全部避开现有数据（现有 max：category 12 / brand 5 / product 3 / spec 4 / spec_value 8 / sku 9 / sku_spec 16 / banner 3） */
const ID = {
  category: [101, 199], brand: [101, 199], product: [101, 199], productImage: [1001, 1999],
  spec: [101, 399], specValue: [1001, 1999], sku: [101, 499], skuSpec: [1001, 1999], banner: [101, 199]
}

/* ────────────────────────── 分类 ────────────────────────── */

const CATEGORIES = [
  { id: 101, parent: 0, name: '家用电器', kw: 'home appliances' },
  { id: 102, parent: 101, name: '厨房电器', kw: 'kitchen appliances' },
  { id: 103, parent: 101, name: '清洁电器', kw: 'vacuum cleaner' },
  { id: 104, parent: 101, name: '生活电器', kw: 'home electronics' },

  { id: 105, parent: 0, name: '美妆个护', kw: 'cosmetics' },
  { id: 106, parent: 105, name: '护肤', kw: 'skincare products' },
  { id: 107, parent: 105, name: '彩妆', kw: 'makeup' },
  { id: 108, parent: 105, name: '洗护', kw: 'body care' },

  { id: 109, parent: 0, name: '家居生活', kw: 'home decor' },
  { id: 110, parent: 109, name: '家纺', kw: 'bedding' },
  { id: 111, parent: 109, name: '收纳', kw: 'storage basket' },
  { id: 112, parent: 109, name: '厨具', kw: 'cookware' },

  { id: 113, parent: 0, name: '运动户外', kw: 'outdoor sports' },
  { id: 114, parent: 113, name: '健身器材', kw: 'fitness equipment' },
  { id: 115, parent: 113, name: '户外装备', kw: 'camping gear' },

  { id: 116, parent: 0, name: '母婴玩具', kw: 'baby toys' },
  { id: 117, parent: 116, name: '喂养', kw: 'baby feeding' },
  { id: 118, parent: 116, name: '玩具', kw: 'toys' },

  { id: 120, parent: 10, name: '耳机音频', kw: 'headphones' },
  { id: 121, parent: 10, name: '智能穿戴', kw: 'smart watch' },
  { id: 122, parent: 10, name: '电脑外设', kw: 'computer keyboard' },
  { id: 123, parent: 1, name: '卫衣', kw: 'hoodie' },
  { id: 124, parent: 1, name: '牛仔裤', kw: 'jeans' },
  { id: 125, parent: 1, name: '箱包', kw: 'bags' },
  { id: 126, parent: 1, name: '配饰', kw: 'fashion accessories' },
  { id: 127, parent: 7, name: '零食', kw: 'snacks' },
  { id: 128, parent: 7, name: '茶叶', kw: 'tea leaves' }
]

/** 库里原有的 3 个一级分类 icon 是 NULL，给它们补封面 */
const LEGACY_CATEGORY_ICONS = [
  { id: 1, kw: 'fashion clothing', img: 'c-001.jpg' },
  { id: 7, kw: 'groceries', img: 'c-007.jpg' },
  { id: 10, kw: 'gadgets', img: 'c-010.jpg' }
]

/* ────────────────────────── 品牌 ────────────────────────── */

const BRANDS = [
  { id: 101, name: '素野', desc: '基础款服饰，主打面料与版型' },
  { id: 102, name: '云岚', desc: '轻户外与通勤箱包' },
  { id: 103, name: '沐白', desc: '家居布艺与床品' },
  { id: 104, name: '初禾', desc: '温和配方的个护产品' },
  { id: 105, name: '拾光', desc: '精品咖啡与茶饮' },
  { id: 106, name: '北野', desc: '户外露营装备' },
  { id: 107, name: '简厨', desc: '厨房小家电与厨具' },
  { id: 108, name: '星芒', desc: '数码配件与智能穿戴' },
  { id: 109, name: '轻语', desc: '家居收纳方案' },
  { id: 110, name: '悦己', desc: '彩妆与护肤' },
  { id: 111, name: '果芽', desc: '母婴喂养与玩具' },
  { id: 112, name: '风行者', desc: '跑步与健身装备' },
  { id: 113, name: '青谷', desc: '健康零食与冲饮' },
  { id: 114, name: '白露', desc: '香氛与生活电器' },
  { id: 115, name: '极地线', desc: '专业户外服饰' }
]

/* ────────────────────────── 商品 ────────────────────────── */
/* kw 是给图源搜图用的英文关键词，尽量是「主体 + 品类」两词，命中率更高 */
const PRODUCTS = [
  // 服饰鞋包
  { cat: 4, brand: 101, name: '260g 重磅纯棉T恤', sub: '厚实不透，不易变形', kw: 'plain t shirt', low: 79, high: 89, specs: ['颜色', ['白', '黑', '灰']] },
  { cat: 5, brand: 101, name: '法兰绒磨毛衬衫', sub: '秋冬内搭外穿都合适', kw: 'flannel shirt', low: 159, high: 179, specs: ['尺码', ['M', 'L', 'XL']] },
  { cat: 6, brand: 112, name: '轻量回弹跑鞋', sub: '中底回弹，久站不累', kw: 'running shoes', low: 299, high: 339, specs: ['尺码', ['39', '41', '43']] },
  { cat: 123, brand: 101, name: '抓绒连帽卫衣', sub: '内里抓绒，柔软保暖', kw: 'hoodie', low: 189, high: 209, specs: ['尺码', ['M', 'L', 'XL']] },
  { cat: 124, brand: 101, name: '直筒水洗牛仔裤', sub: '微弹面料，不勒腿', kw: 'denim jeans', low: 219, high: 239, specs: ['尺码', ['28', '30', '32']] },
  { cat: 125, brand: 102, name: '通勤双肩背包', sub: '15.6 寸电脑隔层', kw: 'backpack', low: 199, high: 229, specs: ['颜色', ['黑', '藏青']] },
  { cat: 125, brand: 102, name: '帆布托特包', sub: '加厚帆布，能装耐用', kw: 'canvas tote bag', low: 89, high: 99, specs: ['颜色', ['米白', '墨绿']] },
  { cat: 126, brand: 101, name: '纯羊毛针织围巾', sub: '亲肤不扎，保暖好搭', kw: 'wool scarf', low: 129, high: 149, specs: ['颜色', ['驼色', '深灰']] },
  { cat: 126, brand: 101, name: '头层牛皮腰带', sub: '自动扣，宽窄两用', kw: 'leather belt', low: 99, high: 119, specs: ['尺码', ['105cm', '115cm']] },
  { cat: 126, brand: 101, name: '纯棉棒球帽', sub: '可调节，男女同款', kw: 'baseball cap', low: 59, high: 69, specs: ['颜色', ['黑', '米白']] },
  { cat: 123, brand: 115, name: '三合一冲锋衣', sub: '防风防泼水，可拆内胆', kw: 'outdoor jacket', low: 499, high: 559, specs: ['尺码', ['M', 'L', 'XL']] },
  { cat: 123, brand: 101, name: '宽松针织开衫', sub: '慵懒版型，空调房必备', kw: 'knit cardigan', low: 169, high: 189, specs: ['颜色', ['燕麦', '雾蓝']] },

  // 数码配件
  { cat: 120, brand: 108, name: '主动降噪蓝牙耳机', sub: '通勤降噪，单次 8 小时', kw: 'wireless earbuds', low: 399, high: 449, specs: ['颜色', ['白', '黑']] },
  { cat: 120, brand: 108, name: '桌面蓝牙音箱', sub: '木质腔体，人声通透', kw: 'bluetooth speaker', low: 269, high: 299, specs: ['颜色', ['木色', '黑']] },
  { cat: 121, brand: 108, name: '全天候智能手表', sub: '心率血氧，14 天续航', kw: 'smart watch', low: 599, high: 699, specs: ['表带', ['硅胶', '编织']] },
  { cat: 122, brand: 108, name: '87 键机械键盘', sub: '热插拔轴体，紧凑布局', kw: 'mechanical keyboard', low: 329, high: 369, specs: ['轴体', ['红轴', '茶轴']] },
  { cat: 122, brand: 108, name: '无线静音鼠标', sub: '双模连接，静音微动', kw: 'computer mouse', low: 89, high: 99, specs: ['颜色', ['灰', '白']] },
  { cat: 10, brand: 108, name: '20000mAh 移动电源', sub: '双向快充，可上飞机', kw: 'power bank', low: 149, high: 169, specs: ['颜色', ['黑', '白']] },
  { cat: 11, brand: 108, name: '65W 氮化镓充电器', sub: '一个头喂饱手机电脑', kw: 'usb charger', low: 129, high: 149, specs: ['接口', ['双 C', 'C+A']] },
  { cat: 10, brand: 108, name: '磁吸防摔手机壳', sub: '镜头全包，手感细腻', kw: 'phone case', low: 49, high: 59, specs: ['颜色', ['透明', '黑']] },

  // 家电
  { cat: 102, brand: 107, name: 'IH 电饭煲 4L', sub: '厚釜内胆，米饭更饱满', kw: 'rice cooker', low: 399, high: 459, specs: ['容量', ['3L', '4L']] },
  { cat: 102, brand: 107, name: '可视空气炸锅 5L', sub: '少油少烟，一锅多用', kw: 'air fryer', low: 329, high: 379, specs: ['容量', ['4L', '5L']] },
  { cat: 102, brand: 107, name: '破壁料理机', sub: '八叶刀头，细腻无渣', kw: 'kitchen blender', low: 459, high: 529, specs: ['容量', ['1.2L', '1.75L']] },
  { cat: 102, brand: 107, name: '恒温电热水壶', sub: '六段控温，烧水更快', kw: 'electric kettle', low: 179, high: 199, specs: ['容量', ['1.5L', '1.7L']] },
  { cat: 102, brand: 107, name: '半自动意式咖啡机', sub: '20Bar 萃取，家用足够', kw: 'espresso machine', low: 899, high: 999, specs: ['颜色', ['银', '黑']] },
  { cat: 103, brand: 107, name: '激光导航扫地机器人', sub: '边扫边拖，自动回充', kw: 'robot vacuum', low: 1299, high: 1499, specs: ['版本', ['标准版', '自集尘']] },
  { cat: 103, brand: 107, name: '轻量无线吸尘器', sub: '整机 1.4kg，女生也能举', kw: 'vacuum cleaner', low: 799, high: 899, specs: ['版本', ['标准', '除螨套装']] },
  { cat: 104, brand: 114, name: '上加水加湿器', sub: '大雾量，静音夜用', kw: 'humidifier', low: 199, high: 229, specs: ['容量', ['4L', '6L']] },
  { cat: 104, brand: 114, name: '落地循环扇', sub: '直流电机，风感柔和', kw: 'electric fan', low: 299, high: 339, specs: ['档位', ['8 档', '12 档']] },

  // 美妆个护
  { cat: 106, brand: 110, name: '神经酰胺保湿面霜', sub: '修护屏障，不闷痘', kw: 'face cream', low: 159, high: 179, specs: ['规格', ['50ml', '80ml']] },
  { cat: 106, brand: 110, name: '烟酰胺精华液', sub: '提亮肤色，温和不刺激', kw: 'serum bottle', low: 199, high: 219, specs: ['规格', ['30ml']] },
  { cat: 106, brand: 104, name: '多肽紧致眼霜', sub: '轻薄好推，不长脂肪粒', kw: 'eye cream', low: 179, high: 199, specs: ['规格', ['15g']] },
  { cat: 106, brand: 104, name: '氨基酸洁面乳', sub: '洗后不紧绷', kw: 'face wash', low: 69, high: 79, specs: ['规格', ['120g']] },
  { cat: 107, brand: 110, name: '丝绒雾面口红', sub: '显白不拔干', kw: 'lipstick', low: 129, high: 139, specs: ['色号', ['枫叶红', '豆沙', '蜜桃']] },
  { cat: 107, brand: 110, name: '持妆粉底液', sub: '轻薄服帖，控油持妆', kw: 'foundation makeup', low: 189, high: 209, specs: ['色号', ['自然色', '象牙白']] },
  { cat: 107, brand: 110, name: '大地色眼影盘', sub: '哑光珠光一盘搞定', kw: 'eyeshadow palette', low: 139, high: 149, specs: ['色号', ['大地色', '玫瑰色']] },
  { cat: 108, brand: 104, name: '无硅油洗发水', sub: '控油蓬松，温和清洁', kw: 'shampoo bottle', low: 79, high: 89, specs: ['规格', ['500ml', '750ml']] },
  { cat: 108, brand: 104, name: '氨基酸沐浴露', sub: '泡沫绵密，洗完不假滑', kw: 'body wash', low: 69, high: 79, specs: ['香型', ['白茶', '雪松']] },
  { cat: 108, brand: 104, name: '护龈牙膏三支装', sub: '清新口气，呵护牙龈', kw: 'toothpaste', low: 49, high: 55, specs: ['规格', ['3 支装']] },

  // 家居生活
  { cat: 110, brand: 103, name: '60 支长绒棉四件套', sub: '亲肤透气，越洗越软', kw: 'bed sheet', low: 399, high: 469, specs: ['尺寸', ['1.5m 床', '1.8m 床']] },
  { cat: 110, brand: 103, name: '慢回弹记忆枕', sub: '护颈曲度，翻身不塌', kw: 'pillow', low: 169, high: 189, specs: ['高度', ['低枕', '高枕']] },
  { cat: 110, brand: 103, name: '加厚羊毛毯', sub: '午睡盖毯，柔软不扎', kw: 'wool blanket', low: 199, high: 229, specs: ['尺寸', ['150×200', '200×230']] },
  { cat: 110, brand: 103, name: '纯棉强吸水毛巾', sub: 'A 类标准，越洗越软', kw: 'cotton towel', low: 39, high: 45, specs: ['颜色', ['灰', '米白']] },
  { cat: 111, brand: 109, name: '可折叠收纳箱', sub: '带轮可叠，换季好帮手', kw: 'storage box', low: 89, high: 109, specs: ['容量', ['45L', '66L']] },
  { cat: 111, brand: 109, name: '防滑植绒衣架 20 只', sub: '省空间，不滑落', kw: 'clothes hanger', low: 45, high: 55, specs: ['规格', ['20 只']] },
  { cat: 112, brand: 107, name: '麦饭石不粘炒锅', sub: '少油不粘，燃气电磁通用', kw: 'frying pan', low: 229, high: 259, specs: ['口径', ['28cm', '30cm']] },
  { cat: 112, brand: 107, name: '德国钢厨房刀具五件套', sub: '锋利耐用，带刀座', kw: 'kitchen knife', low: 269, high: 299, specs: ['套装', ['五件套']] },
  { cat: 112, brand: 107, name: '316 不锈钢保温杯', sub: '12 小时保温', kw: 'thermos bottle', low: 129, high: 149, specs: ['容量', ['400ml', '500ml']] },

  // 运动户外
  { cat: 114, brand: 112, name: 'TPE 双面防滑瑜伽垫', sub: '加厚 8mm，回弹好', kw: 'yoga mat', low: 129, high: 149, specs: ['颜色', ['紫', '灰蓝']] },
  { cat: 114, brand: 112, name: '可调节包胶哑铃', sub: '一对到手，重量可加', kw: 'dumbbell', low: 199, high: 239, specs: ['重量', ['10kg', '20kg']] },
  { cat: 114, brand: 112, name: '乳胶弹力带套装', sub: '五个阻力级别', kw: 'resistance band', low: 59, high: 69, specs: ['套装', ['五件套']] },
  { cat: 114, brand: 112, name: '钢丝竞速跳绳', sub: '轴承顺滑，可调长度', kw: 'jump rope', low: 39, high: 45, specs: ['手柄', ['泡沫', '合金']] },
  { cat: 115, brand: 106, name: '自动速开帐篷 3-4 人', sub: '一秒开合，防泼水', kw: 'camping tent', low: 399, high: 459, specs: ['规格', ['3 人', '4 人']] },
  { cat: 115, brand: 106, name: '轻量登山包 40L', sub: '背负透气，可挂登山杖', kw: 'hiking backpack', low: 329, high: 369, specs: ['容量', ['35L', '40L']] },
  { cat: 115, brand: 106, name: '信封式睡袋', sub: '可拼接，四季可用', kw: 'sleeping bag', low: 199, high: 229, specs: ['温标', ['5℃', '0℃']] },

  // 母婴玩具
  { cat: 117, brand: 111, name: '宽口径玻璃奶瓶', sub: '耐高温，防胀气', kw: 'baby bottle', low: 89, high: 99, specs: ['容量', ['160ml', '240ml']] },
  { cat: 117, brand: 111, name: '蒸煮搅拌辅食机', sub: '一键做辅食', kw: 'baby food maker', low: 299, high: 329, specs: ['容量', ['300ml']] },
  { cat: 118, brand: 111, name: '大颗粒益智积木', sub: '圆角打磨，安全无异味', kw: 'building blocks', low: 129, high: 149, specs: ['规格', ['80 粒', '160 粒']] },
  { cat: 118, brand: 111, name: '安抚毛绒玩具', sub: '可机洗，柔软亲肤', kw: 'stuffed animal', low: 79, high: 89, specs: ['款式', ['小熊', '兔子']] },
  { cat: 118, brand: 111, name: '儿童进阶拼图', sub: '分阶难度，锻炼专注', kw: 'jigsaw puzzle', low: 59, high: 69, specs: ['阶段', ['2 阶', '3 阶']] },

  // 食品饮料
  { cat: 9, brand: 105, name: '挂耳咖啡 10 片装', sub: '现磨锁鲜，办公室友好', kw: 'drip coffee', low: 59, high: 69, specs: ['烘焙度', ['中烘', '深烘']] },
  { cat: 8, brand: 105, name: '冷萃咖啡液 12 条', sub: '0 糖 0 脂，加奶即饮', kw: 'iced coffee', low: 79, high: 89, specs: ['口味', ['原味', '榛果']] },
  { cat: 128, brand: 113, name: '明前龙井茶罐装', sub: '豆香明显，回甘持久', kw: 'green tea', low: 128, high: 168, specs: ['规格', ['50g', '100g']] },
  { cat: 127, brand: 113, name: '每日坚果混合装', sub: '7 种果仁，独立小包', kw: 'mixed nuts', low: 69, high: 79, specs: ['规格', ['15 包', '30 包']] },
  { cat: 127, brand: 113, name: '低糖全麦饼干', sub: '粗粮饱腹，扛饿不腻', kw: 'cookies', low: 39, high: 45, specs: ['规格', ['400g']] },
  { cat: 8, brand: 105, name: '冻干柠檬蜂蜜茶', sub: '冷热皆宜，果香清爽', kw: 'lemon tea', low: 49, high: 55, specs: ['规格', ['10 条', '20 条']] }
]

/* ────────────────────────── 推荐位 ────────────────────────── */
const BANNERS = [
  { id: 101, position: 1, title: '秋冬新品 · 全场包邮', kw: 'autumn fashion', linkType: 2, linkValue: 1 },
  { id: 102, position: 1, title: '厨房小家电 · 到手价直降', kw: 'kitchen appliances', linkType: 2, linkValue: 102 },
  { id: 103, position: 1, title: '露营季 · 装备一站备齐', kw: 'camping', linkType: 2, linkValue: 115 },
  { id: 104, position: 1, title: '护肤专场 · 温和不刺激', kw: 'skincare products', linkType: 2, linkValue: 106 },
  { id: 105, position: 2, title: '服饰鞋包', kw: 'fashion clothing', linkType: 2, linkValue: 1 },
  { id: 106, position: 2, title: '家用电器', kw: 'home appliances', linkType: 2, linkValue: 101 },
  { id: 107, position: 2, title: '数码配件', kw: 'phone accessories', linkType: 2, linkValue: 10 },
  { id: 108, position: 2, title: '美妆个护', kw: 'beauty products', linkType: 2, linkValue: 105 },
  { id: 109, position: 2, title: '家居生活', kw: 'home living', linkType: 2, linkValue: 109 },
  { id: 110, position: 2, title: '运动户外', kw: 'sports equipment', linkType: 2, linkValue: 113 },
  { id: 111, position: 2, title: '食品饮料', kw: 'food drinks', linkType: 2, linkValue: 7 },
  { id: 112, position: 2, title: '母婴玩具', kw: 'baby toys', linkType: 2, linkValue: 116 }
]

/* ────────────────────────── 图片任务 ────────────────────────── */

const imgName = {
  product: (i, n) => `p-${String(i).padStart(3, '0')}-${n}.jpg`,
  category: (i) => `c-${String(i).padStart(3, '0')}.jpg`,
  banner: (i) => `b-${i}.jpg`,
  shortcut: (i) => `s-${String(i).padStart(2, '0')}.jpg`
}

/** 统一产出 {out, kw, orientation, w, h}；具体 URL 由图源决定 */
function imageJobs() {
  const jobs = []
  PRODUCTS.forEach((p, idx) => {
    const pid = 101 + idx
    for (const n of [1, 2]) {
      jobs.push({ out: imgName.product(pid, n), kw: p.kw, orientation: 'portrait', w: 480, h: 600 })
    }
  })
  CATEGORIES.forEach((c) => {
    jobs.push({ out: imgName.category(c.id), kw: c.kw, orientation: 'portrait', w: 480, h: 600 })
  })
  LEGACY_CATEGORY_ICONS.forEach((c) => {
    jobs.push({ out: c.img, kw: c.kw, orientation: 'portrait', w: 480, h: 600 })
  })
  const squares = BANNERS.filter((b) => b.position === 2)
  BANNERS.filter((b) => b.position === 1).forEach((b) => {
    jobs.push({ out: imgName.banner(b.id), kw: b.kw, orientation: 'landscape', w: 1440, h: 576 })
  })
  squares.forEach((b, i) => {
    jobs.push({ out: imgName.shortcut(i + 1), kw: b.kw, orientation: 'square', w: 300, h: 300 })
  })
  return jobs
}

/* ────────────────────────── Pexels 图源 ────────────────────────── */

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

function cacheFiles(kind, kw) {
  const safe = Buffer.from(kw).toString('base64url').slice(0, 60)
  // v3：只存 .jpeg 的 large2x 地址。
  // v2：存的是 src.original，可能混了 PNG —— 读缓存时统一再过滤一次 .jpeg，所以 v2 也能直接用。
  // 两个版本都查，命中就不再消耗 API 配额。
  return [
    path.join(CACHE_DIR, `v3-${kind}-${safe}.json`),
    path.join(CACHE_DIR, `v2-${kind}-${safe}.json`)
  ]
}

const isJpegUrl = (u) => /\.jpe?g$/i.test(u)

async function pexelsSearchIds(kw, orientation) {
  for (const cf of cacheFiles(orientation, kw)) {
    if (!fs.existsSync(cf)) continue
    try {
      const list = JSON.parse(fs.readFileSync(cf, 'utf8'))
      const jpegs = (list || []).filter(isJpegUrl)
      if (jpegs.length) return jpegs
    } catch { /* 缓存坏了就下一个 */ }
  }
  const url =
    `https://api.pexels.com/v1/search?query=${encodeURIComponent(kw)}` +
    `&orientation=${orientation}&per_page=20&size=medium`
  for (let attempt = 1; attempt <= 2; attempt++) {
    const res = await fetch(url, { headers: { Authorization: PEXELS_API_KEY } })
    if (res.status === 429) {
      // 每小时 200 次的额度用完了，退到下一个关键词（下次跑会自动补上）
      throw new Error('Pexels 每小时额度已用完（200/h）')
    }
    if (!res.ok) throw new Error(`Pexels 搜索失败 HTTP ${res.status} (${kw})`)
    const j = await res.json()
    const bases = (j.photos || [])
      .map((p) => String(p.src.large2x || p.src.large || p.src.original || '').split('?')[0])
      .filter(isJpegUrl)
    fs.mkdirSync(CACHE_DIR, { recursive: true })
    fs.writeFileSync(cacheFiles(orientation, kw)[0], JSON.stringify(bases))
    return bases
  }
  throw new Error(`Pexels 搜索失败：${kw}`)
}

/** Pexels CDN 支持服务端裁切，直接在真实原图地址上要目标尺寸，不用本地 ffmpeg */
const pexelsUrl = (base, w, h) =>
  `${base}?auto=compress&cs=tinysrgb&fit=crop&w=${w}&h=${h}`

/* ────────────────────────── 备用图源（免 key） ────────────────────────── */

const loremUrl = (job, lock) => `https://loremflickr.com/${job.w}/${job.h}/${encodeURIComponent(job.kw)}?lock=${lock}`

/* ────────────────────────── 下载 ────────────────────────── */

async function download(url, dest) {
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const res = await fetch(url, { redirect: 'follow', signal: AbortSignal.timeout(30000) })
      if (!res.ok) throw new Error('HTTP ' + res.status)
      const buf = Buffer.from(await res.arrayBuffer())
      if (buf.length < 2000 || buf[0] !== 0xff || buf[1] !== 0xd8) throw new Error('不是有效 JPEG')
      fs.writeFileSync(dest, buf)
      return
    } catch (e) {
      if (attempt === 3) throw e
      await sleep(600 * attempt)
    }
  }
}

async function fetchImages() {
  const jobs = imageJobs()
  fs.mkdirSync(OUT_IMG, { recursive: true })

  // 1) 先按关键词把候选图 id 准备好（Pexels 走 API + 缓存；loremflickr 不需要）
  const plan = new Map() // out -> url
  if (SOURCE === 'pexels') {
    if (!PEXELS_API_KEY) throw new Error('缺少 PEXELS_API_KEY：PEXELS_API_KEY=xxx node scripts/seed-mall-demo.mjs')
    const keywords = [...new Set(jobs.map((j) => `${j.orientation}|${j.kw}`))]
    console.log(`Pexels 图源：${jobs.length} 张图 / ${keywords.length} 个关键词（搜索结果有缓存，重跑不再请求）`)
    const pools = new Map()
    let n = 0
    for (const k of keywords) {
      const [orientation, kw] = k.split('|')
      try {
        pools.set(k, await pexelsSearchIds(kw, orientation))
      } catch (e) {
        console.log(`  [!] ${kw}: ${e.message}`)
        pools.set(k, [])
      }
      if (++n % 10 === 0) process.stdout.write(`\r  查关键词 ${n}/${keywords.length}  `)
    }
    process.stdout.write('\n')
    const used = new Map()
    for (const j of jobs) {
      const key = `${j.orientation}|${j.kw}`
      const pool = pools.get(key) || []
      if (!pool.length) continue
      const i = used.get(key) || 0
      used.set(key, i + 1)
      plan.set(j.out, pexelsUrl(pool[i % pool.length], j.w, j.h))
    }
  } else {
    console.log(`LoremFlickr 图源：${jobs.length} 张图`)
    let lock = 11
    for (const j of jobs) plan.set(j.out, loremUrl(j, lock++))
  }

  // 2) 下载
  let done = 0, ok = 0, skip = 0
  const failed = []
  const queue = jobs.filter((j) => plan.has(j.out))
  const totalQ = queue.length
  const missing = jobs.length - totalQ
  async function worker() {
    while (queue.length) {
      const job = queue.shift()
      const dest = path.join(OUT_IMG, job.out)
      if (!FORCE && fs.existsSync(dest) && fs.statSync(dest).size > 2000) {
        skip++; done++
      } else {
        try {
          await download(plan.get(job.out), dest)
          ok++
        } catch (e) {
          failed.push(`${job.out} (${job.kw}) ${e.message}`)
        }
        done++
      }
      if (done % 10 === 0 || done === totalQ) {
        process.stdout.write(`\r  ${done}/${totalQ}  新抓 ${ok} 跳过 ${skip} 失败 ${failed.length}  `)
      }
    }
  }
  await Promise.all(Array.from({ length: CONCURRENCY }, worker))
  process.stdout.write('\n')
  if (missing) console.log(`  注意：${missing} 张因为关键词搜不到结果被跳过`)
  if (failed.length) {
    console.log('失败清单（重跑本脚本可补齐）：')
    failed.forEach((f) => console.log('  ' + f))
  }
  return failed
}

/* ────────────────────────── 生成 SQL ────────────────────────── */

const q = (v) => (v === null || v === undefined ? 'NULL' : `'${String(v).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`)
const NOW = '2026-09-20 10:00:00'

function buildSql() {
  const L = []
  const push = (...s) => L.push(...s)

  push('-- 自动生成，请勿手改：node scripts/seed-mall-demo.mjs')
  push('-- 电商演示种子数据：分类 / 品牌 / 商品 / 规格 / SKU / 推荐位')
  push(`-- 图片来自 frontend/public/seed/（图源：${SOURCE}，仅本地演示用）`)
  push('')
  push('SET NAMES utf8mb4;')
  push('')
  push('-- 清理上一次的种子数据（只动种子 id 区间，不影响真实数据）')
  const rangeOf = {
    mall_sku_spec: 'skuSpec', mall_sku: 'sku', mall_spec_value: 'specValue', mall_spec: 'spec',
    mall_product_image: 'productImage', mall_product: 'product', mall_banner: 'banner',
    mall_brand: 'brand', mall_category: 'category'
  }
  for (const t of Object.keys(rangeOf)) {
    const [a, b] = ID[rangeOf[t]]
    push(`DELETE FROM ${t} WHERE id BETWEEN ${a} AND ${b};`)
  }
  push('')

  push('-- 分类')
  for (const c of CATEGORIES) {
    const level = c.parent === 0 ? 1 : (CATEGORIES.find((x) => x.id === c.parent)?.parent === 0 ? 2 : 3)
    push(
      `INSERT INTO mall_category (id,parent_id,level,name,icon,sort,status,create_time,update_time) VALUES ` +
        `(${c.id},${c.parent},${level},${q(c.name)},${q(`${IMG_PREFIX}${imgName.category(c.id)}`)},${c.id % 100},1,${q(NOW)},${q(NOW)});`
    )
  }
  push('')
  push('-- 已有分类补封面')
  for (const c of LEGACY_CATEGORY_ICONS) {
    push(`UPDATE mall_category SET icon=${q(`${IMG_PREFIX}${c.img}`)} WHERE id=${c.id};`)
  }
  push('')

  push('-- 品牌')
  for (const b of BRANDS) {
    push(
      `INSERT INTO mall_brand (id,name,logo,description,sort,status,create_time,update_time) VALUES ` +
        `(${b.id},${q(b.name)},NULL,${q(b.desc)},${b.id % 100},1,${q(NOW)},${q(NOW)});`
    )
  }
  push('')

  push('-- 商品 / 规格 / SKU')
  let specId = ID.spec[0], specValueId = ID.specValue[0], skuId = ID.sku[0], skuSpecId = ID.skuSpec[0]
  let imageId = ID.productImage[0]
  const imageRows = []

  PRODUCTS.forEach((p, idx) => {
    const pid = 101 + idx
    const values = p.specs[1]
    const mainImage = `${IMG_PREFIX}${imgName.product(pid, 1)}`
    const span = Math.max(0, p.high - p.low)
    const unit = values.length > 1 ? Math.round(span / (values.length - 1)) : 0
    const stock = 60 + ((pid * 37) % 240)
    const sales = (pid * 53) % 900
    const detail =
      `${p.name}。${p.sub}。\n\n` +
      `· 规格：${p.specs[0]} 共 ${values.length} 种可选\n` +
      `· 发货：下单后 48 小时内发出，支持 7 天无理由退货\n` +
      `· 售后：退款进度可在「我的订单」里随时查看`

    push(
      `INSERT INTO mall_product (id,category_id,brand_id,name,subtitle,main_image,detail,price_low,price_high,stock,sales,rating,status,sort,create_time,update_time,review_count,good_rate) VALUES ` +
        `(${pid},${p.cat},${p.brand},${q(p.name)},${q(p.sub)},${q(mainImage)},${q(detail)},${p.low.toFixed(2)},${p.high.toFixed(2)},${stock},${sales},5.00,1,${idx + 1},${q(NOW)},${q(NOW)},0,100.00);`
    )

    for (const n of [1, 2]) {
      imageRows.push(
        `INSERT INTO mall_product_image (id,product_id,url,sort,create_time) VALUES ` +
          `(${imageId++},${pid},${q(`${IMG_PREFIX}${imgName.product(pid, n)}`)},${n},${q(NOW)});`
      )
    }

    const thisSpecId = specId++
    push(`INSERT INTO mall_spec (id,product_id,name,sort,create_time) VALUES (${thisSpecId},${pid},${q(p.specs[0])},1,${q(NOW)});`)
    const valueIds = values.map(() => specValueId++)
    values.forEach((v, i) => {
      push(`INSERT INTO mall_spec_value (id,spec_id,value,sort,create_time) VALUES (${valueIds[i]},${thisSpecId},${q(v)},${i + 1},${q(NOW)});`)
    })
    values.forEach((v, i) => {
      const thisSkuId = skuId++
      push(
        `INSERT INTO mall_sku (id,product_id,sku_code,price,stock,sales,image,status,create_time,update_time) VALUES ` +
          `(${thisSkuId},${pid},${q(`${pid}-${i + 1}`)},${(p.low + unit * i).toFixed(2)},${Math.round(stock / values.length)},${Math.round(sales / values.length)},NULL,1,${q(NOW)},${q(NOW)});`
      )
      push(`INSERT INTO mall_sku_spec (id,sku_id,spec_id,spec_value_id) VALUES (${skuSpecId++},${thisSkuId},${thisSpecId},${valueIds[i]});`)
    })
  })
  push('')
  push('-- 商品图集')
  push(...imageRows)
  push('')

  push('-- 推荐位（position 1 轮播 / 2 金刚区，全部上架）')
  const squares = BANNERS.filter((b) => b.position === 2)
  for (const b of BANNERS) {
    const img =
      b.position === 1
        ? `${IMG_PREFIX}${imgName.banner(b.id)}`
        : `${IMG_PREFIX}${imgName.shortcut(squares.findIndex((x) => x.id === b.id) + 1)}`
    push(
      `INSERT INTO mall_banner (id,title,image,position,link_type,link_value,sort,status,start_time,end_time,create_time,update_time) VALUES ` +
        `(${b.id},${q(b.title)},${q(img)},${b.position},${b.linkType},${q(b.linkValue)},${b.id % 100},1,NULL,NULL,${q(NOW)},${q(NOW)});`
    )
  }
  push('')
  return L.join('\n') + '\n'
}

/* ────────────────────────── 主流程 ────────────────────────── */

const failed = await fetchImages()
const sql = buildSql()
fs.mkdirSync(path.dirname(OUT_SQL), { recursive: true })
fs.writeFileSync(OUT_SQL, sql)
console.log(`SQL 已写出：${path.relative(ROOT, OUT_SQL)}（${(sql.length / 1024).toFixed(0)}KB，${sql.split('\n').length} 行）`)

if (APPLY) {
  console.log('导入 MySQL...')
  try {
    execFileSync(MYSQL, MYSQL_ARGS, { input: sql, stdio: ['pipe', 'inherit', 'inherit'] })
    console.log('导入完成 ✓')
  } catch (e) {
    console.error('导入失败：', e.message)
    process.exit(1)
  }
}
if (failed.length) process.exitCode = 2
