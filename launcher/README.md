# 💾 DataBench Launcher
This module contains the launcher of DataBench. It's key features are:
- Installing updates
- Running the main application
- Pre-Configuring DataBench before first start

## Building the Launcher
To build the launcher, you need to have Maven installed. Navigate to the launcher directory and run the following command:
```bash
mvn clean package
```
This will generate a JAR file in the `target` directory.

> [!IMPORTANT]
> The launcher won't work stand-alone. It requires the main DataBench application to be present in the same directory.
> It should only be installed using the official installer or package manager.