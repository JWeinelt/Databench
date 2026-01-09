# 💾 DataBench Server
This module contains the global server of DataBench. It's key features are:
- Managing DataBench versions
- Hosting update files for the launcher
- Providing API endpoints for future features
- Serving as a backend for cloud features

## Building the Server
To build the server, you need to have Maven installed. Navigate to the server directory and run the following command:
```bash
mvn clean package
```
This will generate a JAR file in the `target` directory.

> [!IMPORTANT]
> The server is intended to be run on the official DataBench server infrastructure.
> It should only be deployed and managed by the DataBench development team.
> You can use it, but you won't get any updates for your self-hosted version.