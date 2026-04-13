package com.ds.tracks.isage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/isage")
@RequiredArgsConstructor
public class IsageController {
    private final IsageService isageService;

    @GetMapping("/country-code")
    public ResponseEntity<?> getCountryCode(){
        return  isageService.getCountryCode();
    }
    @PostMapping("/save-request")
    public ResponseEntity<?> saveRequest(@RequestBody Map<String, Object> request) {
        return isageService.saveRequest(request);
    }
    @PostMapping("/save-conv")
    public ResponseEntity<?> saveConv(@RequestBody Map<String, Object> convStore,
                                      Principal principal   ) {
        if (principal == null) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        return isageService.saveConv(convStore, principal.getName());
    }
    @GetMapping("/get-conv")
    public ResponseEntity<?> getConv(Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        return isageService.getConv(principal.getName());
    }
}
