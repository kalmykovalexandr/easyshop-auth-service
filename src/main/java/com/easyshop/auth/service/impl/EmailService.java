package com.easyshop.auth.service.impl;

import com.easyshop.auth.service.EmailServiceInt;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.Locale;


@Slf4j
@Service
public class EmailService implements EmailServiceInt {

    @Value("${easyshop.mail.from-email}") String fromEmail;
    @Value("${easyshop.mail.from-name}") String fromName;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final Locale locale = LocaleContextHolder.getLocale();

    public EmailService(JavaMailSender mailSender,
                        TemplateEngine templateEngine,
                        MessageSource messageSource) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
    }

    @Override
    public void sendVerificationEmail(String email, String otpCode) {
        try {
            String subject = messageSource.getMessage("email.verification.subject", null, locale);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context(locale);
            context.setVariable("verificationCode", otpCode);
            String content = templateEngine.process("email/otp-verification", context);

            helper.setFrom(fromEmail, fromName);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);

        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send verification email to: {}", email, e);
        }
    }
}
