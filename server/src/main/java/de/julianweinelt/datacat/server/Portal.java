package de.julianweinelt.datacat.server;

import de.julianweinelt.datacat.server.server.StoreServer;

public class Portal {
    public static void main(String[] args) {
        new StoreServer().start();
    }
}