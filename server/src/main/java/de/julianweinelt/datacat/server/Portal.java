package de.julianweinelt.datacat.server;

import de.julianweinelt.datacat.server.server.StoreServer;

import javax.sound.sampled.Port;

public class Portal {
    private static Portal instance;
    public static Portal portal() {
        return instance;
    }

    public static void main(String[] args) {
    }

    public Portal() {
        instance = this;
    }

    private StoreServer storeServer;

    private void start() {

        storeServer = new StoreServer();
        storeServer.start();
    }



    public static StoreServer storeServer() {
        return Portal.portal().storeServer;
    }
}