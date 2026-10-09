// scripts/lib/oss.mjs
// 极简 OSS 客户端（零依赖）：只做本项目脚本需要的事 —— 列举 / 上传 / 探测 / 删除。
//
// 为什么不引 aliyun-sdk：这些是**一次性运维脚本**，用 SDK 得先 npm install 一个几十 MB 的包；
// 而 OSS 的 V1 签名规则很固定（见下方 sign()），自己签反而更透明。
//
// ⚠️ 签名三个坑（都实打实踩过）：
//   ① PUT 必须把 Content-Type 写进 StringToSign（否则 403 SignatureDoesNotMatch）；
//   ② ListObjects 的 `prefix` / `max-keys` **不算**子资源，不能进 CanonicalizedResource；
//   ③ 子资源要按字典序排列、值用**未编码**形式。
//
// 配置来源：superMall-mall 的 application.yml（endpoint / bucket-name / AK / SK）+ jasypt 解密，
// 与本机跑着的服务完全一致 —— 不用在脚本里再写一份密钥。
import { execFileSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'

const ROOT = process.cwd()

/** 解一个 jasypt ENC(密文) */
function decrypt(enc) {
  const out = execFileSync(`mvn -N -o jasypt:decrypt-value "-Djasypt.plugin.value=${enc}"`,
    { encoding: 'utf8', cwd: ROOT, shell: true })
  return out.split('\n').map((l) => l.trim())
    .find((l) => l && !l.startsWith('[') && !l.includes(' ')) || ''
}

/** 还原 yml 里的 ${ENV:default} 占位符与 ENC(...) 密文 */
function resolve(raw) {
  if (!raw) return ''
  let v = raw.trim()
  const ph = v.match(/^\$\{([^:}]+)(?::([^}]*))?\}$/)
  if (ph) v = (process.env[ph[1]] ?? ph[2] ?? '').trim()
  if (v.startsWith('ENC(')) v = decrypt(v.slice(4, -1))
  return v
}

/**
 * 读取 OSS 配置。
 * @returns {{endpoint:string,bucket:string,ak:string,sk:string,base:string}} base = https://bucket.endpoint
 */
export function ossConfig(prefixFile = 'superMall-mall') {
  const dir = path.join(ROOT, prefixFile, 'src/main/resources')
  const both = ['application.yml', 'application-dev.yml']
    .map((f) => (fs.existsSync(path.join(dir, f)) ? fs.readFileSync(path.join(dir, f), 'utf8') : ''))
    .join('\n')
  const endpoint = resolve(both.match(/endpoint:\s*(\S+)/)?.[1]).replace(/^https?:\/\//, '')
  const bucket = resolve(both.match(/bucket-name:\s*(\S+)/)?.[1] || both.match(/bucket:\s*(\S+)/)?.[1])
  const ak = resolve(both.match(/access-key-id:\s*(\S+)/)?.[1])
  const sk = resolve(both.match(/access-key-secret:\s*(\S+)/)?.[1])
  if (!endpoint || !bucket || !ak || !sk) throw new Error('OSS 配置不全（endpoint/bucket/AK/SK），检查 application.yml')
  return { endpoint, bucket, ak, sk, base: `https://${bucket}.${endpoint}` }
}

const httpDate = () => new Date().toUTCString()

/** OSS V1 签名 */
function sign(cfg, { method, key = '', contentType = '', md5 = '', date, subresources = '' }) {
  const resource = `/${cfg.bucket}/${key}${subresources}`
  const str = `${method}\n${md5}\n${contentType}\n${date}\n${resource}`
  const signature = crypto.createHmac('sha1', cfg.sk).update(str).digest('base64')
  return `OSS ${cfg.ak}:${signature}`
}

/** 列举（按前缀） */
export async function listObjects(cfg, prefix) {
  const date = httpDate()
  const qs = `prefix=${encodeURIComponent(prefix)}&max-keys=1000`
  const res = await fetch(`${cfg.base}/?${qs}`, {
    headers: { Date: date, Authorization: sign(cfg, { method: 'GET', date }) }
  })
  const text = await res.text()
  if (!res.ok) throw new Error(`列举失败 HTTP ${res.status}：${text.slice(0, 200)}`)
  return [...text.matchAll(/<Contents>([\s\S]*?)<\/Contents>/g)].map((m) => ({
    key: m[1].match(/<Key>([\s\S]*?)<\/Key>/)?.[1] || '',
    size: Number(m[1].match(/<Size>(\d+)<\/Size>/)?.[1] || 0),
    lastModified: m[1].match(/<LastModified>([\s\S]*?)<\/LastModified>/)?.[1] || ''
  }))
}

/** 上传（覆盖同名对象） */
export async function putObject(cfg, key, body, contentType) {
  const date = httpDate()
  const res = await fetch(`${cfg.base}/${encodeURI(key)}`, {
    method: 'PUT',
    headers: {
      Date: date,
      'Content-Type': contentType,
      Authorization: sign(cfg, { method: 'PUT', key, contentType, date })
    },
    body
  })
  return { ok: res.ok, status: res.status, body: res.ok ? '' : (await res.text()).slice(0, 200) }
}

/** 探测对象（返回 null 表示不存在；注意 OSS 里 key 存在时 200，不存在 404） */
export async function headObject(cfg, key) {
  const date = httpDate()
  const res = await fetch(`${cfg.base}/${encodeURI(key)}`, {
    method: 'HEAD',
    headers: { Date: date, Authorization: sign(cfg, { method: 'HEAD', key, date }) }
  })
  if (!res.ok) return null
  return {
    size: Number(res.headers.get('content-length') || 0),
    contentType: res.headers.get('content-type') || '',
    lastModified: res.headers.get('last-modified') || ''
  }
}

/** 删除 */
export async function deleteObject(cfg, key) {
  const date = httpDate()
  const res = await fetch(`${cfg.base}/${encodeURI(key)}`, {
    method: 'DELETE',
    headers: { Date: date, Authorization: sign(cfg, { method: 'DELETE', key, date }) }
  })
  return { ok: res.ok, status: res.status, body: res.ok ? '' : (await res.text()).slice(0, 200) }
}

/** 根据扩展名给个合理的 Content-Type（本站只放 jpg/png/svg/webp） */
export function contentTypeOf(file) {
  const ext = path.extname(file).toLowerCase()
  return {
    '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.png': 'image/png',
    '.gif': 'image/gif', '.webp': 'image/webp', '.svg': 'image/svg+xml', '.bmp': 'image/bmp'
  }[ext] || 'application/octet-stream'
}
