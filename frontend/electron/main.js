const { app, BrowserWindow, shell, Menu, session } = require('electron')
const path = require('path')
const fs = require('fs')

const DEV_URL = 'http://localhost:5173'

// 生产模式 CSP: 允许本机后端 API 与 HTTPS AI 直连; Element Plus 需要内联样式
const CSP = [
  "default-src 'self'",
  "script-src 'self'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: https:",
  "font-src 'self' data:",
  "connect-src 'self' http://localhost:8080 https:"
].join('; ')

function createWindow () {
  const win = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 960,
    title: '智能题库管理系统',
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  Menu.setApplicationMenu(null)

  const distIndex = path.join(__dirname, '..', 'dist', 'index.html')
  if (process.env.VITE_DEV_SERVER_URL) {
    win.loadURL(process.env.VITE_DEV_SERVER_URL)
  } else if (fs.existsSync(distIndex)) {
    // 生产模式: 加载打包后的 dist, API 请求走 http://localhost:8080
    win.loadFile(distIndex)
  } else {
    // 未打包时尝试连接 dev server
    win.loadURL(DEV_URL)
  }

  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })
}

app.whenReady().then(() => {
  // 仅生产模式注入 CSP(开发模式 HMR 需要 websocket 与内联脚本, 不干扰)
  if (app.isPackaged) {
    session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
      callback({
        responseHeaders: {
          ...details.responseHeaders,
          'Content-Security-Policy': [CSP]
        }
      })
    })
  }
  createWindow()
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) createWindow()
})
