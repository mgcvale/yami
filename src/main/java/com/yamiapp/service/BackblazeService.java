package com.yamiapp.service;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2DeleteFileVersionRequest;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
@Service
public class BackblazeService {

    @Value("${backblaze.b2.application-key-id}")
    private String applicationKeyId;
    @Value("${backblaze.b2.application-key}")
    private String applicationKey;
    @Value("${backblaze.b2.bucket-name}")
    private String bucketName;


    private B2StorageClient client;
    private String bucketId;

    @PostConstruct
    public void initialize() {
        System.out.println(applicationKey);
        System.out.println(applicationKeyId);

        client = B2StorageClientFactory.createDefaultFactory().create(
                applicationKeyId,
                applicationKey,
                "FoodApp/1.0"
        );
        try {
            bucketId = client.getBucketOrNullByName(bucketName).getBucketId();
        } catch (B2Exception e) {
            e.printStackTrace();
            System.exit(-1); // The server shouldn't keep running without backblaze
        }
    }

    @PreDestroy
    public void cleanup() {
        if (client != null) {
            client.close();
        }
    }


    public B2FileVersion uploadFile(MultipartFile file, String b2FileName) throws B2Exception, IOException {
        File tempFile = Files.createTempFile("b2-upload-", ".tmp").toFile();
        try {
            file.transferTo(tempFile);
            B2ContentSource contentSource = B2FileContentSource.build(tempFile);

            B2FileVersion fileVersion = client.uploadSmallFile(
                    B2UploadFileRequest.builder(
                            bucketId,
                            b2FileName,
                            file.getContentType(),
                            contentSource
                    ).build()
            );

            return fileVersion;
        } finally {
            Files.deleteIfExists(tempFile.toPath());
        }
    }


    public ByteArrayResource downloadFile(String fileName) throws B2Exception {
        B2ContentMemoryWriter handler = B2ContentMemoryWriter.build();
        client.downloadByName(
                B2DownloadByNameRequest.builder(bucketName, fileName).build(),
                handler
        );

        return new ByteArrayResource(handler.getBytes()) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }

    public void deleteFile(@NotNull String filename, @NotNull String fileId) throws B2Exception {
        client.deleteFileVersion(filename, fileId);
    }

    public MediaType getContentType(String fileName, String fileID) throws B2Exception {
        B2FileVersion fileInfo = client.getFileInfoByName(bucketName, fileName);
        return MediaType.parseMediaType(fileInfo.getContentType());
    }


}
