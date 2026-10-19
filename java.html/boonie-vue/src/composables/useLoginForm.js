import { computed } from 'vue'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE_RE = /^1\d{10}$/

/**
 * 账号 / 密码表单校验
 * 原项目是命令式地给 input 加 ok / err 类，这里改成计算属性驱动的状态
 *
 * @param {import('vue').Ref<string>} account
 * @param {import('vue').Ref<string>} password
 * @param {{minPwd?: number, minAcc?: number}} options
 */
export function useLoginForm(account, password, { minPwd = 6, minAcc = 3 } = {}) {
  /** 账号是否合法：邮箱 / 手机号 / 用户名长度达标 */
  const isAccountValid = (v) => {
    const value = (v ?? '').trim()
    if (!value) return false
    return EMAIL_RE.test(value) || PHONE_RE.test(value) || value.length >= minAcc
  }

  /** 账号输入框状态：'' | 'ok' | 'err' */
  const accountState = computed(() => {
    const v = account.value.trim()
    if (!v) return ''
    return isAccountValid(v) ? 'ok' : 'err'
  })

  /** 账号右侧图标 */
  const accountIcon = computed(() => {
    if (!account.value.trim()) return ''
    return accountState.value === 'ok' ? '✅' : '⚠️'
  })

  /** 密码输入框状态 */
  const passwordState = computed(() => {
    const v = password.value
    if (!v) return ''
    return v.length >= minPwd ? 'ok' : 'err'
  })

  /** 是否可提交 */
  const canSubmit = computed(
    () => isAccountValid(account.value) && password.value.length >= minPwd,
  )

  return { accountState, accountIcon, passwordState, canSubmit, isAccountValid }
}
