#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
把「原图」批量处理成首页滚动图片墙用的规范图（JPEG + 统一比例）。

为什么要它：原始素材格式/尺寸五花八门（实测遇到过 AVIF/WebP 却写成 .png、
500×500 到 5131×3421 混排、单张 3MB），直接放进项目会
  ① MIME 与扩展名不符，浏览器不保证渲染；
  ② 列内节奏忽大忽小；
  ③ 首屏一口气拉十几 MB。
所以统一转 JPEG、裁到同一比例、按 01、02… 重命名。

依赖：Pillow（`pip install Pillow`）

用法：
  # 商城首页图池
  python scripts/optimize-images.py \
      --src frontend/photo/_source/marquee \
      --out frontend/photo/marquee

  # 外卖首页图池
  python scripts/optimize-images.py \
      --src frontend/photo/_source/marquee-food \
      --out frontend/photo/marquee-food

  # 换比例/质量
  python scripts/optimize-images.py --src ... --out ... --size 800x1000 --quality 84
"""
from __future__ import annotations

import argparse
import os
import re
import sys

# Windows 控制台默认 GBK，输出里带非 GBK 字符会直接抛 UnicodeEncodeError 中断转换
try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:  # pragma: no cover
    pass

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit('缺少 Pillow，请先 pip install Pillow')

IMG_EXT = {'.jpg', '.jpeg', '.png', '.webp', '.avif', '.bmp', '.gif', '.tif', '.tiff'}


def natural_key(name: str):
    """自然排序：2.jpg 排在 10.jpg 前面（默认字典序会反过来）"""
    return [int(t) if t.isdigit() else t.lower() for t in re.split(r'(\d+)', name)]


def parse_size(text: str):
    m = re.fullmatch(r'\s*(\d+)\s*[x×]\s*(\d+)\s*', text)
    if not m:
        sys.exit(f'--size 格式应为 800x1000，收到：{text}')
    return int(m.group(1)), int(m.group(2))


def main() -> int:
    ap = argparse.ArgumentParser(description='原图 → 滚动图池（JPEG + 统一比例）')
    ap.add_argument('--src', required=True, help='原图目录')
    ap.add_argument('--out', required=True, help='输出目录（会按 01.jpg、02.jpg… 覆盖写入）')
    ap.add_argument('--size', default='800x1000', help='目标画布，默认 800x1000（4:5）')
    ap.add_argument('--quality', type=int, default=84, help='JPEG 质量，默认 84')
    ap.add_argument('--bg', default='255,255,255', help='补边颜色（等比缩放后居中留白），默认白')
    args = ap.parse_args()

    tw, th = parse_size(args.size)
    bg = tuple(int(x) for x in args.bg.split(','))

    files = [f for f in sorted(os.listdir(args.src), key=natural_key)
             if os.path.splitext(f)[1].lower() in IMG_EXT and os.path.isfile(os.path.join(args.src, f))]
    if not files:
        sys.exit(f'{args.src} 里没找到图片')

    os.makedirs(args.out, exist_ok=True)
    print(f'源 {len(files)} 张 -> {args.out}（{tw}x{th}, q={args.quality}）')
    for i, name in enumerate(files, 1):
        with Image.open(os.path.join(args.src, name)) as im:
            fmt, w, h = im.format, im.width, im.height
            rgb = im.convert('RGB')
            r = min(tw / w, th / h)
            nw, nh = max(1, round(w * r)), max(1, round(h * r))
            canvas = Image.new('RGB', (tw, th), bg)
            canvas.paste(rgb.resize((nw, nh), Image.LANCZOS), ((tw - nw) // 2, (th - nh) // 2))
            dst = os.path.join(args.out, f'{i:02d}.jpg')
            canvas.save(dst, 'JPEG', quality=args.quality, optimize=True, progressive=True)
            flag = '  [!] 扩展名与真实格式不符' if (
                (fmt == 'WEBP' and not name.lower().endswith(('.webp',)))
                or (fmt == 'AVIF' and not name.lower().endswith(('.avif',)))
                or (fmt == 'PNG' and not name.lower().endswith('.png'))
                or (fmt == 'JPEG' and not name.lower().endswith(('.jpg', '.jpeg')))
            ) else ''
            print(f'  {fmt:5} {w:>5}x{h:<5} -> {os.path.basename(dst)}  '
                  f'{os.path.getsize(dst) // 1024:>4}KB   ({name[:38]}){flag}')
    print('完成 OK')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
