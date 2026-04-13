package com.ds.tracks.reportData.service;

import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.reportData.dao.ReportDataDao;
import com.ds.tracks.reportData.model.InvoiceData;
import com.ds.tracks.reportData.model.dto.InvoiceDto;
import com.ds.tracks.reportData.model.dto.InvoiceRows;
import com.ds.tracks.reportData.model.dto.ReportDto;
import com.ds.tracks.reportData.repository.InvoiceDataRepository;
import com.ds.tracks.space.model.Space;
import com.ds.tracks.space.repository.SpaceDao;
import com.ds.tracks.space.repository.SpaceRepository;
import com.ds.tracks.tasks.dao.TasksDao;
import com.ds.tracks.user.model.User;
import com.ds.tracks.user.service.UserService;
import com.ds.tracks.workspace.Workspace;
import com.ds.tracks.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import static com.ds.tracks.commons.utils.Utils.isValidString;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportDataServiceImpl implements ReportDataService {
    private final TasksDao tasksDao;
    private final SpaceDao spaceDao;
    private final ReportDataDao reportDataDao;
    private final UserService userService;
    private final InvoiceDataRepository invoiceDataRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SpaceRepository spaceRepository;
    private final AuditLogService auditLogService;


    @Value("${report.baseDir}")
    private String baseDir;

    @Value("${report.invoice.file}")
    private String invoiceFilePath;
    @Value("${report.invoice.file-v2}")
    private String invoiceFilePathV2;

    @Value("${report.invoice.images}")
    private String invoiceImagePath;


    @Override
    public ResponseEntity<?> generate(ReportDto requestParam) {
        initializeParams(requestParam);
        return new ResponseEntity<>(tasksDao.reportPaged(requestParam), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getInvoices(String workspaceId, String sortBy, String sortOrder, Integer page, Integer size) {
        auditLogService.save("Viewed Invoice List");
        return new ResponseEntity<>(reportDataDao.getInvoices(workspaceId, isValidString(sortBy)? sortBy : "_id", Objects.equals(sortOrder, "desc")? -1 : 1, Objects.isNull(page)? 0 : page, Objects.isNull(size)? 100 : size), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> deleteInvoice(String id) {
        invoiceDataRepository.deleteById(id);
        return new ResponseEntity<>("Invoice Deleted", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> generateReport( ReportDto requestParam, HttpServletResponse response) {
        List<Document> list = null;
        ByteArrayOutputStream outputStream=null;
        InputStream inputStream = null;
        initializeParams(requestParam);
        String reportPath = "";
        DecimalFormat formatter = new DecimalFormat("#,###");
        switch (requestParam.getReportType().toLowerCase()){
            case "invoice":
                reportPath = "invoiceReport";
                list = reportDataDao.invoiceReport(requestParam);
                break;
            case "client-employee":
                reportPath = "clientReport";
                list = reportDataDao.clientwiseTasksReport(requestParam, "Normal");
                break;
            case "dubai-client-employee":
                reportPath = "clientReport";
                list = reportDataDao.clientwiseTasksReport(requestParam, "Dubai");
                break;
            case "personal-tax":
                reportPath = "personalTaxReport";
                list = reportDataDao.personalTaxReport(requestParam);
                break;
            default:
                reportPath = "tasksReport";
                list = tasksDao.report(requestParam);
                break;
        }
        if (Objects.isNull(list) || list.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        try {
            outputStream = new ByteArrayOutputStream();
//            inputStream = Files.newInputStream(Paths.get(filePath));
            inputStream = getClass().getResourceAsStream(baseDir + reportPath +  "/report.jasper");
            Map<String, Object> TitleData = new HashMap<>();
            TitleData.put("startDate", requestParam.getStartDateString());
            TitleData.put("endDate", requestParam.getEndDateString());
            TitleData.put("imgPath", "classpath:"+ baseDir);
            TitleData.put("SUB_REPORT", "classpath:"+ baseDir + reportPath +  "/sub_report.jasper");
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    (JasperReport) JRLoader.loadObject(inputStream),
                    TitleData,
                    new JRBeanCollectionDataSource(list));
            if (Objects.equals(requestParam.getFileType(), "xlsx")) {
                JRXlsxExporter exporter = new JRXlsxExporter();
                SimpleXlsxReportConfiguration reportConfigXLS = new SimpleXlsxReportConfiguration();
                reportConfigXLS.setSheetNames(new String[]{"sheet1"});
                exporter.setConfiguration(reportConfigXLS);
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
                response.setHeader("Content-Disposition", "attachment;filename=Tracks_Report.xlsx");
                response.setContentType("application/octet-stream");
                exporter.exportReport();
            } else {
                JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
                write(response, outputStream);
            }
            auditLogService.save("Downloaded Report");
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (Objects.nonNull(inputStream)) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {}
            }
            if (Objects.nonNull(outputStream)) {
                try {
                    outputStream.close();
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public ResponseEntity<?> generateInvoice(InvoiceDto invoiceDto, HttpServletResponse response, HttpServletRequest request) {
        List<Document> list = new ArrayList<>();
        DecimalFormat formatter = new DecimalFormat("#,###");
        InvoiceData invoiceData = new InvoiceData();
        if(isValidString(invoiceDto.getId())){
            invoiceData = invoiceDataRepository.findFirstById(invoiceDto.getId());
            if(Objects.nonNull(invoiceData.getSpaceId())){
                Space space = spaceRepository.findFirstById(invoiceData.getSpaceId());
                if(Objects.nonNull(space)){
                    invoiceData.setClientName(space.getName());
                    invoiceData.setClientAddress(space.getAddress());
                    invoiceData.setClientPhone(space.getPhone());
                    invoiceData.setClientType(Objects.isNull(space.getClientType()) ? "Normal"  : space.getClientType());
                }
            }
            int sl = 1;
            for(InvoiceRows row : invoiceData.getRows()){
                if(Objects.nonNull(row.getAmount())){
                    list.add(new Document("particulars", isValidString(row.getParticulars()) ? row.getParticulars() : "")
                            .append("amount", formatter.format(row.getAmount()))
                            .append("sl",  String.valueOf(sl))
                    );

                    sl++;
                }
            }
        } else {
            Double grandTotal = 0D;
            List<InvoiceRows> deleteRows = new ArrayList<>();
            int sl = 1;
            for(InvoiceRows row : invoiceDto.getRows()){
                if(Objects.nonNull(row.getAmount())){
                    list.add(new Document("particulars", isValidString(row.getParticulars()) ? row.getParticulars() : "")
                            .append("amount", formatter.format(row.getAmount()))
                            .append("sl", String.valueOf(sl))
                    );
                    sl++;
                    grandTotal += row.getAmount();
                } else {
                    deleteRows.add(row);
                }
            }
            Workspace workspace = workspaceRepository.findFirstById(invoiceDto.getWorkspaceId());
            Space space = spaceRepository.findFirstById(invoiceDto.getClient());
            User user = userService.getUserNameAndDesignationById(invoiceDto.getSignedBy());


            invoiceData.setClientType(Objects.isNull(space.getClientType()) ? "Normal"  : space.getClientType());

            invoiceDto.getRows().removeAll(deleteRows);
            invoiceDto.setTotal(grandTotal);

            Integer invoiceSl = Objects.nonNull(workspace.getLastInvoiceNumber()) ? workspace.getLastInvoiceNumber() + 1 : 1;

            invoiceData.setWorkspaceId(invoiceDto.getWorkspaceId());
            invoiceData.setInvoiceNo(generatedId(invoiceDto.getInvoiceType(), space.getName(), space.getMnemonic(), invoiceSl));
            invoiceData.setInvoiceType(invoiceDto.getInvoiceType());
            invoiceData.setInvoiceDate(invoiceDto.getInvoiceDate());
            invoiceData.setInvoiceSl(invoiceSl);

            invoiceData.setCurrency(Objects.equals(invoiceData.getClientType(), "Dubai") ? "AED":"BDT");

            invoiceData.setClientName(space.getName());
            invoiceData.setSpaceId(invoiceDto.getClient());
            invoiceData.setClientAddress(space.getAddress());
            invoiceData.setClientPhone(space.getPhone());

            invoiceData.setSignedBy(user.getFullName());
            invoiceData.setSignedByDesg(user.getDesignation());

            invoiceData.setVatRegNo(workspace.getVatRegNo());
            invoiceData.setCorpAddr(workspace.getCorpAddr());

            setBankDetails(invoiceData, workspace);

            invoiceData.setSubtotal(grandTotal);
            invoiceData.setVatPercentage(Objects.nonNull(invoiceDto.getVatPercentage()) ? invoiceDto.getVatPercentage() : 0);
            invoiceData.setIsVatApplicable(Objects.equals(invoiceDto.getIsVatApplicable(), Boolean.TRUE));

            double vatPercent = invoiceData.getIsVatApplicable()? (invoiceData.getVatPercentage()/100) : 0;
            invoiceData.setVatAmount(invoiceData.getSubtotal() * vatPercent);
            invoiceData.setTotal(invoiceData.getSubtotal() + invoiceData.getVatAmount());
            invoiceData.setTotalInWords(invoiceDto.getTotalInWords());

            invoiceData.setRows(invoiceDto.getRows());
        }
        ByteArrayOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = new ByteArrayOutputStream();
            inputStream = getClass().getResourceAsStream(Objects.equals(invoiceData.getClientType() , "Dubai") ? invoiceFilePathV2:invoiceFilePath);
            Map<String, Object> params = new HashMap<>();
            params.put("invoiceNo", invoiceData.getInvoiceNo());
            params.put("invoiceDate", invoiceData.getInvoiceDate());
            params.put("name", invoiceData.getClientName());
            params.put("phone", invoiceData.getClientPhone());
            params.put("address", isValidString(invoiceData.getClientAddress()) ? invoiceData.getClientAddress() : "");
            params.put("inWords", isValidString(invoiceData.getTotalInWords()) ? "In Word : "+invoiceData.getTotalInWords(): "");
            params.put("total", formatter.format(invoiceData.getTotal()));
            params.put("subtotal", formatter.format(invoiceData.getSubtotal()));
            params.put("vat", "VAT("+(Objects.equals(invoiceData.getIsVatApplicable(), Boolean.TRUE)? invoiceData.getVatPercentage(): "0")+"%)");
            params.put("vatAmount", formatter.format(invoiceData.getVatAmount()));
            params.put("vatRegNo", invoiceData.getVatRegNo());
            params.put("bankAccount", getBankDetails(invoiceData));
            params.put("bkashAccount", "You may pay the fee using Bkash Merchant account \""+invoiceData.getBkashAcc()+"\"");
            params.put("corpAddress", invoiceData.getCorpAddr());
            params.put("signedBy", invoiceData.getSignedBy());
            params.put("signedByDesg", Objects.nonNull(invoiceData.getSignedByDesg()) ? invoiceData.getSignedByDesg() : "");
            params.put("imgPath", "classpath:"+invoiceImagePath);
            params.put("corpBranch", StringUtils.isEmpty(invoiceData.getCorpBranch())? "" : "Branch:\n"+invoiceData.getCorpBranch());
            params.put("branchAddress",
                    "Branch Office:\n" +
                    "Office no. 20, The Prism (19th Floor)\n" +
                    "Business Bay, Dubai, United Arab Emirates\n" +
                    "PO Box 52938, Phone: +971 (0) 4 549 8220");
            params.put("invoiceType", (Objects.isNull(invoiceData.getInvoiceType()) ? "" :getInvoiceTypeName(invoiceData.getInvoiceType().toUpperCase()))+" INVOICE");
            params.put("sealPath", Objects.equals(invoiceData.getClientType(), "Dubai")? "accfintax_seal_uae.png" : "accfintax_seal.png");
            params.put("currencyTypeHeader", "Amount ("+(Objects.isNull(invoiceData.getCurrency())?"BDT":invoiceData.getCurrency())+")");

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    (JasperReport) JRLoader.loadObject(inputStream),
                    params, new JRBeanCollectionDataSource(list)
            );
            JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
            response.setHeader("Content-Disposition", "attachment;filename=".concat(invoiceData.getInvoiceNo().replace("/","_")).concat(".pdf"));

            write(response, outputStream);
            invoiceData.setCreatedAt(new Date());
            invoiceData.setCreatedBy(userService.getCurrentUserId());
            if(!isValidString(invoiceDto.getId())){
                spaceDao.incrementWorkspaceInvoice(invoiceDto.getWorkspaceId());
                invoiceDataRepository.save(invoiceData);
                auditLogService.save( "Created Invoice For "+ invoiceData.getClientName(), CollectionName.invoice_data, invoiceData.getId(), invoiceData.getSpaceId(), null);
            }
            return new ResponseEntity<>(invoiceData, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e.getCause());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (Objects.nonNull(inputStream)) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {}
            }
            if (Objects.nonNull(outputStream)) {
                try {
                    outputStream.close();
                } catch (Exception ignored) {}
            }
        }
    }

    public String getInvoiceTypeName(String value) {
        if ("FIN".equals(value)) {
            return "Finance";
        } else if ("ACC".equals(value)) {
            return "Accounts";
        } else if ("TAX".equals(value)) {
            return "Tax";
        } else if ("BPO".equals(value)) {
            return "BPO";
        } else if ("OTH".equals(value)) {
            return "Others";
        } else {
            return ""; // return "Unknown" if the value doesn't match any case
        }
    }


    private void setBankDetails(InvoiceData invoiceData, Workspace workspace){
        if(Objects.equals(invoiceData.getClientType(), "Dubai")){
            invoiceData.setBankAcc(workspace.getBankAccUAE());
            invoiceData.setBankAccName(workspace.getBankAccNameUAE());
            invoiceData.setBankAcc(workspace.getBankNameUAE());
            invoiceData.setBankBranch(workspace.getBankBranchUAE());
            invoiceData.setBankIBAN(workspace.getBankIbanUAE());
            invoiceData.setBankSwiftCode(workspace.getBankSwiftCodeUAE());
        } else {
            invoiceData.setBankAcc(workspace.getBankAcc());
            invoiceData.setBkashAcc(workspace.getBkashAcc());
            invoiceData.setBankName(workspace.getBankName());
            invoiceData.setBankBranch(workspace.getBankBranch());
        }
    }

    private String getBankDetails(InvoiceData invoiceData){
        StringBuilder bankDetails = new StringBuilder();
        if(Objects.equals(invoiceData.getClientType(), "Dubai")){
                if(Objects.nonNull(invoiceData.getBankName())) bankDetails.append("Bank Name: ").append(invoiceData.getBankName()).append("\n");
                if(Objects.nonNull(invoiceData.getBankAccName())) bankDetails.append("Account Name: ").append(invoiceData.getBankAccName()).append("\n");
                if(Objects.nonNull(invoiceData.getBankAcc())) bankDetails.append("A/C No: ").append(invoiceData.getBankAcc()).append("\n");
                if(Objects.nonNull(invoiceData.getBankIBAN())) bankDetails.append("IBAN: ").append(invoiceData.getBankIBAN()).append("\n");
                if(Objects.nonNull(invoiceData.getBankSwiftCode())) bankDetails.append("Swift Code: ").append(invoiceData.getBankSwiftCode()).append("\n");
        } else {
            if(Objects.nonNull(invoiceData.getBankName()) && Objects.nonNull(invoiceData.getBankName())){
                bankDetails.append("Account No. ").append("\"").append(invoiceData.getBankAcc()).append("\"");
                bankDetails.append(", ").append(invoiceData.getBankName());
            }
            if(Objects.nonNull(invoiceData.getBankBranch())) bankDetails.append(", ").append(invoiceData.getBankBranch());
        }
        return bankDetails.toString();
    }

    private String generatedId(String type, String client, String clientCode, long invoiceNo) {
        return "AFT/"+type+"/"+clientCode+"/"+String.format("%06d", invoiceNo);
    }



    private void initializeParams(ReportDto requestParam) {
        if (Objects.isNull(requestParam.getStartDate())) {
            requestParam.setStartDate(new Date());
        }
        if (Objects.isNull(requestParam.getEndDate())) {
            requestParam.setEndDate(requestParam.getStartDate());
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        requestParam.setStartDateString(sdf.format(requestParam.getStartDate()));
        requestParam.setEndDateString(sdf.format(requestParam.getEndDate()));
    }

    @Override
    public ResponseEntity<?> getConfigurations() {
        return new ResponseEntity<>(spaceDao.getConfigs(), HttpStatus.OK);
    }

    public void write(HttpServletResponse response, ByteArrayOutputStream baos) {

        try {
            // Retrieve output stream
            ServletOutputStream outputStream = response.getOutputStream();
            // Write to output stream
            baos.writeTo(outputStream);
            // Flush the stream
            outputStream.flush();
            outputStream.close();
            baos.flush();
            baos.close();

        } catch (Exception e) {
//            logger.error("Unable to write report to the output stream");
            throw new RuntimeException(e);
        }
    }
}
