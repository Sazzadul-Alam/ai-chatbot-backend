package com.ds.tracks.reportData.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class InvoiceDto {
    private String id;
    private String workspaceId;
    private String invoiceType;
    private Date invoiceDate;
    private String signedBy;
    private String client;
    private String totalInWords;
    private Double total;
    private Double vatPercentage;
    private Boolean isVatApplicable;
    private List<InvoiceRows> rows;
}
