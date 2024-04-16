package com.ncea.search.service.ncea_search_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.elasticsearch.annotations.Document;
import java.util.List;

@RestController
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @GetMapping("/search")
    public List<Document> search(@RequestParam String query) {
        return documentService.searchDocuments(query);
    }

}
