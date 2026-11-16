# 青序 · To-do List

一款浅绿色、简约风格的 Android 每日待办与习惯打卡应用。

## 功能
- 创建、编辑、删除待办，支持每天重复或仅一次。
- 今日任务一键勾选 / 撤销，数据按日期保存，不会在跨日时丢失。
- **每项任务独立选择是否统计连续坚持天数**；不启用时任务只作为普通待办。
- 日期切换查看和勾选历史任务、每日完成率、近 7 天统计。
- 本地离线保存，不需要账号、不上传数据。

## 技术
Kotlin + Jetpack Compose + Material 3；SharedPreferences/JSON 离线存储。Android 8.0 (API 26) 起。

## 构建
- Android Studio 使用 JDK 17 打开仓库，安装 Android SDK 35。
- 使用 Gradle 8.9、Android Gradle Plugin 8.7.3。
- 运行 `gradle assembleDebug`（或安装 Gradle Wrapper 后运行 `./gradlew assembleDebug`）。
- 仓库 GitHub Actions 在推送到 main 时自动构建并上传 debug APK：进入 Actions → Android APK → Artifacts 下载。

## 打卡规则
- 每天重复的任务在创建当天起每天出现；一次性任务只在指定日期出现。
- 当前连续天数包含“今天已完成”或“昨天已完成”的连续区间；今天未完成但昨天已完成时不会提前断签。
- 仅对开启“统计坚持天数”的每日任务显示连续天数。
- 历史日期可通过首页日期左右切换查看并修改完成记录；统计根据已有记录实时计算。

## 视觉
薄荷绿 / 鼠尾草绿、柔和卡片、轻量日历与清单图标；应用图标以同一清单概念制作矢量版本，保证 Android 桌面适配。
