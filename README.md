# Wear OS GPS Logger

适用于三星 Galaxy Watch 8 的低功耗 GPS 轨迹记录器。

## 功能特性

- **低功耗设计** — 自适应采样频率，亮屏 10s/15m，息屏 30s/50m
- **后台记录** — Foreground Service 保活，支持全天续航
- **轨迹管理** — 查看历史轨迹、统计信息、删除确认
- **GPX 1.1 导出** — 标准格式，含海拔/速度/边界框元数据
- **文件分享** — 通过蓝牙/WiFi 分享至手机
- **Android 14+ 合规** — 完整运行时权限处理

## 技术栈

- Kotlin 2.0 + Jetpack Compose for Wear OS
- Room 数据库 + MVVM 架构
- Fused Location Provider (Google Play Services)
- Target SDK 34 / Min SDK 30 (Wear OS 3.0+)

## 项目结构

```
wear/src/main/java/com/example/gpslogger/
├── data/           # Room 数据库 (TrackEntity + LocationEntity)
├── service/        # 低功耗 GPS 前台服务
├── ui/             # Wear OS Compose 界面
├── export/         # GPX 1.1 导出工具
├── GpsLoggerApp.kt # Application 类
└── MainActivity.kt # 导航 + 权限处理
```

## 安装

1. 用 Android Studio 打开项目
2. 连接 Galaxy Watch 8（开启开发者模式 + ADB 调试）
3. 点击 Run 安装到手表
4. 授予位置权限后即可使用

## License

MIT
