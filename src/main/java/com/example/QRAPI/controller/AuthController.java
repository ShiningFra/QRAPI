package com.example.QRAPI.controller;

import com.example.QRAPI.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    
    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/reserve")
    public ResponseEntity<String> reserveToken(@RequestParam String fournisseur) {
        String token = jwtUtil.generateTokenForProvider(fournisseur);
	System.out.println("Token généré : " + token + "\n");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(token, headers, HttpStatus.OK);
    }
}
