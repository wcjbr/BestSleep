# BestSleep 文档

## 概述

BestSleep 是一个 Paper 插件，用于在夜晚发起集体睡觉请求。当达到原版 `playersSleepingPercentage` 入睡比例后，只会把已经同意的玩家传送回各自的床，并尝试强制进入睡眠。随后时间会持续推进到白天，并且睡觉人数越多，推进速度越快。

这个插件适合 Java 版和基岩版混合服务器：

- Java 玩家使用 Paper `Dialog`
- 基岩版玩家使用 Floodgate / Geyser 表单

## 功能

- 只能在夜晚发起请求
- 兼容原版 `playersSleepingPercentage`
- 不要求全员同意
- 只传送并处理已同意的玩家
- 传送前和入睡前都会检查床位占用状态
- 管理睡眠过程中会临时记录床位占用
- 支持每个玩家的请求冷却时间
- 使用 Title 持续显示当前 MC 时间
- 睡觉人数越多，推进到白天越快
- 内置 `zh_cn` 和 `en_us` 国际化文本

## 命令

- `/bestsleep`
  发起睡觉请求
- `/bestsleep request`
  发起睡觉请求
- `/bestsleep accept`
  同意当前请求
- `/bestsleep deny`
  拒绝当前请求
- `/bestsleep cancel`
  如果你是发起者，则取消当前请求

别名：

- `/sleepvote`
- `/groupsleep`

## 运行要求

- Paper `1.21.x`
- Java `21+`
- 如果需要识别基岩版玩家，安装 Floodgate
- 如果需要基岩版接入，安装 Geyser

## 安装

1. 构建插件：

```bash
mvn clean package
```

2. 把 `target/` 下生成的 jar 放进 Paper 服务器的 `plugins/` 目录。

3. 启动一次服务器，让插件生成数据目录。

4. 如果需要基岩版支持，再额外安装：

- Geyser-Spigot
- floodgate-spigot

## 本地开发

仓库内置了本地运行脚本：

```bash
./run.sh
```

它会自动执行：

- 用 Maven 构建插件
- 复制 jar 到 `run/plugins/`
- 清理该插件的 Paper remap 缓存
- 如果 Geyser 配置已存在，则把认证模式改为 offline
- 启动 `run/` 目录下的 Paper 服务端

## 配置

主配置文件：

```yml
default-locale: zh_cn
request-cooldown-seconds: 60
```

### `default-locale`

当玩家客户端语言没有匹配到已有语言包时，使用这个语言作为回退语言。

当前内置：

- `zh_cn`
- `en_us`

### `request-cooldown-seconds`

每个玩家发起新睡觉请求的冷却时间。

### 语言包

`messages` 节点里保存了界面文本和玩家提示文本。

如果你想增加更多语言，可以按下面已有结构扩展：

- `messages.zh_cn`
- `messages.en_us`

## 睡眠流程

1. 玩家只能在夜晚发起睡觉请求。
2. 只有同一世界内的在线玩家会被纳入参与者。
3. 所有参与者都必须有可用床位。
4. Java 玩家收到 Paper 对话框。
5. 基岩版玩家收到 Floodgate 表单。
6. 当同意人数达到原版入睡比例后，请求进入推进阶段。
7. 只有已同意的玩家会被传送回自己的床。
8. 在强制睡眠前会再次检查床是否被占用。
9. 世界时间会持续推进到白天。
10. 到白天后，请求才真正结束。

## 发布与分发

仓库已包含 GitHub Actions 工作流，用于：

- CI 构建
- 根据 tag 自动创建 GitHub Release
- 自动生成 Release Notes
- 自动上传 jar 到 GitHub Release
- 自动发布到 Modrinth

发布要求：

- `pom.xml` 中的版本必须是正式版本，不能是 `-SNAPSHOT`
- tag 必须和版本一致，例如 `v1.0.0`

相关文件：

- `.github/workflows/ci.yml`
- `.github/workflows/release.yml`
- `.github/MODRINTH.md`
