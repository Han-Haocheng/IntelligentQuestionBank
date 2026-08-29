// 前端公共常量与工具函数(统一各页面重复定义)
export const TYPE_NAMES = ['单选题', '多选题', '填空题', '判断题', '简答题']
export const DIFFICULTY_NAMES = ['入门', '简单', '中等', '较难', '困难']
export const MODE_NAMES = ['顺序', '随机', '错题重做']

/** 选项字母: 0 -> A */
export function letter (i) {
  return String.fromCharCode(65 + i)
}

/** 拆分英文逗号标签 */
export function splitTags (tags) {
  return tags ? tags.split(',').filter(t => t) : []
}
