/**
 * AHP 比较对配置
 * 与后端 ExpertAHPService.DIMENSION_INDICATORS 保持一致
 */

// 维度层比较对（5个维度共10对）
export const dimensionComparisons = [
  { key: '安全性_可靠性', itemA: '安全性', itemB: '可靠性', defaultScore: 2.00 },
  { key: '安全性_传输能力', itemA: '安全性', itemB: '传输能力', defaultScore: 0.33 },
  { key: '安全性_抗干扰能力', itemA: '安全性', itemB: '抗干扰能力', defaultScore: 2.00 },
  { key: '安全性_效能影响', itemA: '安全性', itemB: '效能影响', defaultScore: 0.33 },
  { key: '可靠性_传输能力', itemA: '可靠性', itemB: '传输能力', defaultScore: 0.50 },
  { key: '可靠性_抗干扰能力', itemA: '可靠性', itemB: '抗干扰能力', defaultScore: 1.00 },
  { key: '可靠性_效能影响', itemA: '可靠性', itemB: '效能影响', defaultScore: 0.50 },
  { key: '传输能力_抗干扰能力', itemA: '传输能力', itemB: '抗干扰能力', defaultScore: 2.00 },
  { key: '传输能力_效能影响', itemA: '传输能力', itemB: '效能影响', defaultScore: 1.00 },
  { key: '抗干扰能力_效能影响', itemA: '抗干扰能力', itemB: '效能影响', defaultScore: 0.50 }
]

/**
 * 指标元数据：编号、简称、释义（与业务指标说明一致）
 * key 字段名需与后端 ExpertAHPService 中指标名一致（*得分 后缀）
 */
export const indicatorMetaByDimension = {
  安全性: [
    { key: '密钥泄露得分', code: '1.1', shortLabel: '密钥泄露', description: '单位时间内密钥泄露事件发生次数' },
    { key: '被侦察得分', code: '1.2', shortLabel: '被侦察', description: '通信信号被敌方截获/识别的概率' },
    { key: '抗拦截得分', code: '1.3', shortLabel: '抗拦截', description: '通信内容被破译的难度等级' }
  ],
  可靠性: [
    { key: '崩溃比例得分', code: '2.1', shortLabel: '崩溃比例', description: '系统崩溃次数/总运行次数' },
    { key: '恢复能力得分', code: '2.2', shortLabel: '恢复能力', description: '平均恢复时间的倒数归一化值' },
    { key: '通信可用得分', code: '2.3', shortLabel: '通信可用', description: '通信链路畅通时间/总任务时间' }
  ],
  传输能力: [
    { key: '带宽得分', code: '3.1', shortLabel: '带宽', description: '系统可提供的有效传输带宽（Mbps）' },
    { key: '呼叫建立得分', code: '3.2', shortLabel: '呼叫建立', description: '从拨号到接通的平均时间（ms）' },
    { key: '传输时延得分', code: '3.3', shortLabel: '传输时延', description: '端到端信息传输延迟（ms）' },
    { key: '误码率得分', code: '3.4', shortLabel: '误码率', description: '单位时间内错误码元比例' },
    { key: '吞吐量得分', code: '3.5', shortLabel: '吞吐量', description: '单位时间内成功传输的有效数据量（Mbps）' },
    { key: '频谱效率得分', code: '3.6', shortLabel: '频谱效率', description: '单位频谱资源传输的数据量（bit/Hz）' }
  ],
  抗干扰能力: [
    { key: '信干噪比得分', code: '4.1', shortLabel: '信干噪比', description: '信号与干扰加噪声的功率比（dB）' },
    { key: '抗干扰余量得分', code: '4.2', shortLabel: '抗干扰余量', description: '系统能承受的最大干扰强度（dB）' },
    { key: '通信距离得分', code: '4.3', shortLabel: '通信距离', description: '保证通信质量的最远传输距离（km）' }
  ],
  效能影响: [
    { key: '战损率得分', code: '6.1', shortLabel: '战损率', description: '通信装备在作战行动中遭受毁伤的比例' },
    { key: '任务完成率得分', code: '6.2', shortLabel: '任务完成率', description: '通信保障任务成功完成的次数与总任务次数的比值' },
    { key: '致盲率得分', code: '6.3', shortLabel: '致盲率', description: '通信装备在作战行动中遭受致盲攻击导致失效的比例' }
  ]
}

/** 由元数据生成指标 key 列表（供矩阵顺序与后端一致） */
export const indicatorElements = Object.fromEntries(
  Object.entries(indicatorMetaByDimension).map(([dim, list]) => [dim, list.map((x) => x.key)])
)

/** 指标释义（完整一句，供表格/悬停） */
export const indicatorDescriptions = Object.fromEntries(
  Object.entries(indicatorMetaByDimension).map(([dim, list]) => [
    dim,
    Object.fromEntries(list.map((x) => [x.key, `${x.code} ${x.shortLabel}：${x.description}`]))
  ])
)

export function getIndicatorMeta(dim, key) {
  return indicatorMetaByDimension[dim]?.find((m) => m.key === key)
}

/** 矩阵表头/行名：编号 + 简称（半角空格，全项目统一） */
export function indicatorDisplayLabel(dim, key) {
  const m = getIndicatorMeta(dim, key)
  return m ? `${m.code} ${m.shortLabel}` : key.replace(/得分$/, '')
}

/** 旭日图等仅需文字块时：仅简称，避免与维度内圈样式混杂 */
export function indicatorShortLabel(dim, key) {
  const m = getIndicatorMeta(dim, key)
  return m ? m.shortLabel : key.replace(/得分$/, '')
}

/** 悬停：编号 + 释义全文 */
export function indicatorTooltipText(dim, key) {
  const m = getIndicatorMeta(dim, key)
  return m ? `${m.code} ${m.shortLabel}：${m.description}` : indicatorDescriptions[dim]?.[key] || key
}

// 指标层比较对（各维度内部）
export const indicatorComparisons = {
  安全性: [
    { key: '密钥泄露得分_被侦察得分', itemA: '密钥泄露得分', itemB: '被侦察得分', defaultScore: 2.00 },
    { key: '密钥泄露得分_抗拦截得分', itemA: '密钥泄露得分', itemB: '抗拦截得分', defaultScore: 1.00 },
    { key: '被侦察得分_抗拦截得分', itemA: '被侦察得分', itemB: '抗拦截得分', defaultScore: 0.50 }
  ],
  可靠性: [
    { key: '崩溃比例得分_恢复能力得分', itemA: '崩溃比例得分', itemB: '恢复能力得分', defaultScore: 1.00 },
    { key: '崩溃比例得分_通信可用得分', itemA: '崩溃比例得分', itemB: '通信可用得分', defaultScore: 0.50 },
    { key: '恢复能力得分_通信可用得分', itemA: '恢复能力得分', itemB: '通信可用得分', defaultScore: 0.50 }
  ],
  传输能力: [
    { key: '带宽得分_呼叫建立得分', itemA: '带宽得分', itemB: '呼叫建立得分', defaultScore: 3.00 },
    { key: '带宽得分_传输时延得分', itemA: '带宽得分', itemB: '传输时延得分', defaultScore: 2.00 },
    { key: '带宽得分_误码率得分', itemA: '带宽得分', itemB: '误码率得分', defaultScore: 2.00 },
    { key: '带宽得分_吞吐量得分', itemA: '带宽得分', itemB: '吞吐量得分', defaultScore: 0.50 },
    { key: '带宽得分_频谱效率得分', itemA: '带宽得分', itemB: '频谱效率得分', defaultScore: 1.00 },
    { key: '呼叫建立得分_传输时延得分', itemA: '呼叫建立得分', itemB: '传输时延得分', defaultScore: 0.50 },
    { key: '呼叫建立得分_误码率得分', itemA: '呼叫建立得分', itemB: '误码率得分', defaultScore: 0.50 },
    { key: '呼叫建立得分_吞吐量得分', itemA: '呼叫建立得分', itemB: '吞吐量得分', defaultScore: 0.33 },
    { key: '呼叫建立得分_频谱效率得分', itemA: '呼叫建立得分', itemB: '频谱效率得分', defaultScore: 0.50 },
    { key: '传输时延得分_误码率得分', itemA: '传输时延得分', itemB: '误码率得分', defaultScore: 1.00 },
    { key: '传输时延得分_吞吐量得分', itemA: '传输时延得分', itemB: '吞吐量得分', defaultScore: 0.33 },
    { key: '传输时延得分_频谱效率得分', itemA: '传输时延得分', itemB: '频谱效率得分', defaultScore: 0.50 },
    { key: '误码率得分_吞吐量得分', itemA: '误码率得分', itemB: '吞吐量得分', defaultScore: 0.33 },
    { key: '误码率得分_频谱效率得分', itemA: '误码率得分', itemB: '频谱效率得分', defaultScore: 0.50 },
    { key: '吞吐量得分_频谱效率得分', itemA: '吞吐量得分', itemB: '频谱效率得分', defaultScore: 2.00 }
  ],
  抗干扰能力: [
    { key: '信干噪比得分_抗干扰余量得分', itemA: '信干噪比得分', itemB: '抗干扰余量得分', defaultScore: 1.00 },
    { key: '信干噪比得分_通信距离得分', itemA: '信干噪比得分', itemB: '通信距离得分', defaultScore: 2.00 },
    { key: '抗干扰余量得分_通信距离得分', itemA: '抗干扰余量得分', itemB: '通信距离得分', defaultScore: 2.00 }
  ],
  效能影响: [
    { key: '战损率得分_任务完成率得分', itemA: '战损率得分', itemB: '任务完成率得分', defaultScore: 0.50 },
    { key: '战损率得分_致盲率得分', itemA: '战损率得分', itemB: '致盲率得分', defaultScore: 1.00 },
    { key: '任务完成率得分_致盲率得分', itemA: '任务完成率得分', itemB: '致盲率得分', defaultScore: 2.00 }
  ]
}

// 维度列表
export const dimensions = [
  { code: '安全性', name: '安全性' },
  { code: '可靠性', name: '可靠性' },
  { code: '传输能力', name: '传输能力' },
  { code: '抗干扰能力', name: '抗干扰能力' },
  { code: '效能影响', name: '效能影响' }
]

/** 各维度含义说明（表头悬停展示） */
export const dimensionDescriptions = {
  安全性: '通信保密、防侦察与抗截获等综合安全能力在效能评估中的体现。',
  可靠性: '网络与业务连续运行能力，含故障恢复、通信可用性等。',
  传输能力: '带宽、时延、误码、呼叫建立、吞吐量与频谱效率等传输质量相关指标。',
  抗干扰能力: '在干扰环境下维持链路质量的能力，如信干噪比、余量与可达距离。',
  效能影响: '通信对作战任务结果的直接影响，如战损、任务完成与致盲率等。'
}

/** 默认把握度（0~1），用于说明与「有把握」档位参照 */
export const defaultConfidence = 0.8

/** 新专家或无已存数据时，矩阵上三角默认把握度（刻意小于 0.6，表示尚未形成把握） */
export const defaultBlankConfidence = 0.55

/** Saaty 正互反标度范围：上三角 aᵢⱼ∈[1/9, 9]；>1 表示行比列重要，<1 表示行不如列重要（如 0.33≈1/3） */
export const AHP_SCORE_MIN = 1 / 9
export const AHP_SCORE_MAX = 9

/**
 * Saaty 1～9 标度完整说明（页面顶部表格）
 * 列：标度 | 含义 | 说明 | 适用场景
 */
/**
 * 把握度等级与判断可信度 λ（供矩阵中「把握度」数值填写参照）
 */
export const ahpConfidenceLevelLegend = [
  {
    level: '1 级',
    meaning: '完全确认',
    standard:
      '评价者对评价指标非常熟悉，经验丰富，认为自己的评价结果完全可信，无任何异议；数据测量和分析完全可信。',
    lambda: '1'
  },
  {
    level: '2 级',
    meaning: '确认',
    standard:
      '评价者对指标熟悉，有一定的从业经历，认为自己的评价结果可信；数据测量和分析可信。',
    lambda: '0.8'
  },
  {
    level: '3 级',
    meaning: '基本确认',
    standard:
      '评价者对指标有一定的了解，但不是特别熟悉，认为自己的评价结果有一定的可信度，但没有较大的把握；数据测量和分析基本可信。',
    lambda: '0.6'
  },
  {
    level: '4 级',
    meaning: '不能确认',
    standard:
      '评价者对评价指标不熟悉，认为自己的评价结果不能确认；数据测量和分析不完全可信。',
    lambda: '< 0.6'
  }
]

export const ahpScaleFullLegend = [
  { scale: '1', meaning: '同样重要', note: '两个因素相比，具有相同的重要性', scene: '两者同等重要，难以区分' },
  { scale: '3', meaning: '稍微重要', note: '一个因素比另一个稍微重要', scene: '经验判断轻微偏向一方' },
  { scale: '5', meaning: '明显重要', note: '一个因素比另一个明显重要', scene: '经验判断明显偏向一方' },
  { scale: '7', meaning: '强烈重要', note: '一个因素比另一个强烈重要', scene: '实际显示非常偏向一方' },
  { scale: '9', meaning: '极端重要', note: '一个因素比另一个极端重要', scene: '绝对偏向一方，证据确凿' },
  { scale: '2、4、6、8', meaning: '相邻标度的中间值', note: '需要折中时使用', scene: '介于两个判断之间' }
]

/** 将配置默认标度转为矩阵上三角整数 1～9（≥1 时四舍五入并截断到区间；小于 1 的默认记为 1，由用户再调） */
export function defaultScoreToInteger(s) {
  const x = Number(s)
  if (!Number.isFinite(x) || x <= 0) return 1
  if (x >= 1) return Math.min(9, Math.max(1, Math.round(x)))
  return 1
}

// 兼容旧组件：简要标度（弹层等）
export const scaleDescription = [
  { value: 9, description: '极端重要' },
  { value: 7, description: '强烈重要' },
  { value: 5, description: '明显重要' },
  { value: 3, description: '稍微重要' },
  { value: 1, description: '同样重要' },
  { value: '2,4,6,8', description: '相邻标度中间值' }
]
