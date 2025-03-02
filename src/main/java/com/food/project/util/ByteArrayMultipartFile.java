package com.food.project.util;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteArrayMultipartFile implements MultipartFile {
    private final byte[] byteArray;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    public ByteArrayMultipartFile(byte[] byteArray, String name, String originalFilename, String contentType) {
        this.byteArray = byteArray;
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return byteArray.length == 0;
    }

    @Override
    public long getSize() {
        return byteArray.length;
    }

    @NotNull
    @Override
    public byte[] getBytes() throws IOException {
        return byteArray;
    }

    @NotNull
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(byteArray);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        java.nio.file.Files.write(dest.toPath(), byteArray);
    }
}
