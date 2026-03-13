---
title: Installing on your computer
---
# Installing on your computer
## Requirements
### Windows
- Windows 10 or newer
- 2GB Memory (4GB recommended)
- Disk space of ~500MB
- A 64-bit operating system
- **Java 17 or newer** (when not using the default bundled installer)

### GNU/Linux
- glibc ≥ 2.28
- 2GB Memory
- Disk space of ~500MB
- Desktop environment with X11 or Wayland
- **Java 17 or newer** (when not using the default bundled installer)

### MacOS
- MacOS 11 (Big Sur) or newer
- 64-bit Intel or Apple Silicon (ARM64)
- 2GB Memory (4GB recommended)
- Disk space of ~500MB

## Available installing methods
You can either install DataCat using the installer with bundled JRE or the one without it.

:::tip
**JRE** means **J**ava **R**untime **E**nvironment. It is recommended to use the bundled install, as it always is the same
JRE only used for DataCat, making it easier to provide support.
:::

| Platform              | Installer    | Java required    |
|-----------------------|--------------|------------------|
| Windows               | .exe/.msi    | No               |
| MacOS                 | .dmg/.pkg    | No               |
| Linux                 | .deb/.rmp    | No               |
| Manual (Windows Only) | .exe/.tar.gz | Java 17 or newer |

## Tested platforms
Currently, DataCat is available for these operating systems:

| OS                   | Status |
|----------------------|--------|
| Windows 10           | ✅      |
| Windows 11           | ✅      |
| Windows 8            | ❗      |
| Windows 7/Vista      | ❗      |
| Windows XP and older | ❌      |
| Debian 12            | ✅      |
| Debian 13            | ✅      |
| Fedora               | ⚠️     |
| Ubuntu               | ⚠️     |
| MacOS 11+            | ✅      |
| Windows Server 2012+ | ⚠️     |

✅ = Tested and working\
⚠️ = May work but is not actively tested\
❗= May work, but no support is provided for it\
❌ = Not working

If you want to suggest and/or test DataCat on another operating system, feel free to write us an eMail to os@data-cat.de.

## How to install

:::tip
You can also install DataCat using winget on Windows and apt on GNU/Linux.