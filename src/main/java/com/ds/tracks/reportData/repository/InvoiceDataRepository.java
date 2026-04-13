package com.ds.tracks.reportData.repository;

import com.ds.tracks.reportData.model.InvoiceData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceDataRepository extends MongoRepository<InvoiceData, String> {
    InvoiceData findFirstById(String id);

    Page<InvoiceData> findAllByWorkspaceId(String workspaceId, PageRequest of);
}
