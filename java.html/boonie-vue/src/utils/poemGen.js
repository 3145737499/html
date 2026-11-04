/**
 * 烟雨集句 · 诗生成器
 * ------------------------------------------------------------
 * 思路（无外部 API，纯前端、离线可用）：
 *   本功能的核心是「根据其他人留下的诗，合成一首风格相近的新诗」——
 *   这正是古典诗词里的「集句」：取众人之句，重新缀成一首。
 *
 *   1. 把语料（站内固有诗笺 + 每日诗 + 诗笺卡片描述 + 访客留句）按标点拆成「句读」(clause)；
 *   2. 统计语料中最常见的「句长」与「每首诗句数」，让生成物在 字数 / 格式 上贴近主页诗笺卡片；
 *   3. 从真实句读中挑句缀成新诗（保证可读、风格一致）；
 *      并以一定概率做「桥接」：把两句同长度的实句在中部裁开、互换后半，得到一句全新却仍通顺的新句，
 *      既「生成」又「取自众人之诗」。
 *
 * 暴露：generatePoemFrom(lines) -> { text, len, count }
 */

/** 偏好的句长（古典汉诗常见），用于在分析时加权 */
const PREF_LEN = [7, 5, 6, 4, 8]
/** 偏好的每诗句数（让生成物更像「一首诗」），用于加权 */
const PREF_COUNT = [2, 3, 4, 1]
/** 桥接（生成全新句读）的概率 */
const BRIDGE_RATE = 0.35

/** 把一段诗句按标点拆成若干句读，去空白、过滤过短 */
function splitClauses(text) {
    return String(text || '')
        .split(/[，。？！；、\n]/)
        .map((s) => s.replace(/\s/g, '').trim())
        .filter((s) => s.length >= 2)
}

function pick(arr) {
    return arr.length ? arr[Math.floor(Math.random() * arr.length)] : ''
}

/**
 * 分析语料，得出目标句长与目标句数
 */
function analyze(lines) {
    const poems = lines.map(splitClauses).filter((a) => a.length)
    const lenCount = {}
    const countCount = {}

    poems.forEach((clauses) => {
        countCount[clauses.length] = (countCount[clauses.length] || 0) + 1
        clauses.forEach((c) => {
            lenCount[c.length] = (lenCount[c.length] || 0) + 1
        })
    })

    let bestLen = 7
    let bestLenScore = -1
    for (const k in lenCount) {
        const L = +k
        const score = lenCount[k] * (PREF_LEN.includes(L) ? 2 : 1)
        if (score > bestLenScore) {
            bestLenScore = score
            bestLen = L
        }
    }

    let bestCount = 2
    let bestCountScore = -1
    for (const k in countCount) {
        const C = +k
        const score = countCount[k] * (PREF_COUNT.includes(C) ? 2 : 1)
        if (score > bestCountScore) {
            bestCountScore = score
            bestCount = C
        }
    }
    if (bestCount < 1) bestCount = 2
    if (bestCount > 4) bestCount = 4

    return {len: bestLen, count: bestCount}
}

/**
 * 取一句用于缀诗：以 BRIDGE_RATE 概率做「桥接」生成全新句读，否则取真实句读
 * @param {string[]} sameLen 与目标句长相同的句读集合
 * @param {string[]} pool 全部句读（桥接素材不足时的兜底）
 */
function makeClause(sameLen, pool) {
    if (sameLen.length >= 2 && Math.random() < BRIDGE_RATE) {
        const a = pick(sameLen)
        const b = pick(sameLen)
        if (a && b && a !== b) {
            const cut = 1 + Math.floor(Math.random() * (a.length - 1))
            return a.slice(0, cut) + b.slice(cut)
        }
    }
    return pick(pool)
}

/**
 * 入口：基于语料集句，合成一首贴近主页诗笺卡片风格的新诗
 * @param {string[]} lines 语料（每项为一段诗句文本）
 * @returns {{ text: string, len: number, count: number }}
 */
export function generatePoemFrom(lines) {
    const safe = Array.isArray(lines) ? lines.filter(Boolean) : []
    const pool = [...new Set(safe.flatMap(splitClauses))]
    if (pool.length === 0) {
        return {text: '烟雨空蒙，且待君来留第一句。', len: 0, count: 0}
    }

    const {len, count} = analyze(safe)
    const sameLen = pool.filter((c) => c.length === len)
    const source = sameLen.length >= 2 ? sameLen : pool
    const n = Math.min(count, Math.max(1, source.length))

    const chosen = []
    const used = new Set()
    let guard = 0
    while (chosen.length < n && guard++ < 200) {
        const c = makeClause(sameLen, source)
        if (!c || used.has(c)) continue
        used.add(c)
        chosen.push(c)
    }
    // 语料很小不足以凑满时，放宽去重补满
    while (chosen.length < n && chosen.length < source.length) {
        const c = pick(source)
        if (!chosen.includes(c)) chosen.push(c)
    }

    const text = chosen.join('，') + '。'
    return {text, len, count: chosen.length}
}
