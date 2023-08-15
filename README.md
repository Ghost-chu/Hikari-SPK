# Hikari-SPK
由 Java 语言编写的轻量 Synology 套件中心服务器，[SSPKS](https://github.com/jdel/sspks)的替代品。

## 简介

Hikari-SPK 旨在为了解决 [SSPKS](https://github.com/jdel/sspks) 在大量 SPK 包下运行缓慢，且消耗大量 CPU 和内存的问题。  
您只需要编辑 `application.yml` 配置基本设置，然后其余的和 [SSPKS](https://github.com/jdel/sspks) 的使用方式一样，将 SPK 包扔进 `packages` 文件夹，Hikari-SPK 将会为它们自动生成元数据。

## 安装

1. 运行 Hikari-SPK，等待生成 `application.yml` 配置文件后关闭
2. 编辑 `application.yml` 中的端口和 `base-url` 等选项，保存配置文件
3. 启动 Hikari-SPK
4. 将 SPK 扔进 `packages` 文件夹
5. 准备就绪！

## 添加至套件中心

在 Synology 套件中中心添加以下位置的套件源

```
https://<base-url>/spk
```

例如：
当 `base-url` 被设置为 `https://myspk.com:7777` 时，你应该输入 `https://myspk.com:7777/spk`

## 效果示例

![QQ图片20230422202417](https://user-images.githubusercontent.com/30802565/233784596-130146f1-52ea-46da-bca2-2d48288e5770.jpg)

## 配置文件

[查看带有注释的默认配置文件](https://github.com/Ghost-chu/Hikari-SPK/blob/master/src/main/resources/application.yml)
