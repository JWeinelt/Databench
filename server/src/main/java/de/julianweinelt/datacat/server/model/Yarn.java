package de.julianweinelt.datacat.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Yarn {
    private final UUID uniqueID;
    private final Account creator;

    private String slug;
    private String displayName;
    private String shortDescription;
    private String longDescription;
    private List<YarnTag> tags = new ArrayList<>();
    private List<YarnGalleryImage> images = new ArrayList<>();


    public Yarn(UUID uniqueID, Account creator) {
        this.uniqueID = uniqueID;
        this.creator = creator;
    }
}