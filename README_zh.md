# XIcons

[English](README.md) | [中文](README_zh.md)

XIcons 是一个强大的 Java 图标库集合，提供了多个流行的图标库的 Java 实现，支持 Swing 和 JavaFX 两种 UI 框架。

## ✨ 特性

- 🎨 **多图标库支持**：包含 Fluent、Antd、Carbon、Font Awesome、Ionicons、Material Design、Tabler 等流行图标库
- 🖥️ **双框架支持**：同时支持 Swing 和 JavaFX 应用程序

## 📚 支持的图标库

### Fluent Icons
- Regular Icons
- Filled Icons

### Ant Design Icons
- Filled Icons
- Outlined Icons
- Twotone Icons

### Carbon Icons
- Default Icons
- Filled Icons
- Round Icons

### Font Awesome Icons
- Default Icons
- Regular Icons

### Ionicons
- Ionicons 4 Default Icons
- Ionicons 5 Default Icons
- Ionicons 5 Sharp Icons

### Material Design Icons
- Filled Icons
- Outlined Icons
- Round Icons
- Sharp Icons
- Twotone Icons

### Tabler Icons
- Default Icons
- Filled Icons
- Sharp Icons

## 🖼️ 效果

![swing](https://s2.loli.net/2025/08/14/9RU1BfotKdzZbNS.png)
![jfx](https://s2.loli.net/2025/08/14/NCuav8jVyzsGoLl.png)

## 📦 安装

需要使用对应图标的classifier

- `all` - 全部图标
- `antd` - Ant Design
- `carbon` - Carbon
- `fa` - Font Awesome Icons
- `fluent` - Fluent Icons
- `ionicons4` - Ionicons 4
- `ionicons5` - Ionicons 5
- `material` - Material Design Icons
- `tabler` - Tabler Icons
### Maven

在您的 `pom.xml` 文件中添加以下依赖：

#### Swing 版本
```xml
<dependency>
    <groupId>cn.enaium.xicons</groupId>
    <artifactId>xicons-swing</artifactId>
    <version>1.0.0</version>
    <classifier>fluent</classifier>
</dependency>
```

#### JavaFX 版本
```xml
<dependency>
    <groupId>cn.enaium.xicons</groupId>
    <artifactId>xicons-jfx</artifactId>
    <version>1.0.0</version>
    <classifier>fluent</classifier>
</dependency>
```

### Gradle

在您的 `build.gradle` 文件中添加以下依赖：

#### Swing 版本
```gradle
implementation 'cn.enaium.xicons:xicons-swing:1.0.0:fluent'
```

#### JavaFX 版本
```gradle
implementation 'cn.enaium.xicons:xicons-jfx:1.0.0:fluent'
```

或者在 Kotlin DSL (`build.gradle.kts`) 中：

#### Swing 版本
```kotlin
implementation("cn.enaium.xicons:xicons-swing:1.0.0:fluent")
```

#### JavaFX 版本
```kotlin
implementation("cn.enaium.xicons:xicons-jfx:1.0.0:fluent")
```

## 💻 使用方法

```java
import cn.enaium.xicons.swing.icons.FluentIcons;
import javax.swing.*;

// 使用 Regular 图标
JLabel label = new JLabel(FluentIcons.Regular.AnimalDog);

// 使用 Filled 图标
JButton button = new JButton(FluentIcons.Filled.AnimalDog);
```

### 在 JavaFX 应用中使用

```java
import cn.enaium.xicons.jfx.icons.FluentIcons;
import javafx.scene.control.Label;

// 使用 Regular 图标
Label label = new Label("Dog", FluentIcons.Regular.AnimalDog);

// 使用 Filled 图标
Button button = new Button("Dog", FluentIcons.Filled.AnimalDog);
```

## 🙏 致谢

感谢以下开源项目提供的图标资源：

- [Fluent Icons](https://github.com/microsoft/fluentui-system-icons)
- [Ant Design Icons](https://github.com/ant-design/ant-design-icons)
- [Carbon Icons](https://github.com/carbon-design-system/carbon-icons)
- [Font Awesome](https://fontawesome.com/)
- [Ionicons](https://ionic.io/ionicons)
- [Material Design Icons](https://material.io/icons)
- [Tabler Icons](https://tabler-icons.io/)

---

⭐ 如果这个项目对你有帮助，请给它一个星标！
