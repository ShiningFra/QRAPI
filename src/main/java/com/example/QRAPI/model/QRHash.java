package com.example.QRAPI.model; 

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.util.UUID;

@Table("qr_hash")
public class QRHash {
    @PrimaryKey
    private UUID id;

    private String hash;
    private UUID qr_data_id;  // Clé étrangère vers QRData

    // Getters et setters (ne pas modifier les noms des méthodes)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    
    public UUID getQrDataId() { return qr_data_id; }
    public void setQrDataId(UUID qr_data_id) { this.qr_data_id = qr_data_id; }
}
