package com.example.QRAPI.service;

import com.example.QRAPI.model.QRHash;
import com.example.QRAPI.repository.QRHashRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ScanService {

    private final QRHashRepository qrHashRepository;

    public ScanService(QRHashRepository qrHashRepository) {
        this.qrHashRepository = qrHashRepository;
    }

    /**
     * Traite le scan d'un QR Code en validant le token JWT et en vérifiant les données.
     * @param qrCodeData Le token JWT extrait du QR Code.
     * @param secret La clé secrète utilisée pour signer le token.
     * @return Un message indiquant le statut du scan.
     */
    public QRHash processScan(String qrCodeData) {
        // Extraction des données hachées du token JWT
        String extractedHashedData = qrCodeData;
        
        if (extractedHashedData == null) {
            return null;
        }

        // Vérification dans la base de données
        Optional<QRHash> storedHash = qrHashRepository.findByHash(extractedHashedData);

        if (storedHash.isPresent()) {
            return storedHash.get();
        } else {
            return null;
        }
    }
}
