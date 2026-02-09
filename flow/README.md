# 💾 DataBench Flow
This module contains the flow component of DataBench. 
With the DataBench Flow installed on your database server machine, you can get features not directly
supported by your database, such as an SQL Agent for MySQL or MariaDB servers.

With the DataBench Flow, you can schedule SQL jobs to run at specific times or intervals,
automating routine database tasks such as backups, maintenance, and data processing.

## ⚙️ Installing on your server
> [!NOTE]
> You may also install the DataBench Flow on another machine than your database server,
> but you have to make sure that the Flow can connect to your database server.
 
### 🔓 Requirements
- Currently, the DataBench Flow only supports MySQL and MariaDB databases.
- Java Runtime Environment (JRE) 17 or higher installed on the server machine.
- Network access to the database server from the machine where the DataBench Flow is installed.
- A database user with sufficient privileges to execute the scheduled SQL jobs.
- (Optional) Firewall rules allowing incoming connections to the DataBench Flow on the specified port.

> [!TIP]
> You may open the port ``25295`` on your firewall to allow remote access to the DataBench Flow.
> This is required if you want to manage the sql agent jobs from DataBench.

### 📫 Installation Steps
1. Download the latest release of the DataBench Flow from the [GitHub Releases](https://github.com/JWeinelt/DataBench/releases) page.
    - Make sure to choose the latest version of the DataBench Flow.
    - The Flow is platform-independent, so you can run it on any operating system that supports Java.
2. Put the downloaded JAR file on your server machine in a directory of your choice.
3. Open a terminal or command prompt on the server machine (if running a cli-only server, just navigate to the directory).
4. Create a new ``start.sh`` file (Linux/macOS) or ``start.bat`` file (Windows) in the same directory as the JAR file.
5. Add the following command to the ``start.sh`` or ``start.bat`` file:
    ```bash
      java -jar databench-Flow-<version>.jar
   ```
   Replace `<version>` with the actual version number of the downloaded JAR file.
6. Save the file and close the text editor.
7. Make the ``start.sh`` file executable (Linux/macOS) by running the following command in the terminal:
    ```bash
    chmod +x start.sh
    ```
8. Start the DataBench Flow by running the ``start.sh`` file (Linux/macOS) or ``start.bat`` file (Windows):
    - On Linux/macOS:
      ```bash
      ./start.sh
      ```
    - On Windows:
      ```cmd
      start.bat
      ```
9. Follow the on-screen instructions to complete the initial setup of the DataBench Flow or take a look at the [wiki](https://github.com/JWeinelt/DataBench/wiki/Flow-Setup) for more detailed setup instructions.


## 📤 Building from source
To build the DataBench Flow from source, you need to have Maven installed. Navigate to the Flow directory and run the following command:
```bash
mvn clean package
```
This will generate a JAR file in the `target` directory, which you can then use to run the DataBench Flow as described in the installation steps above.
