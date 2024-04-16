package com.ncea.search.service.ncea_search_service;
import org.springframework.data.annotation.Id;

@org.springframework.data.elasticsearch.annotations.Document(indexName = "documents")
public class Document {

    @Id
    private String id;
    private String content;

}
