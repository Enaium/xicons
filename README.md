# XIcons

[English](README.md) | [中文](README_zh.md)

XIcons is a powerful Java icon library collection that provides Java implementations of multiple popular icon libraries, supporting both Swing and JavaFX UI frameworks.

## ✨ Features

- 🎨 **Multiple Icon Library Support**: Includes popular icon libraries such as Fluent, Antd, Carbon, Font Awesome, Ionicons, Material Design, Tabler, and more
- 🖥️ **Dual Framework Support**: Supports both Swing and JavaFX applications

## 📚 Supported Icon Libraries

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

## 🖼️ Preview

![swing](https://s2.loli.net/2025/08/14/9RU1BfotKdzZbNS.png)
![jfx](https://s2.loli.net/2025/08/14/NCuav8jVyzsGoLl.png)

## 📦 Installation

You need to use the corresponding icon classifier:

- `all` - All icon
- `antd` - Ant Design Icons
- `carbon` - Carbon Icons
- `fa` - Font Awesome Icons
- `fluent` - Fluent Icons
- `ionicons4` - Ionicons 4
- `ionicons5` - Ionicons 5
- `material` - Material Design Icons
- `tabler` - Tabler Icons

### Maven

Add the following dependencies to your `pom.xml` file:

#### Swing Version
```xml
<dependency>
    <groupId>cn.enaium.xicons</groupId>
    <artifactId>xicons-swing</artifactId>
    <version>1.0.0</version>
    <classifier>fluent</classifier>
</dependency>
```

#### JavaFX Version
```xml
<dependency>
    <groupId>cn.enaium.xicons</groupId>
    <artifactId>xicons-jfx</artifactId>
    <version>1.0.0</version>
    <classifier>fluent</classifier>
</dependency>
```

### Gradle

Add the following dependencies to your `build.gradle` file:

#### Swing Version
```gradle
implementation 'cn.enaium.xicons:xicons-swing:1.0.0:fluent'
```

#### JavaFX Version
```gradle
implementation 'cn.enaium.xicons:xicons-jfx:1.0.0:fluent'
```

Or in Kotlin DSL (`build.gradle.kts`):

#### Swing Version
```kotlin
implementation("cn.enaium.xicons:xicons-swing:1.0.0:fluent")
```

#### JavaFX Version
```kotlin
implementation("cn.enaium.xicons:xicons-jfx:1.0.0:fluent")
```

## 💻 Usage

### In Swing Applications

```java
import cn.enaium.xicons.swing.icons.FluentIcons;
import javax.swing.*;

// Using Regular icons
JLabel label = new JLabel(FluentIcons.Regular.AnimalDog);

// Using Filled icons
JButton button = new JButton(FluentIcons.Filled.AnimalDog);
```

### In JavaFX Applications

```java
import cn.enaium.xicons.jfx.icons.FluentIcons;
import javafx.scene.control.Label;

// Using Regular icons
Label label = new Label("Dog", FluentIcons.Regular.AnimalDog);

// Using Filled icons
Button button = new Button("Dog", FluentIcons.Filled.AnimalDog);
```

## 🙏 Acknowledgments

Thanks to the following open-source projects for providing icon resources:

- [Fluent Icons](https://github.com/microsoft/fluentui-system-icons)
- [Ant Design Icons](https://github.com/ant-design/ant-design-icons)
- [Carbon Icons](https://github.com/carbon-design-system/carbon-icons)
- [Font Awesome](https://fontawesome.com/)
- [Ionicons](https://ionic.io/ionicons)
- [Material Design Icons](https://material.io/icons)
- [Tabler Icons](https://tabler-icons.io/)

---

⭐ If this project helps you, please give it a star!
