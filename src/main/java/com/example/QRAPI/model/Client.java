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
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    
    public String getNom(){
        return nom;
    }
    public String getPrenom(){
        return prenom;
    }
    public String getPhone(){
        return telephone;
    }
    
    public Long getId(){
        return id;
    }
}
