# AI 分析配置说明

后端通过 OpenAI 兼容接口调用大模型(默认适配 DeepSeek, 也可换任意兼容服务)。

编辑 backend/src/main/resources/application.yml:

    qbank:
      ai:
        enabled: true
        api-key: "你的API Key"   # 在 platform.deepseek.com 申请
        base-url: https://api.deepseek.com
        model: deepseek-chat
        timeout-ms: 60000

- 配置 api-key 后: 题目分析 / 学情报告调用真实大模型。
- api-key 为空时: 自动降级为内置本地规则分析(无需联网), 结果会标注来源。
- 更换服务商: 修改 base-url 与 model(如通义/智谱/Kimi 的 OpenAI 兼容地址)。