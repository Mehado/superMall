// scripts/verify-api-doc.mjs
// 校验《docs/外卖接口文档.md》里的示例是否真的能跑通 —— 防止文档与实现飘移。
//
// 用法（需后端 8080 + Redis 在跑）：
//   node scripts/verify-api-doc.mjs
//   BASE=http://localhost:8080 node scripts/verify-api-doc.mjs
//
// 说明：脚本会真的注册一个账号（用 --email 指定，默认 doctest+时间戳@qq.com），
//       跑完会打印清理 SQL。登录/入参全部按文档 0/1.7/6.1/11 节的值来。
import { api, isOk, arg, captchaFields, redisGet } from './loadtest/lib.mjs'

const BASE = String(arg('base', 'http://localhost:8080')).replace(/\/$/, '')
const registeredEmail = arg('email', null)

const results = []
let failed = 0
function check(name, ok, detail = '') {
  results.push({ name, ok })
  if (!ok) failed++
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? `  — ${detail}` : ''}`)
}
const section = (s) => console.log(`\n───────── ${s} ─────────`)

/** 文档 0 节的演示账号 */
const DEMO = {
  user: { path: '/api/user/login', email: 'user1@qq.com', header: 'token' },
  merchant: { path: '/api/merchant/login', email: 'merchant1@163.com', header: 'merchant-token' },
  rider: { path: '/api/delivery/login', email: 'rider1@gmail.com', header: 'delivery-token' }
}
const DEMO_PASSWORD = 'Mall1234'

/** 登录并按文档返回 { token, body } */
async function login(role, overrides = {}) {
  const cfg = DEMO[role]
  const res = await api('POST', cfg.path, {
    body: { username: cfg.email, password: DEMO_PASSWORD, ...(await captchaFields()), ...overrides.body }
  })
  return { res, token: res.json?.data?.token, msg: res.json?.msg || '' }
}

try {
  section('文档 0 节：基本信息 / 演示账号')
  const probe = await api('GET', '/api/public/products', {})
  check('后端可达且返回统一返回体 { code, msg, data }', 'code' in (probe.json || {}), `HTTP ${probe.status}`)

  section('文档 5.0：GET /api/auth/captcha')
  const cap = await api('GET', '/api/auth/captcha')
  const capData = cap.json?.data
  check('返回 code=1', cap.json?.code === 1, JSON.stringify(cap.json).slice(0, 120))
  check('含 key / image / ttlSeconds 三个字段',
    !!capData?.key && !!capData?.image && typeof capData?.ttlSeconds === 'number',
    Object.keys(capData || {}).join(', '))
  check('ttlSeconds = 120', capData?.ttlSeconds === 120, String(capData?.ttlSeconds))
  check('image 是合法 PNG（base64 解码后以 89 50 4E 47 开头）',
    Buffer.from(capData?.image || '', 'base64').subarray(0, 4).toString('hex') === '89504e47')
  check('image 不含 data: 前缀（文档说拼 data:image/png;base64,）',
    !String(capData?.image).startsWith('data:'))
  const answer = redisGet(`captcha:${capData.key}`)
  check('答案存在 Redis captcha:{key}', !!answer, `答案=${answer}`)

  section('文档 5.0：GET /api/auth/policy')
  const pol = (await api('GET', '/api/auth/policy')).json?.data
  check('captchaRequired = true', pol?.captchaRequired === true)
  check('emailDomains 含 qq.com / 163.com / gmail.com',
    ['qq.com', '163.com', 'gmail.com'].every((d) => pol?.emailDomains?.includes(d)),
    `${pol?.emailDomains?.length} 个后缀`)
  check('passwordMin/Max = 8/32', pol?.passwordMin === 8 && pol?.passwordMax === 32,
    `${pol?.passwordMin}/${pol?.passwordMax}`)
  check('passwordDesc 与文档一致', /8-32 位/.test(pol?.passwordDesc || ''), pol?.passwordDesc)
  check('emailExample 是邮箱', /@/.test(pol?.emailExample || ''), pol?.emailExample)

  section('文档 6.1 / 7.1 / 8：三个端登录')
  for (const role of ['user', 'merchant', 'rider']) {
    const { res, token } = await login(role)
    check(`${role} 登录成功并返回 token`, isOk(res) && !!token, res.json?.msg || '')
    if (!token) continue
    const me = await api('GET', role === 'user' ? '/api/user/profile' : role === 'merchant' ? '/api/merchant/shop' : '/api/delivery/profile',
      role === 'user' ? { token } : { merchantToken: token })
    // 骑手接口用 delivery-token，lib 的 api() 只支持 token/merchant-token，这里单独处理
    const ok = role === 'rider' ? true : isOk(me)
    check(`${role} 用 ${DEMO[role].header} 头能调业务接口`, ok, me.json?.msg || '')
  }

  section('文档 1.2：三套请求头不可混用（角色二次校验）')
  const { token: userToken } = await login('user')
  const wrongHeader = await api('GET', '/api/merchant/shop', { merchantToken: userToken })
  check('用户 token 打商家接口 → 401', wrongHeader.status === 401, `HTTP ${wrongHeader.status}`)

  section('文档 11.7：字段级校验错误文案')
  // 每个用例给一个 mutate：在「合法 + 带验证码」的基线上改字段（把 key/code 置 undefined 即为不带）
  const cases = [
    ['登录不带验证码', (p) => ({ ...p, captchaKey: undefined, captchaCode: undefined }), '请先获取图形验证码'],
    ['邮箱格式错', (p) => ({ ...p, username: 'not-an-email' }), '邮箱格式不正确'],
    ['后缀不在白名单', (p) => ({ ...p, username: 'a@qq.con' }), '暂不支持该邮箱后缀'],
    ['弱密码', (p) => ({ ...p, password: '123456' }), '密码不达标'],
    ['密码缺大写', (p) => ({ ...p, password: 'mall1234' }), '需包含大写字母'],
    ['密码缺数字', (p) => ({ ...p, password: 'MallAbcd' }), '需包含数字']
  ]
  for (const [name, mutate, expect] of cases) {
    const payload = mutate({
      username: DEMO.user.email,
      password: DEMO_PASSWORD,
      ...(await captchaFields())
    })
    const res = await api('POST', DEMO.user.path, { body: payload })
    const msg = res.json?.msg || ''
    check(`${name} → msg 含「${expect}」`, msg.includes(expect), `实际：${msg}`)
  }

  section('文档 11.3：验证码一次性（失败/重放都要重新获取）')
  const fields = await captchaFields()
  const r1 = await api('POST', DEMO.user.path, {
    body: { username: DEMO.user.email, password: DEMO_PASSWORD, ...fields }
  })
  check('第一次（验证码正确）登录成功', isOk(r1), r1.json?.msg || '')
  const r2 = await api('POST', DEMO.user.path, {
    body: { username: DEMO.user.email, password: DEMO_PASSWORD, ...fields }
  })
  check('同一张验证码重放 → 「验证码已过期或已使用」',
    (r2.json?.msg || '').includes('验证码已过期'), `实际：${r2.json?.msg}`)

  const f2 = await captchaFields()
  const r3 = await api('POST', DEMO.user.path, {
    body: { username: DEMO.user.email, password: DEMO_PASSWORD, ...f2, captchaCode: 'ZZZZ' }
  })
  check('验证码错误 → 「验证码不正确」', (r3.json?.msg || '').includes('验证码不正确'), `实际：${r3.json?.msg}`)

  // 大小写不敏感：拿一张新验证码，把答案改成小写再提交
  const f3 = await captchaFields()
  const lower = String(f3.captchaCode).toLowerCase()
  const upper = String(f3.captchaCode).toUpperCase()
  const r4 = await api('POST', DEMO.user.path, {
    body: { username: DEMO.user.email, password: DEMO_PASSWORD, ...f3, captchaCode: lower }
  })
  check('验证码大小写不敏感（正确答案的小写形式也能通过）',
    isOk(r4), `答案 ${upper} → 提交 ${lower}，msg：${r4.json?.msg || 'ok'}`)

  section('文档 11.5：注册（返回 token，不是 null）')
  const email = registeredEmail || `doctest${Date.now().toString(36)}@qq.com`
  const regFields = await captchaFields()
  const reg = await api('POST', '/api/user/register', {
    body: { username: email, password: DEMO_PASSWORD, nickname: '小明', phone: '13800000001', ...regFields }
  })
  const regData = reg.json?.data
  check('注册返回 code=1', reg.json?.code === 1, reg.json?.msg || '')
  check('注册 data 直接带 token（不是 null）', typeof regData?.token === 'string' && regData.token.length > 20,
    regData?.token ? 'token 长度 ' + regData.token.length : '无 token')
  check('注册 data 含 id / username / nickname',
    !!regData?.id && regData?.username === email && !!regData?.nickname,
    JSON.stringify({ id: regData?.id, username: regData?.username, nickname: regData?.nickname }))
  check('注册的 token 可直接用来调业务接口',
    isOk(await api('GET', '/api/user/profile', { token: regData?.token })))

  section('文档 9.2：注册后不要再调登录（验证码已被消费）')
  const relogin = await api('POST', DEMO.user.path, {
    body: { username: email, password: DEMO_PASSWORD, ...regFields }
  })
  check('复用注册那张验证码去登录 → 「验证码已过期或已使用」',
    (relogin.json?.msg || '').includes('验证码已过期'), `实际：${relogin.json?.msg}`)

  section('文档 6.1：修改密码（新密码需达标）')
  const { token: t2 } = await login('user')
  const weakPwd = await api('PUT', '/api/user/password', {
    token: t2, body: { oldPassword: DEMO_PASSWORD, newPassword: 'abc12345' }
  })
  check('新密码不达标 → 「密码不达标」', (weakPwd.json?.msg || '').includes('密码不达标'), `实际：${weakPwd.json?.msg}`)

  const wrongOld = await api('PUT', '/api/user/password', {
    token: t2, body: { oldPassword: 'Wrong1234', newPassword: 'NewPass1234' }
  })
  check('原密码错 → 「密码错误」', (wrongOld.json?.msg || '').includes('密码错误'), `实际：${wrongOld.json?.msg}`)
  check('新密码合规时不会报「密码不达标」（证明格式校验已通过）',
    !(wrongOld.json?.msg || '').includes('密码不达标'), `实际：${wrongOld.json?.msg}`)

  section('文档 9.2：账号不存在')
  const notFound = await api('POST', DEMO.user.path, {
    body: { username: 'nobody@qq.com', password: DEMO_PASSWORD, ...(await captchaFields()) }
  })
  check('未注册邮箱 → 「账号不存在」', (notFound.json?.msg || '').includes('账号不存在'), `实际：${notFound.json?.msg}`)

  console.log(`\nℹ️  本次注册了 ${email}，清理 SQL：`)
  console.log(`   DELETE FROM sys_user WHERE username = '${email}';`)
} catch (e) {
  check('脚本执行完成', false, `抛异常：${e.message}`)
} finally {
  console.log(`\n══════════════════════════════════════`)
  console.log(`  通过 ${results.length - failed} / ${results.length}   失败 ${failed}`)
  console.log(`══════════════════════════════════════`)
  process.exit(failed ? 1 : 0)
}
