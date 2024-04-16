package com.ncea.search.service.ncea_search_service;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SpanQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class DocumentService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final List<String> tabooWords = new ArrayList<>(Arrays.asList("the", "an", "a", "and", "or"));

    //map of fields with respective boost factor
    private final Map<String, Float> fieldsToSearch = Map.of(
            "resourceTitleObject.default.keyword", 1.0f,
            "resourceAltTitleObject.default.keyword", 0.5f
    );

    private final int proximityScore = 0;

    public List<Document> searchDocuments(String query) {
        CopyOnWriteArrayList<Document> results = new CopyOnWriteArrayList<>();

        // Submit tasks to the executor service for parallel processing
        for (int i = 0; i < 10; i++) {
            executorService.submit(() -> {
                // Create SpanTermQueryBuilders for each field in fieldsToSearch
                List<SpanQueryBuilder> spanTermQueryBuilders = fieldsToSearch.keySet().stream()
                        .map(field -> QueryBuilders.spanTermQuery(field, query).boost(fieldsToSearch.get(field)))
                        .collect(Collectors.toList());


                // Create a SpanNearQueryBuilder and add SpanTermQueryBuilders to it
                SpanQueryBuilder spanQueryBuilder = null;
                if (!spanTermQueryBuilders.isEmpty()) {
                    spanQueryBuilder = spanTermQueryBuilders.get(0);
                    for (int j = 1; j < spanTermQueryBuilders.size(); j++) {
                        SpanQueryBuilder nextSpanQueryBuilder = spanTermQueryBuilders.get(j);
                        spanQueryBuilder = QueryBuilders.spanNearQuery(spanQueryBuilder, proximityScore)
                                .addClause(nextSpanQueryBuilder)
                                .inOrder(true);
                    }
                }

                // Create a BoolQueryBuilder to exclude documents containing taboo words
                BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                        .must(spanQueryBuilder);

                // Add mustNot clauses to exclude documents containing taboo words
                for (String tabooWord : tabooWords) {
                    for (String field : fieldsToSearch.keySet()) {
                        boolQueryBuilder.mustNot(QueryBuilders.matchQuery(field, tabooWord));
                    }
                }

                Criteria criteria = new Criteria(fieldsToSearch.keySet().iterator().next()).is(boolQueryBuilder);
                CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);

                SearchHits<Document> searchHits = elasticsearchOperations.search(criteriaQuery, Document.class);
                List<Document> partialResults = searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
                results.addAll(partialResults);
            });
        }
        // Wait for all tasks to complete
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            // Wait
        }

        return results;
    }

}
