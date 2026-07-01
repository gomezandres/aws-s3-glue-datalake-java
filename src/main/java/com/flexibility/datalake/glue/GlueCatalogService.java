package com.flexibility.datalake.glue;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.GetTablesRequest;
import software.amazon.awssdk.services.glue.model.Table;
import software.amazon.awssdk.services.glue.paginators.GetTablesIterable;

/**
 * Reads table metadata from the AWS Glue Data Catalog for a given database, paginating
 * through results instead of loading everything into memory at once.
 */
public class GlueCatalogService implements AutoCloseable {

    private final GlueClient glueClient;

    public GlueCatalogService(Region region) {
        this.glueClient = GlueClient.builder()
                .region(region)
                .build();
    }

    /**
     * Prints, page by page, every table registered under the given database in the Glue Catalog.
     */
    public void listTables(String databaseName, int pageSize) {
        GetTablesRequest request = GetTablesRequest.builder()
                .databaseName(databaseName)
                .maxResults(pageSize)
                .build();

        GetTablesIterable pages = glueClient.getTablesPaginator(request);

        int pageNumber = 0;
        for (var page : pages) {
            pageNumber++;
            System.out.printf("--- Glue Catalog: database '%s', pagina %d (%d tablas) ---%n",
                    databaseName, pageNumber, page.tableList().size());
            for (Table table : page.tableList()) {
                System.out.printf("  - %s (columnas: %d)%n", table.name(), columnCount(table));
            }
        }
    }

    private int columnCount(Table table) {
        if (table.storageDescriptor() == null) {
            return 0;
        }
        return table.storageDescriptor().columns().size();
    }

    @Override
    public void close() {
        glueClient.close();
    }
}
