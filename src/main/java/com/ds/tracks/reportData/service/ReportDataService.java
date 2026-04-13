package com.ds.tracks.reportData.service;

import com.ds.tracks.reportData.model.dto.InvoiceDto;
import com.ds.tracks.reportData.model.dto.ReportDto;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ReportDataService {

    ResponseEntity<?> getConfigurations();

    ResponseEntity<?> generateReport( ReportDto requestParam, HttpServletResponse response);

    ResponseEntity<?> generateInvoice(InvoiceDto invoiceData, HttpServletResponse response, HttpServletRequest request);

    ResponseEntity<?> generate(ReportDto requestParam);

    ResponseEntity<?> getInvoices(String workspaceId, String sortBy, String sortOrder, Integer page, Integer size);

    ResponseEntity<?> deleteInvoice(String id);

}