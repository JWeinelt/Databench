package de.julianweinelt.datacat.flow.setup;

import com.google.gson.Gson;
import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SetupServer {
    private Javalin app;
    private File setupFile;

    public SetupServer() {

    }

    public void start() {
        app = Javalin.create(config -> {
            config.startup.showJavalinBanner = false;
            config.startup.startupWatcherEnabled = false;

            config.routes.get("/api/hostnames", ctx -> {
                ctx.result(new Gson().toJson(getAvailableHostNames()));
            });
        });
    }


    private List<String> getAvailableHostNames() {
        List<String> availableHostNames = new ArrayList<>();
        try {
            InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (InetAddress address : addresses) {
                availableHostNames.add(address.getHostAddress());
            }
        } catch (Exception e) {
            log.error("Failed to get local host names: {}", e.getMessage());
        }
        availableHostNames.add("0.0.0.0");
        availableHostNames.add("localhost");
        return availableHostNames;
    }
}
