package com.ds.tracks.files;

import com.ds.tracks.audit.AuditLogService;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.user.service.UserService;
import com.jcraft.jsch.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ds.tracks.commons.utils.Utils.sourceToCollectionName;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilesServiceImpl implements FilesService{


    private final FileInfoRepository fileInfoRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;

    @Value("${sftp.host}")
    private String HOST;
    @Value("${sftp.username}")
    private String USERNAME;
    @Value("${sftp.password}")
    private String PASSWORD;
    @Value("${sftp.files_path}")
    private String FILEPATH;
    private final String DETAILS_FOLDER = "details";

    private final List<String> acceptedFormats = Arrays.asList(".JPG", ".JPEG", ".PNG", ".MP4", ".MPEG", ".MP3", ".WAV", ".PDF", ".DOC", ".DOCX", ".XLS", ".XLSX");
    @Override
    public ResponseEntity<?> upload(String workspaceId, String spaceId, String subspaceId, MultipartFile file, String source, String sourceId) {
        String fileName = file.getOriginalFilename();
        if(Objects.nonNull(fileName)){
            String extension = fileName.substring(fileName.lastIndexOf("."));
            if (acceptedFormats.contains(extension.toUpperCase())) {
                String category = getFileCategory(extension.toUpperCase());
                String uploadName = new SimpleDateFormat("ddMMyyyy").format(new Date()) + "-" + UUID.randomUUID().toString().toUpperCase() + "-" + fileName.replaceAll("\\s+", "-");

                String uploadPath = FILEPATH + File.separator + category;
                try {
                    createDirIfNotExists(FILEPATH);
                    createDirIfNotExists(uploadPath);
                    Files.write(Paths.get(uploadPath+ File.separator+ uploadName), file.getBytes());
                    FileInfo fileInfo = FileInfo.builder()
                            .source(source).sourceId(sourceId)
                            .workspaceId(workspaceId).spaceId(spaceId).subspaceId(subspaceId).folderId(null)
                            .filename(fileName).extension(extension).category(category)
                            .uploadedAt(new Date()).uploadedBy(userService.getCurrentUserId())
                            .savedFilename(uploadName).savedLocation(uploadPath).build();
                    fileInfoRepository.save(fileInfo);
                    auditLogService.save("Uploaded File with name "+fileInfo.getFilename(),  sourceToCollectionName(fileInfo.getSource()), fileInfo.getSourceId());
                    return new ResponseEntity<>("File Uploaded", HttpStatus.OK);
                } catch (Exception ignored) {}
            }
        }
        return new ResponseEntity<>("Invalid File Format", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> getAll(String sourceId, String source) {
        auditLogService.save("Viewed Attachment List",  sourceToCollectionName(source), sourceId);
        return new ResponseEntity<>(fileInfoRepository.findBySourceIdAndSource(sourceId, source), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> delete(String id) {
        FileInfo fileInfo = fileInfoRepository.findFirstById(id);
        if(Objects.nonNull(fileInfo)){
            if(Objects.equals(fileInfo.getUploadedBy(), userService.getCurrentUserId())){
                try{
                    String filePath = FILEPATH+File.separator+fileInfo.getCategory()+File.separator+fileInfo.getSavedFilename();
                    File file = new File(filePath);
                    FileUtils.forceDelete(file);
                    fileInfoRepository.delete(fileInfo);
                    auditLogService.save("Deleted File with name "+fileInfo.getFilename(), sourceToCollectionName(fileInfo.getSource()), fileInfo.getSourceId());
                    return new ResponseEntity<>("File deleted", HttpStatus.OK);
                } catch (Exception e){
                    log.error(e.getMessage(), e.getCause());
                    return new ResponseEntity<>("An Error Occurred", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                return new ResponseEntity<>("You do not have permission to delete this file", HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>("File not found", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> uploadSpaceDetails(String id, String type, MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if(Objects.nonNull(fileName)){
            String extension = fileName.substring(fileName.lastIndexOf("."));
            if (acceptedFormats.contains(extension.toUpperCase())) {
                try {
                    createDirIfNotExists(FILEPATH);
                    createDirIfNotExists(FILEPATH + File.separator + DETAILS_FOLDER);
                    String uploadPath = FILEPATH + File.separator + DETAILS_FOLDER + File.separator + id;
                    createDirIfNotExists(uploadPath);
                    String uploadName = new SimpleDateFormat("ddMMyyyy").format(new Date()) + "-" + UUID.randomUUID().toString().toUpperCase() + "-" + fileName.replaceAll("\\s+", "-");
                    Files.write(Paths.get(uploadPath+ File.separator+ uploadName), file.getBytes());
                    FileInfo fileInfo = FileInfo.builder()
                            .source("space")
                            .sourceId(type)
                            .spaceId(id)
                            .filename(fileName)
                            .extension(extension)
                            .category(DETAILS_FOLDER)
                            .uploadedAt(new Date())
                            .uploadedBy(userService.getCurrentUserId())
                            .savedFilename(uploadName)
                            .savedLocation(uploadPath)
                            .build();
                    fileInfoRepository.save(fileInfo);
                    auditLogService.save("Uploaded Client Attachment named "+fileInfo.getFilename(), CollectionName.spaces, fileInfo.getSpaceId(), fileInfo.getSpaceId(), null);
                    return new ResponseEntity<>("File Uploaded", HttpStatus.OK);
                } catch (Exception ignored) {}
            }
        }
        return new ResponseEntity<>("Invalid File Format", HttpStatus.BAD_REQUEST);


    }

    @Override
    public ResponseEntity<?> deleteSpaceDetails(String id) {
        FileInfo fileInfo = fileInfoRepository.findFirstById(id);
        if(Objects.nonNull(fileInfo)){
            try{
                String filePath = FILEPATH + File.separator + DETAILS_FOLDER + File.separator + fileInfo.getSpaceId() + File.separator + fileInfo.getSavedFilename();
                File file = new File(filePath);
                FileUtils.forceDelete(file);
                fileInfoRepository.deleteById(id);
                auditLogService.save("Deleted Client Attachment named "+fileInfo.getFilename(), CollectionName.spaces, fileInfo.getSpaceId(), fileInfo.getSpaceId(), null);
                return new ResponseEntity<>("File deleted", HttpStatus.OK);
            } catch (Exception e){
                log.error(e.getMessage(), e.getCause());
                return new ResponseEntity<>("An Error Occurred", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("File not found", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<?> download(String id, HttpServletResponse response) {
        FileInfo fileInfo = fileInfoRepository.findFirstById(id);
        try{
            auditLogService.save("Downloaded File with name "+fileInfo.getFilename(),  sourceToCollectionName(fileInfo.getSource()), fileInfo.getSourceId());
            sendFileToOutputStream(fileInfo.getFilename(), FILEPATH+File.separator+fileInfo.getCategory()+File.separator+fileInfo.getSavedFilename(), response);
            return new ResponseEntity<>(HttpStatus.OK);

        } catch (Exception e){
            log.error(e.getMessage(), e.getCause());
            return new ResponseEntity<>("File not found", HttpStatus.BAD_REQUEST);
        }
    }
    @Override
    public void downloadSpaceDetails(String id, HttpServletResponse response) {
        FileInfo fileInfo = fileInfoRepository.findFirstById(id);
        try{
            auditLogService.save("Downloaded Client Attachment named "+fileInfo.getFilename(), CollectionName.spaces, fileInfo.getSpaceId(), fileInfo.getSpaceId(), null);
            sendFileToOutputStream(fileInfo.getFilename(), FILEPATH + File.separator + DETAILS_FOLDER + File.separator + fileInfo.getSpaceId() + File.separator + fileInfo.getSavedFilename(), response);
        } catch (Exception e){
            log.error(e.getMessage(), e.getCause());
        }
    }
    public void sendFileToOutputStream(String filename, String filePath, HttpServletResponse response) throws IOException {
        File file = new File(filePath);
        if(file.exists() && file.isFile()){
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLength(Math.toIntExact(file.length()));
            inputToOutputStream(new FileInputStream(filePath),response.getOutputStream());
        } else {
            throw new IOException();
        }
    }

    public void inputToOutputStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        int read = -1;
        byte[] limit = new byte[1024];
        try {
            while( (read = inputStream.read(limit)) != -1) {
                outputStream.write(limit, 0, read);
            }
        }
        finally {
            inputStream.close();
            outputStream.close();
        }
    }
    private boolean uploadToSFTP(String filePath, String fileName, MultipartFile file)  {
        ChannelSftp channelSftp = setupJsch();
        InputStream inputStream = null;
        if(Objects.nonNull(channelSftp)){
            try {
                channelSftp.connect();
                channelSftp.cd(filePath);
                inputStream = new BufferedInputStream(file.getInputStream());
                channelSftp.put(inputStream, fileName);
                return true;
            } catch (Exception e) {
                log.error(e.getMessage(), e.getCause());
            } finally {
                try {
                    channelSftp.exit();
                    channelSftp.disconnect();
                    channelSftp.getSession().disconnect();
                } catch (JSchException ignore) {}
                if(Objects.nonNull(inputStream)){
                    try {
                        inputStream.close();
                    } catch (IOException ignore) {}
                }
            }
        }
        return false;
    }
    private String getFileCategory(String extension) {
        switch (extension) {
            case ".JPG":
            case ".JPEG":
            case ".PNG":
                return  "image";
            case ".MP4":
            case ".MPEG":
                return  "video";
            case ".MP3":
            case ".WAV":
                return  "audio";
            case ".PDF":
            case ".DOC":
            case ".DOCX":
            case ".XLS":
            case ".XLSX":
                return  "document";
            default:
                return  "uncategorized";
        }
    }

    private boolean isValidFile(String fileName) {
        return false;
    }

    public void writeFileToFtpServer(ChannelSftp channelSftp, String filePath, String fileName, MultipartFile file) throws IOException, JSchException {

    }


    public ChannelSftp setupJsch() {
        Session jschSession = null;
        try{
            JSch jsch = new JSch();
            Properties config = new Properties();
            jschSession = jsch.getSession(USERNAME, HOST);
            jschSession.setPassword(PASSWORD);
            config.put("StrictHostKeyChecking", "no");
            jschSession.setConfig(config);
            jschSession.connect();
            return (ChannelSftp) jschSession.openChannel("sftp");
        } catch (Exception e){
            if(Objects.nonNull(jschSession)){
                jschSession.disconnect();
            }
        }
        return null;
    }

    public void createDirIfNotExists(String dir) throws IOException {
        File file = new File(dir);
        if (!file.exists()) {
            Files.createDirectories(Paths.get(file.getPath()));
            log.info("Directory created successfully!");
        }
    }
}
