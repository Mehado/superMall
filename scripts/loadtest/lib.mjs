// scripts/loadtest/lib.mjs
// 零依赖 HTTP 压测小工具（Node 18+ 内置 fetch），供同目录脚本复用。
import { execFileSync } from 'node:child_process'

/** 读取 --name value 形式的命令行参数 */
export function arg(name, def) {
  const i = process.argv.indexOf(`--${name}`)
  if (i === -1) return def
  const v = process.argv[i + 1]
  return v === undefined || v.startsWith('--') ? true : v
}

export const num = (name, def) => Number(arg(name, def))
export const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

export const BASE = String(arg('base', 'http://localhost:8080')).replace(/\/$/, '')

/** Redis 连接参数（验证码答案存在 Redis，压测需要读它） */
const REDIS_HOST = process.env.REDIS_HOST || '127.0.0.1'
const REDIS_PORT = process.env.REDIS_PORT || '6379'
// 本机开发 Redis 密码（项目文档里已写明）；换环境请设 REDIS_PASSWORD
const REDIS_PW = process.env.REDIS_PASSWORD || 'mehado'

/** 用 redis-cli 跑一条命令，返回 stdout（已 trim） */
function redisCli(args) {
  const base = ['-h', REDIS_HOST, '-p', REDIS_PORT, '-a', REDIS_PW]
  const attempts = [[...base, '--no-auth-warning', ...args], [...base, ...args]]
  let lastErr
  for (const a of attempts) {
    try {
      return execFileSync('redis-cli', a, { encoding: 'utf8' }).trim()
    } catch (e) {
      lastErr = e
    }
  }
  throw new Error(`redis-cli 执行失败：${lastErr?.message || '未知错误'}`)
}

/** 读一个 Redis key */
export function redisGet(key) {
  return redisCli(['GET', key])
}

/**
 * 取一组图形验证码字段，直接展开进请求体：
 *   body: { username, password, ...(await captchaFields()) }
 *
 * 为什么能取到答案：后端把答案存在 Redis（captcha:{key}），压测脚本在本机跑，
 * 可以直接读。这样既不用给后端开后门，也能跑通带验证码的真实登录链路。
 * 后端把 super-mall.captcha.enabled 置 false 时，接口不给 key，返回 {}。
 */
export async function captchaFields() {
  const res = await api('GET', '/api/auth/captcha')
  const key = res.json?.data?.key
  if (!key) return {}
  const code = redisGet(`captcha:${key}`)
  if (!code) {
    throw new Error(
      `Redis 取不到 captcha:${key}。请确认 Redis 在 ${REDIS_HOST}:${REDIS_PORT} 且在跑，` +
        `密码是否正确（可用 REDIS_PASSWORD 覆盖，当前默认 mehado）`
    )
  }
  return { captchaKey: key, captchaCode: code }
}

/**
 * 可选：用 redis-cli 清理指定 key（设置 REDIS_PASSWORD 环境变量后生效）。
 * 用于重置限流窗口，让压测结果可复现。
 */
export function redisClean(keys) {
  const host = REDIS_HOST
  const port = REDIS_PORT
  const pw = REDIS_PW
  const attempts = [
    ['-h', host, '-p', port, '-a', pw, '--no-auth-warning', 'DEL', ...keys],
    ['-h', host, '-p', port, '-a', pw, 'DEL', ...keys],
  ]
  for (const args of attempts) {
    try {
      execFileSync('redis-cli', args, { stdio: 'ignore' })
      return true
    } catch {
      /* 换下一种参数重试 */
    }
  }
  return false
}

/**
 * 可选：清空所有接口限流窗口（rate:limit:*）。设置 REDIS_PASSWORD 后生效。
 * 注意：限流按 getRemoteAddr() 取 IP，Node 走 IPv6 时是 0:0:0:0:0:0:0:1、curl 可能走 127.0.0.1，
 * 所以按 pattern 清而不是写死某个 IP。
 */
export function redisClearRateLimits() {
  const pw = REDIS_PW
  const base = ['-h', REDIS_HOST, '-p', REDIS_PORT, '-a', pw]
  for (const args of [[...base, '--no-auth-warning'], base]) {
    try {
      const out = execFileSync('redis-cli', [...args, 'KEYS', 'rate:limit:*'], { encoding: 'utf8' })
      const keys = out.split(/\r?\n/).map((s) => s.trim()).filter(Boolean)
      if (keys.length) execFileSync('redis-cli', [...args, 'DEL', ...keys], { stdio: 'ignore' })
      return true
    } catch {
      /* 换下一种参数重试 */
    }
  }
  return false
}

/** 发一个请求，返回耗时与解析后的响应体 */
export async function api(method, path, { token, merchantToken, body } = {}) {
  const headers = {}
  if (token) headers.token = token
  if (merchantToken) headers['merchant-token'] = merchantToken
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const started = performance.now()
  try {
    const res = await fetch(BASE + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    const text = await res.text()
    let json = null
    try {
      json = JSON.parse(text)
    } catch {
      /* 非 JSON 响应 */
    }
    return { status: res.status, ms: performance.now() - started, json, text }
  } catch (e) {
    return { status: 0, ms: performance.now() - started, json: null, text: String(e?.message || e) }
  }
}

export const isOk = (r) => r.json?.code === 1

export function percentile(sorted, p) {
  if (!sorted.length) return 0
  const i = Math.min(sorted.length - 1, Math.max(0, Math.ceil((p / 100) * sorted.length) - 1))
  return sorted[i]
}

export function report({ title, latencies, ok, fail, seconds, reasons }) {
  const sorted = [...latencies].sort((a, b) => a - b)
  console.log(`\n===== ${title} =====`)
  console.log(`总请求  : ${latencies.length}`)
  console.log(`成功    : ${ok}`)
  console.log(`失败    : ${fail}`)
  console.log(`耗时    : ${seconds.toFixed(2)} s`)
  console.log(`QPS     : ${(latencies.length / seconds).toFixed(1)}`)
  console.log(
    `延迟 ms : p50=${percentile(sorted, 50).toFixed(1)}  p90=${percentile(sorted, 90).toFixed(1)}` +
      `  p99=${percentile(sorted, 99).toFixed(1)}  max=${(sorted.at(-1) || 0).toFixed(1)}`
  )
  if (reasons?.size) {
    console.log('失败原因 :')
    for (const [k, v] of [...reasons.entries()].sort((a, b) => b[1] - a[1])) {
      console.log(`  - ${k} × ${v}`)
    }
  }
}
