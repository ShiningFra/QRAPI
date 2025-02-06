/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.QRAPI.controller;

/**
 *
 * @author Roddier
 */

import com.example.QRAPI.service.QRCodeService;
import com.example.QRAPI.service.ScanService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr")
public class QRCodeController {

    private final QRCodeService qrCodeService;
    private final ScanService scanService;

    public QRCodeController(QRCodeService qrCodeService, ScanService scanService) {
        this.qrCodeService = qrCodeService;
        this.scanService = scanService;
    }

    // Générer un code QR et le retourner sous forme d'image
    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateQRCode(
            @RequestParam Long clientId,
            @RequestParam Long chauffeurId,
            @RequestParam Long courseId) {
        try {
            byte[] qrCodeImage = qrCodeService.generateQRCodeImage(clientId, chauffeurId, courseId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.IMAGE_PNG);

            return new ResponseEntity<>(qrCodeImage, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Scanner un QR Code et valider
    @GetMapping("/scan")
    public ResponseEntity<String> scanQRCode(@RequestParam String qrCodeData) {
        try {
            String result = scanService.processScan(qrCodeData);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur de confirmation", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/view")
    public ResponseEntity<byte[]> View(@RequestParam String qrCodeData) {
        try {
            byte[] qrCodeImage = generateQRCodeImageFromData(qrCodeData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.IMAGE_PNG);

            return new ResponseEntity<>(qrCodeImage, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/hello")
    public String SayHello() {
        return "Hello : Your Project is running ❣";
    }
    
    private byte[] generateQRCodeImageFromData(String qrData) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hintMap = new HashMap<>();
        hintMap.put(EncodeHintType.MARGIN, 1); // Réduire les marges du QR code

        BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 350, 350, hintMap);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        }
    }
}
