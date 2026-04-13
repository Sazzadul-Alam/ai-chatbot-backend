package com.ds.tracks.isage;

import com.ds.tracks.isage.model.ChatConversation;
import com.ds.tracks.isage.model.ChatRequest;
import com.ds.tracks.isage.model.CountryCode;
import com.ds.tracks.isage.repository.ChatConversationRepository;
import com.ds.tracks.isage.repository.ChatRequestRepository;
import com.ds.tracks.isage.repository.CountryCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IsageServiceImpl implements IsageService {
    private final CountryCodeRepository countryCodeRepository;
    private final ChatRequestRepository chatRequestRepository;
    private final ChatConversationRepository chatConversationRepository;

    @Override
    public ResponseEntity<?> getCountryCode() {
        try {
            List<CountryCode> countries = countryCodeRepository.findAllByOrderByCountryAsc();
            if (countries.isEmpty()) {
                return new ResponseEntity<>("No country codes found", HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(countries, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching country codes: {}", e.getMessage());
            return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> saveRequest(Map<String, Object> request) {
        try {
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setRequest(request);
            chatRequestRepository.save(chatRequest);
            return new ResponseEntity<>("Saved", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to save request: {}", e.getMessage());
            return new ResponseEntity<>("Failed to save", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> saveConv(Map<String, Object> data, String loginId) {
        try {
            // find existing record for this user or create new
            ChatConversation conv = chatConversationRepository
                    .findByLoginId(loginId)
                    .orElse(new ChatConversation());

            conv.setData(data);
            conv.setLoginId(loginId);
            conv.setUpdatedAt(new Date());

            // set createdAt only for new records
            if (conv.getId() == null) {
                conv.setCreatedAt(new Date());
            }

            chatConversationRepository.save(conv);
            return new ResponseEntity<>("Saved", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to save conversation: {}", e.getMessage());
            return new ResponseEntity<>("Failed to save", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Override
    public ResponseEntity<?> getConv(String loginId) {
        try {
            return chatConversationRepository.findByLoginId(loginId)
                    .map(conv -> new ResponseEntity<Object>(conv, HttpStatus.OK))
                    .orElse(new ResponseEntity<>("No conversations found", HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            log.error("Failed to get conversation: {}", e.getMessage());
            return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
