# AI 分析配置说明 (v2: 前端本地)

v2 起 AI 分析由**前端浏览器本地直连**大模型完成，不再经过后端。

配置入口: 系统界面左侧菜单 -> 「AI 设置」

- 接口地址: 默认 https://api.deepseek.com (任意 OpenAI 兼容服务均可)
- API Key: 在 platform.deepseek.com 申请, 仅保存在本机浏览器 localStorage
- 模型: 默认 deepseek-chat, 可换同服务商其他模型
- 测试连接: 保存后点击即可验证配置

网络说明:

- 开发模式(npm run dev): 请求经 Vite 代理 /ai-proxy 转发到接口地址, 规避浏览器跨域限制
- Electron 生产模式: 无跨域限制, 直接连接接口地址

历史记录: 题目分析与学情报告结果保存在本机浏览器(localStorage, 最近 50 条), 可在设置页清空。
