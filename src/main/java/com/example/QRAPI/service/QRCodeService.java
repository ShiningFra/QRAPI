package com.example.QRAPI.service;

import com.example.QRAPI.model.Course;
import com.example.QRAPI.model.Chauffeur;
import com.example.QRAPI.model.Client;
import com.example.QRAPI.model.QRCode;
import com.example.QRAPI.repository.ClientRepository;
import com.example.QRAPI.repository.ChauffeurRepository;
import com.example.QRAPI.repository.CourseRepository;
import com.example.QRAPI.repository.QRCodeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class QRCodeService {

    private final ChauffeurRepository chauffeurRepository;
    private final ClientRepository clientRepository;
    private final CourseRepository courseRepository;
    private final QRCodeRepository qrCodeRepository;

    private static final int QR_CODE_WIDTH = 350;
    private static final int QR_CODE_HEIGHT = 350;

    public QRCodeService(ChauffeurRepository chauffeurRepository, ClientRepository clientRepository,
                         CourseRepository courseRepository, QRCodeRepository qrCodeRepository) {
        this.chauffeurRepository = chauffeurRepository;
        this.clientRepository = clientRepository;
        this.courseRepository = courseRepository;
        this.qrCodeRepository = qrCodeRepository;
    }

    public byte[] generateQRCodeImage(Long clientId, Long chauffeurId, Long courseId)
            throws WriterException, IOException {
        // Récupérer les entités et vérifier leur existence
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable avec l'ID : " + clientId));
        Chauffeur chauffeur = chauffeurRepository.findById(chauffeurId)
                .orElseThrow(() -> new IllegalArgumentException("Chauffeur introuvable avec l'ID : " + chauffeurId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course introuvable avec l'ID : " + courseId));

        // Construire les données du QR code
        String qrData = buildQRData(client, chauffeur, course);

        // Enregistrer le QR code dans la base de données
        QRCode qrCode = new QRCode();
        qrCode.setChauffeurId(chauffeurId);
        qrCode.setClientId(clientId);
        qrCode.setCourse(courseRepository.findById(courseId).get());
        qrCode.setContent(qrData);
        qrCodeRepository.save(qrCode);

        // Générer l'image du QR code
        return generateQRCodeImageFromData(qrData);
    }

    private String buildQRData(Client client, Chauffeur chauffeur, Course course) {
        return new StringBuilder()
                .append(client.getId()).append(":").append(chauffeur.getId()).append(":").append(course.getId()).append(":")
                .append(client.getNom()).append(":").append(client.getPrenom()).append(":").append(client.getPhone()).append(":")
                .append(chauffeur.getNom()).append(":").append(chauffeur.getPrenom()).append(":").append(chauffeur.getPhone()).append(":")
                .append(course.getDepart()).append(":").append(course.getArrivee()).append(":").append(course.getDateDepart()).append(":")
                .append(course.getPrix())
                .toString();
    }

    private byte[] generateQRCodeImageFromData(String qrData) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hintMap = new HashMap<>();
        hintMap.put(EncodeHintType.MARGIN, 1); // Réduire les marges du QR code

        BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT, hintMap);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        }
    }
}
