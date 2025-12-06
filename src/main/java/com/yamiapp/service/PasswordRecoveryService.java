package com.yamiapp.service;

import com.yamiapp.exception.BadRequestException;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.NotFoundException;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.UserDTO;
import com.yamiapp.util.MailHelper;
import com.yamiapp.util.RedisHelper;
import jakarta.mail.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordRecoveryService {
    private RedisHelper redisHelper;
    private UserService userService;
    private MailHelper mailHelper;

    @Value("${yami.recoverymail.timeout}")
    private Long mailTimeout;

    @Value("${yami.frontend.url}")
    private String frontendUrl;

    public PasswordRecoveryService(JedisPool jedisPool, Session mailSession, UserService userService) {
        this.redisHelper = new RedisHelper(jedisPool);
        this.mailHelper = new MailHelper(mailSession);
        this.userService = userService;
    }

    public void requestRecovery(String email) {
        // check user
        Optional<User> optU = userService.getRawByEmail(email);
        if (optU.isEmpty()) {
            throw new NotFoundException(ErrorStrings.INVALID_USER_EMAIL.getMessage());
        }
        User u = optU.get();

        // now, create redis token and send email
        String token = UUID.randomUUID().toString();

        redisHelper.executeVoid(j -> j.setex(token, mailTimeout, u.getEmail()));
        String mailMessage = composeRecoveryMail(token, u);

        mailHelper.sendMail(email, "Yami - Pedido de recuperação de senha", composeRecoveryMail(token, u));
    }
    private String composeRecoveryMail(String token, User u) {
        String url = frontendUrl + "/account/recovery?token=" + token + "&username=" + u.getUsername();

        return """
            Olá %s,
            
            Recebemos uma solicitação para redefinir a senha da sua conta no Yami.
            
            Para continuar, clique no link abaixo:
            %s
            
            Se você não fez essa solicitação, apenas ignore este e-mail; sua conta continuará segura.
            Este link é válido por uma hora.
            
            Atenciosamente,
            Equipe Yami
        """.formatted(u.getUsername(), url);
    }

    public void resetPassword(String token, String newPassword) {
        String email = redisHelper.execute(j -> j.get(token));
        if (email == null) {
            throw new BadRequestException(ErrorStrings.INVALID_TOKEN.getMessage());
        }

        Optional<User> optU = userService.getRawByEmail(email);
        if (optU.isEmpty()) {
            throw new BadRequestException(ErrorStrings.INVALID_TOKEN.getMessage()); // this shouldn't happen
        }

        UserDTO dto = new UserDTO().withPassword(newPassword);

        userService.updateRawUser(optU.get(), dto);
    }

}
