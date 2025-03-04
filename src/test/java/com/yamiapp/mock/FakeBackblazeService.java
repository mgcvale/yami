package com.yamiapp.mock;

import com.backblaze.b2.client.structures.*;
import com.yamiapp.service.BackblazeService;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FakeBackblazeService extends BackblazeService {
    private final Map<String, byte[]> files = new HashMap<>();

    @Override
    public B2FileVersion uploadFile(MultipartFile file, String fileName) throws IOException {
        files.put(fileName, file.getBytes());
        return new B2FileVersion(fileName, fileName, file.getSize(), "", "", "", new HashMap<>(), "", new Date().getTime(), null, null, null, "");
    }

    @Override
    public ByteArrayResource downloadFile(String filePath) {
        return new ByteArrayResource(files.get(filePath));
    }

    @Override
    public void deleteFile(@NotNull String filePath, @NotNull String fileName) {
        files.remove(fileName);
    }
}
