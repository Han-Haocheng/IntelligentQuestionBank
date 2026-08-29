const { app, BrowserWindow, shell, Menu } = require('electron')
const path = require('path')
const fs = require('fs')

const DEV_URL = 'http://localhost:5173'

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

app.whenReady().then(createWindow)

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) createWindow()
})
