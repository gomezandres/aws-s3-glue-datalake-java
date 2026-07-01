package com.flexibility;

import com.flexibility.datalake.athena.AthenaQueryService;
import com.flexibility.datalake.config.DataLakeConfig;
import com.flexibility.datalake.glue.GlueCatalogService;

/**
 * Demo: connects to an AWS Glue Data Catalog backed data lake, lists the tables of a
 * database and runs a paginated Athena query against it.
 *
 * Required environment variables:
 *   GLUE_DATABASE          - Glue Data Catalog database name
 *   GLUE_TABLE             - table to query
 *   ATHENA_OUTPUT_LOCATION - s3://bucket/prefix/ where Athena writes query results
 * Optional:
 *   AWS_REGION   (default: us-east-1)
 *   ATHENA_QUERY (default: SELECT * FROM <GLUE_TABLE>)
 *   PAGE_SIZE    (default: 50)
 */
public class App {

    public static void main(String[] args) {
        DataLakeConfig config = DataLakeConfig.fromEnvironment();

        try (GlueCatalogService glueCatalogService = new GlueCatalogService(config.getRegion())) {
            glueCatalogService.listTables(config.getGlueDatabase(), config.getPageSize());
        }

        try (AthenaQueryService athenaQueryService = new AthenaQueryService(config.getRegion())) {
            String queryExecutionId = athenaQueryService.submitQuery(
                    config.getAthenaQuery(), config.getGlueDatabase(), config.getAthenaOutputLocation());

            System.out.println("Athena query execution id: " + queryExecutionId);

            athenaQueryService.waitForCompletion(queryExecutionId);
            athenaQueryService.printResultsPaginated(queryExecutionId, config.getPageSize());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido esperando el resultado de la consulta Athena", e);
        }
    }
}
