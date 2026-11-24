# Tiny-Metronome（精拍节拍器）

一款面向 **Android 16（API 36）** 的高精度原生节拍器，采用 Kotlin + Jetpack Compose + 硬件 DAC 采样级音频引擎构建，支持标准拍号细分、复合不对称拍分组、多节奏（Polyrhythm）以及任意拍四档自定义重音。

## 核心特性

### 1. 采样级节拍精度（±0.02ms，长期零漂移）
- **硬件原生采样率**：通过 `AudioTrack.getNativeOutputSampleRate` 匹配设备 DAC 原生采样率（通常 48 kHz），避免系统重采样引入抖动。
- **乘法纪元零漂移算法**：所有拍点帧号由单调小节号乘法直接锚定，在 `123 BPM`、`7/8`、`4:3` 等非整除节奏下连续运行上千拍，累计漂移严格 **≤ 1 个采样（≤ 0.021ms）**。
- **单采样精确混音 + 零 GC 热路径**：在低延迟 `AudioTrack` 流中按精确子缓冲偏移混音，运行于 `THREAD_PRIORITY_URGENT_AUDIO` 实时线程，热切换 BPM 无跳相、热修改重音零相位扰动。
- **硬件音画同步**：基于 `AudioTimestamp` / `playbackHeadPosition` 在声音真正输出扬声器/耳机的瞬间高亮拍位。

### 2. 多样自定义节拍类型
- **标准拍号 + 细分**：自由拍号 `1~16` 拍、分母 `2/4/8/16`，内置四分、八分、三连音、十六分、附点八分、十六分切分、爵士摇摆（Swing），支持细分音强度调节。
- **复合 / 加法拍子分组**：内置 `5/8 (3+2 / 2+3)`、`6/8 (3+3)`、`7/8 (3+2+2 / 2+2+3 / 2+3+2)`、`8/8 (3+3+2)`、`9/8`、`11/8` 等，支持可视化自由增删分组（总拍数上限 16）。
- **多节奏（Polyrhythm）**：支持任意 `2..12` 对 `2..12` 双声部同周期对齐（含 `3:2`、`4:3`、`5:4`、`7:4` 快捷组合），主/副声部可独立设置音色与每拍重音。

### 3. 自由强调任意拍 & 练琴工具
- **四档重音编排**：点击任意拍柱循环切换 **强拍（Accent）→ 次强拍（Medium）→ 普通拍（Normal）→ 静音休止（Mute）**，提供“默认重音 / 反拍 2&4 / 单数强拍 / 全部均等”快捷切换。
- **4 套内置程序化音色**：木鱼、机械节拍器、电子滴答、军鼓边击，支持点击试听。
- **Tap Tempo 点击测速** & **本地预设库**（9 套内置经典预设 + 无限用户自定义预设持久化）。
- **Android 16 原生规范适配**：`compileSdk = 36`、`targetSdk = 36`、`minSdk = 26`，支持强制 Edge-to-Edge、预测性返回手势、大屏/横竖屏双栏自适应及 16KB 内存页对齐。

## 目录结构

```text
app/src/main/java/com/metronome/app/
├── MainActivity.kt                  # Edge-to-Edge 入口与屏幕常亮控制
├── model/                           # 节拍配置模型与内置预设
├── engine/
│   ├── MeasureTimeline.kt           # 节拍时间轴编排器（纯 Kotlin）
│   ├── SoundSynthesizer.kt          # 48kHz 程序化音色合成器
│   ├── SampleAccurateRenderer.kt    # 采样级零漂移渲染核心（零 GC）
│   ├── MetronomeAudioEngine.kt      # AudioTrack 低延迟驱动与音画同步
│   └── TapTempoCalculator.kt        # 点击测速算法
├── data/PresetRepository.kt         # SharedPreferences + JSON 持久化
└── ui/                              # Jetpack Compose Material 3 界面与 ViewModel
```

## 构建与测试

环境要求：JDK 17、Android SDK Platform 36（Android 16）。

```bash
# 运行全部单元测试（含千拍级 PCM 波形精度测试）
./gradlew testDebugUnitTest

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（已开启 R8 代码与资源压缩）
./gradlew assembleRelease
```

### 自定义 Release 签名（可选）
在项目根目录创建 `keystore.properties`（已被 `.gitignore` 忽略，不会提交到仓库）：

```properties
storeFile=release.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

也可通过环境变量 `KEYSTORE_FILE`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` 在 CI/CD 中传入正式签名。

## License

[MIT](LICENSE)
