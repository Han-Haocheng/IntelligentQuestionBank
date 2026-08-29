import { ElMessageBox } from 'element-plus'

/**
 * 统一确认弹窗: 返回 true=确认, false=取消/关闭
 * 内部吞掉 ElMessageBox 的取消 rejection, 避免未捕获的 Promise 错误
 */
export async function confirmAction (message, title = '提示', options = {}) {
  try {
    await ElMessageBox.confirm(message, title, { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消', ...options })
    return true
  } catch (e) {
    return false
  }
}
