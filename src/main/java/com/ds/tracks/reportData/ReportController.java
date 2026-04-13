package com.ds.tracks.reportData;

import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.reportData.model.InvoiceData;
import com.ds.tracks.reportData.model.dto.InvoiceDto;
import com.ds.tracks.reportData.model.dto.ReportDto;
import com.ds.tracks.reportData.service.ReportDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {
    private final ReportDataService reportDataService;

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody ReportDto requestParam){
         return reportDataService.generate(requestParam);
    }

    @PostMapping("/generate-report")
    public ResponseEntity<?> generateReport(@RequestBody ReportDto requestParam, HttpServletResponse response){
         return reportDataService.generateReport( requestParam, response);
    }

    @GetMapping("/configurations")
    public ResponseEntity<?> getConfigurations(){
         return  reportDataService.getConfigurations();
    }

    // Invoice --------

    @PostMapping("/create-invoice")
    public ResponseEntity<?> createInvoice(@RequestBody InvoiceDto invoiceData, HttpServletResponse response, HttpServletRequest request){
        return  reportDataService.generateInvoice(invoiceData, response, request);
    }
    @PostMapping("/delete-invoice")
    public ResponseEntity<?> deleteInvoice(@RequestParam String id){
        return  reportDataService.deleteInvoice(id);
    }
    @PostMapping("/invoice-list")
    public ResponseEntity<?> getInvoices(@RequestParam String id,
                                         @RequestParam(required = false) String sortBy,
                                         @RequestParam(required = false) String sortOrder,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer size){
        return  reportDataService.getInvoices(id, sortBy, sortOrder, page, size);
    }
}
