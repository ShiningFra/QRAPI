package com.example.QRAPI.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.util.UUID;

@Table("history")
public class History {
    @PrimaryKey
    private UUID id;

    private Long client_id;
    private Long driver_id;   // anciennement chauffeurId
    private Long trip_id;     // anciennement courseId
    private String location;  // anciennement lieu
    private String supplier;  // anciennement fournisseur
    private String hour;      // anciennement heure
    private String date;
    private String city;      // anciennement ville
    private String country;   // anciennement pays

    // Getters et setters (noms des méthodes inchangés)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getClientId() { return client_id; }
    public void setClientId(Long client_id) { this.client_id = client_id; }

    public Long getChauffeurId() { return driver_id; }
    public void setChauffeurId(Long driver_id) { this.driver_id = driver_id; }

    public Long getCourseId() { return trip_id; }
    public void setCourseId(Long trip_id) { this.trip_id = trip_id; }

    public String getLieu() { return location; }
    public void setLieu(String location) { this.location = location; }

    public String getFournisseur() { return supplier; }
    public void setFournisseur(String supplier) { this.supplier = supplier; }

    public String getHeure() { return hour; }
    public void setHeure(String hour) { this.hour = hour; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getVille() { return city; }
    public void setVille(String city) { this.city = city; }

    public String getPays() { return country; }
    public void setPays(String country) { this.country = country; }

    @Override
    public String toString() {
        return "{"
            + "\"clientId\":" + client_id + ","
            + "\"chauffeurId\":" + driver_id + ","
            + "\"courseId\":" + trip_id + ","
            + "\"lieu\":\"" + location + "\","
            + "\"fournisseur\":\"" + supplier + "\","
            + "\"heure\":\"" + hour + "\","
            + "\"date\":\"" + date + "\","
            + "\"ville\":\"" + city + "\","
            + "\"pays\":\"" + country + "\""
            + "}";
    }
}
