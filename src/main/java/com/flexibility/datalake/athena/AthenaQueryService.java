package com.flexibility.datalake.athena;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.paginators.GetQueryResultsIterable;

/**
 * Runs SQL queries against the S3 data lake through Amazon Athena, using the tables
 * registered in the Glue Data Catalog as metastore, and streams the results page by page.
 */
public class AthenaQueryService implements AutoCloseable {

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    private final AthenaClient athenaClient;

    public AthenaQueryService(Region region) {
        this.athenaClient = AthenaClient.builder()
                .region(region)
                .build();
    }

    public String submitQuery(String query, String database, String outputLocation) {
        StartQueryExecutionRequest request = StartQueryExecutionRequest.builder()
                .queryString(query)
                .queryExecutionContext(QueryExecutionContext.builder().database(database).build())
                .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
                .build();

        return athenaClient.startQueryExecution(request).queryExecutionId();
    }

    /**
     * Polls the query execution status until it reaches a terminal state.
     */
    public void waitForCompletion(String queryExecutionId) throws InterruptedException {
        while (true) {
            GetQueryExecutionResponse response = athenaClient.getQueryExecution(
                    GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build());

            QueryExecutionState state = response.queryExecution().status().state();
            switch (state) {
                case SUCCEEDED -> {
                    return;
                }
                case FAILED, CANCELLED -> throw new IllegalStateException(
                        "La consulta Athena termino en estado " + state + ": "
                                + response.queryExecution().status().stateChangeReason());
                default -> Thread.sleep(POLL_INTERVAL.toMillis());
            }
        }
    }

    /**
     * Retrieves and prints the query results page by page via the SDK paginator,
     * so large result sets are never fully loaded into memory at once.
     */
    public void printResultsPaginated(String queryExecutionId, int pageSize) {
        GetQueryResultsRequest request = GetQueryResultsRequest.builder()
                .queryExecutionId(queryExecutionId)
                .maxResults(pageSize)
                .build();

        GetQueryResultsIterable pages = athenaClient.getQueryResultsPaginator(request);

        int pageNumber = 0;
        for (GetQueryResultsResponse page : pages) {
            pageNumber++;
            List<Row> rows = page.resultSet().rows();
            // Athena repeats the column names as the first data row of the very first page.
            int startIndex = (pageNumber == 1 && !rows.isEmpty()) ? 1 : 0;

            System.out.printf("--- Athena: pagina %d (%d filas) ---%n", pageNumber, rows.size() - startIndex);
            for (int i = startIndex; i < rows.size(); i++) {
                System.out.println("  " + formatRow(rows.get(i)));
            }
        }
    }

    private String formatRow(Row row) {
        return row.data().stream()
                .map(datum -> datum.varCharValue() == null ? "" : datum.varCharValue())
                .collect(Collectors.joining(" | "));
    }

    @Override
    public void close() {
        athenaClient.close();
    }
}
