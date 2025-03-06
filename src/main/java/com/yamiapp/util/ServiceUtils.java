package com.yamiapp.util;

import com.yamiapp.exception.BadRequestException;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.ForbiddenException;
import com.yamiapp.model.Role;
import com.yamiapp.model.User;
import com.yamiapp.service.UserService;
import org.apache.http.entity.ContentType;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ServiceUtils {

    public static MultipartFile convertToJPEG(MultipartFile multipartFile) throws IOException {
        byte[] fileBytes = multipartFile.getBytes();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileBytes);
        BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);

        if (bufferedImage == null) {
            System.out.println("Buffered image is NULL");
            throw new BadRequestException(ErrorStrings.INVALID_IMAGE_FILETYPE.getMessage());
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "JPEG", byteArrayOutputStream);
        return  new ByteArrayMultipartFile(byteArrayOutputStream.toByteArray(), multipartFile.getName(), multipartFile.getOriginalFilename(), ContentType.IMAGE_JPEG.getMimeType());
    }


    public static void validateUser(UserService userService, String userToken) {
        User u = userService.getByToken(userToken);
        if (u.getRole().ordinal() <= Role.PRO_USER.ordinal()) {
            throw new ForbiddenException(ErrorStrings.FORBIDDEN_NOT_ADMIN.getMessage());
        }
    }

}
