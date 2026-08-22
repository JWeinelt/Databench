package de.julianweinelt.datacat.server.model;

import java.util.UUID;

public record YarnGalleryImage(UUID uniqueId, String alt, String type, boolean featured) {
}
