package de.julianweinelt.databench.dbx.util;

public record DatabaseType(String name, String jdbcLink, String driverClass, String vendor) {}