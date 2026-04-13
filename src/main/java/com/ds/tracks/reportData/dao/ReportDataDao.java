package com.ds.tracks.reportData.dao;

import com.ds.tracks.effort.model.EffortLog;
import com.ds.tracks.reportData.model.ReportData;
import com.ds.tracks.reportData.model.dto.ReportDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

public interface ReportDataDao {

    Object getInvoices(String workspaceId, String sortBy, Integer sortOrder, Integer page, Integer size);


    List<Document> clientwiseTasksReport(ReportDto requestParam, String clientType);

    List<Document> invoiceReport(ReportDto requestParam);

    List<Document> personalTaxReport(ReportDto requestParam);
}
