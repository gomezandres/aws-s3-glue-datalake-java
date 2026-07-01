package com.flexibility.datalake.config;

import software.amazon.awssdk.regions.Region;

/**
 * Configuration read from environment variables. No credentials are handled here:
 * the AWS SDK default credentials provider chain (env vars, shared config, instance/task role, etc.)
 * is used by the clients instead.
 */
public final class DataLakeConfig {

    private final Region region;
    private final String glueDatabase;
    private final String glueTable;
    private final String athenaOutputLocation;
    private final String athenaQuery;
    private final int pageSize;

    private DataLakeConfig(Region region, String glueDatabase, String glueTable,
                            String athenaOutputLocation, String athenaQuery, int pageSize) {
        this.region = region;
        this.glueDatabase = glueDatabase;
        this.glueTable = glueTable;
        this.athenaOutputLocation = athenaOutputLocation;
        this.athenaQuery = athenaQuery;
        this.pageSize = pageSize;
    }

    public static DataLakeConfig fromEnvironment() {
        String regionName = env("AWS_REGION", "us-east-1");
        String database = requireEnv("GLUE_DATABASE");
        String table = requireEnv("GLUE_TABLE");
        String outputLocation = requireEnv("ATHENA_OUTPUT_LOCATION");
        String query = env("ATHENA_QUERY", "SELECT * FROM " + table);
        int pageSize = Integer.parseInt(env("PAGE_SIZE", "50"));

        return new DataLakeConfig(Region.of(regionName), database, table, outputLocation, query, pageSize);
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno requerida: " + name);
        }
        return value;
    }

    public Region getRegion() {
        return region;
    }

    public String getGlueDatabase() {
        return glueDatabase;
    }

    public String getGlueTable() {
        return glueTable;
    }

    public String getAthenaOutputLocation() {
        return athenaOutputLocation;
    }

    public String getAthenaQuery() {
        return athenaQuery;
    }

    public int getPageSize() {
        return pageSize;
    }
}
