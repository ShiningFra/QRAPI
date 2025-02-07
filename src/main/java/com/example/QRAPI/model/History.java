package com.example.QRAPI.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.util.UUID;

@Table("history")
public class History {
    @PrimaryKey
    private UUID id;

    private Long clientId;
    private Long chauffeurId;
    private Long courseId;
    private String lieu;
    private String fournisseur;
    private String heure;
    private String date;
    private String ville;
    private String pays;

    // Getters et setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public Long getChauffeurId() { return chauffeurId; }
    public void setChauffeurId(Long chauffeurId) { this.chauffeurId = chauffeurId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public String getFournisseur() { return fournisseur; }
    public void setFournisseur(String fournisseur) { this.fournisseur = fournisseur; }
    public String getHeure() { return heure; }
    public void setHeure(String heure) { this.heure = heure; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    @Override
    public String toString() {
        return "{"
            + "\"clientId\":" + clientId + ","
            + "\"chauffeurId\":" + chauffeurId + ","
            + "\"courseId\":" + courseId + ","
            + "\"lieu\":\"" + lieu + "\","
            + "\"fournisseur\":\"" + fournisseur + "\","
            + "\"heure\":\"" + heure + "\","
            + "\"date\":\"" + date + "\","
            + "\"ville\":\"" + ville + "\","
            + "\"pays\":\"" + pays + "\""
            + "}";
    }

}
