package com.example.QRAPI.controller;

import com.example.QRAPI.service.ScanService;
import com.example.QRAPI.model.QRData;
import com.example.QRAPI.model.QRHash;
import com.example.QRAPI.model.History;
import com.example.QRAPI.repository.QRDataRepository;
import com.example.QRAPI.repository.QRHashRepository;
import com.example.QRAPI.repository.HistoryRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.spec.SecretKeySpec;
import java.security.Principal;
import java.io.ByteArrayOutputStream;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
@RequestMapping("/api/qr")
public class QRCodeController {

    private final QRDataRepository qrDataRepository;
    private final QRHashRepository qrHashRepository;
    private final HistoryRepository historyRepository;
    private final ScanService scanService;

    public QRCodeController(QRDataRepository qrDataRepository, QRHashRepository qrHashRepository, HistoryRepository historyRepository, ScanService scanService) {
        this.qrDataRepository = qrDataRepository;
        this.qrHashRepository = qrHashRepository;
        this.historyRepository = historyRepository;
        this.scanService = scanService;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateQRCode(@RequestBody QRData qrData, @RequestParam String secret, @RequestParam long expirationMillis, Principal fournisseur) {
  System.out.println("Utilisateur connecté : " + (fournisseur != null ? fournisseur.getName() : "NULL"));
try {
            qrData.setId(UUID.randomUUID());
            qrData.setFournisseur(fournisseur.getName());
            qrDataRepository.save(qrData);

            String rawData = qrData.toString();
            String hashedData = hashData(rawData);

            QRHash qrHash = new QRHash();
	    qrHash.setId(UUID.randomUUID());
	    qrHash.setHash(hashedData);
	    qrHash.setQrDataId(qrData.getId());
            qrHashRepository.save(qrHash);

            String signedData = signData(hashedData, secret, expirationMillis);

            byte[] qrCodeImage = generateQRCodeImageFromData(signedData);
System.out.println("Succès de génération \n");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.IMAGE_PNG);

            return new ResponseEntity<>(qrCodeImage, headers, HttpStatus.OK);
        } catch (Exception e) {
System.out.println("Echec de génération \n" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/scan")
    public ResponseEntity<String> scanQRCode(@RequestParam String qrCodeData, @RequestParam String secret, @RequestBody History history, Principal fournisseur) {
String decodedData = verifySignature(qrCodeData, secret);
if (decodedData == null) {
System.out.println("Invalide \n");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("QR Code invalide !");
}        

	try {
            QRHash result = scanService.processScan(decodedData);
	    if(result != null){
            QRData data = qrDataRepository.findById(result.getQrDataId()).get();
            history.setClientId(data.getClientId());
            history.setChauffeurId(data.getChauffeurId());
            history.setCourseId(data.getCourseId());
            history.setFournisseur(data.getFournisseur());
            historyRepository.save(history);
            System.out.println("Le fournisseur " + fournisseur.getName() + " a effectué le scan des données : " + history + "\n");
            return ResponseEntity.ok("Scan réussi");
	    }else{
	    System.out.println("Le fournisseur " + fournisseur.getName() + " a échoué un scan \n"); 
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("QR Code non trouvé.");
        }} catch (Exception e) {
    	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur : " + e.getMessage());
        }
    }

    private String hashData(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : encodedHash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private String signData(String data, String secret, long expirationMillis) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);
        
        return Jwts.builder()
                .setSubject(data)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String verifySignature(String token, String secret) {
    try {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    } catch (JwtException e) {
        return null;
    }
}


    private byte[] generateQRCodeImageFromData(String qrData) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hintMap = new HashMap<>();
        hintMap.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 350, 350, hintMap);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        }
    }
}
