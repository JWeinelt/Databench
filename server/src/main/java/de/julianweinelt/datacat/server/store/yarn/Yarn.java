package de.julianweinelt.datacat.server.store.yarn;

import de.julianweinelt.datacat.server.model.MAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class Yarn {
    private UUID uniqueID;

    private String name;
    private String slug;
    private MAccount author;
    private String shortDescription;
    private String longDescription;
    private final List<String> tags = new ArrayList<>();
    private final List<String> features = new ArrayList<>();

    private YarnStatus status;


    private String wikiLink;
    private String sourceLink;

    private int downloads;
}
