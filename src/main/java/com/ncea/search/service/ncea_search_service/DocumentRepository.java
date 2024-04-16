package com.ncea.search.service.ncea_search_service;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocumentRepository extends ElasticsearchRepository<Document, String> {
}
