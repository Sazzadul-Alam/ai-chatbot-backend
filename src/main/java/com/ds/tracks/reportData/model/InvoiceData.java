package com.ds.tracks.reportData.model;


import com.ds.tracks.reportData.model.dto.InvoiceRows;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Document("invoice_data")
public class InvoiceData {
    @Id
    private String id;
    private String workspaceId;
    private String spaceId;
    private String subspaceId;
    private String clientType;

    private String invoiceNo;
    private String invoiceType;
    private Date invoiceDate;

    private String clientName;
    private String clientAddress;
    private String clientPhone;

    private String totalInWords;
    private Double total;
    private Double subtotal;
    private Double vatAmount;
    private Double vatPercentage;


    private String vatRegNo;
    private String corpAddr;
    private String corpBranch;
    private String bankName;
    private String bankBranch;
    private String bankAcc;
    private String bankAccName;
    private String bankSwiftCode;
    private String bankIBAN;
    private String bkashAcc;

    private String signedBy;
    private String signedByDesg;
    private Integer invoiceSl;
    private Boolean isVatApplicable;


    private List<InvoiceRows> rows;

    private String createdBy;
    private String currency;
    private Date createdAt;


}
