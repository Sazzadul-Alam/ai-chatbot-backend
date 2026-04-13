package com.ds.tracks.user.serviceImpl;

import com.ds.tracks.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.CharEncoding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;

import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Session.getInstance;
import static javax.mail.Transport.send;
import static javax.mail.internet.InternetAddress.parse;

@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    String username;
    @Value("${spring.mail.password}")
    String password;
    @Value("${spring.mail.host}")
    String host;
    @Value("${spring.mail.port}")
    String port;

    @Override
    @Async
    public void sendEmail(String emailAddress, String subject, String body) {
        try {
            MimeMessage mail = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, CharEncoding.UTF_8);
            helper.setFrom(username);
            helper.setSubject(subject);
            helper.setTo(emailAddress);
            helper.setText(body);
            javaMailSender.send(mail);
        } catch (Exception e) {
            log.error("Error => ", e);
        }
    }

    @Async
    @Override
    public void sendEmail(List<String> emailList, String subject, String body) {

        for(String emailAddress : emailList){
            try {
                MimeMessage mail = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mail, true, CharEncoding.UTF_8);
                helper.setFrom(username);
                helper.setSubject(subject);
                helper.setTo(emailAddress);
                helper.setText("Dear Concern," + "\n\n" + body
                        + "\n\n\nThanks & Regards, \nSystem Auto generated email.");
                javaMailSender.send(mail);
            } catch (Exception e) {
                log.error("Error => ", e);
            }
        }

    }
    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        return getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }
}
