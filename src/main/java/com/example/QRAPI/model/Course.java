/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.QRAPI.model;

/**
 *
 * @author Roddier
 */

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String lieu_depart;
    private String lieu_arrivee;
    private Double prix;
    private String date_depart;
    

    @ManyToOne
    @JoinColumn(name = "chauffeur_id", nullable = false)
    private Chauffeur chauffeur; // Relation avec l'entité Chauffeur
    
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client; // Relation avec l'entité Chauffeur
    
    
    private Boolean completed = false;
    
    // Getters et setters
    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public Chauffeur getChauffeur(){
        return chauffeur;
    }
    
    public Client getClient(){
        return client;
    }
    
    public Boolean getCompleted() { return completed;}
    
    public String getDateDepart(){
        return date_depart;
    }
    
    public Double getPrix(){
        return prix;
    }
    
    public String getDepart(){
        return lieu_depart;
    }
    
    public String getArrivee(){
        return lieu_arrivee;
    }
    
    public Long getId(){
        return id;
    }
}
