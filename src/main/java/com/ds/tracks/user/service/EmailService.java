package com.ds.tracks.user.service;

import java.util.List;

public interface EmailService {
    void sendEmail(String emailAddress, String subject, String body);
    void sendEmail(List<String> emailAddress, String subject, String body);
}
