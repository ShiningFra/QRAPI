/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.QRAPI.model;

/**
 *
 * @author Roddier
 */

import com.example.QRAPI.repository.CourseRepository;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QRCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long clientId;
    private Long chauffeurId;
    private String content;

    // Constructeur
    
    @OneToOne
    @JoinColumn(name = "course_id")
    private Course course;
    
    // Getters & Setters
    public String getContent() { return content; }
    
    public Long getId(){
        return id;
    }
    
    public void setClientId(Long clientId){
        this.clientId = clientId;
    }
    
    public void setCourse(Course course){
        this.course = course;
    }
    
    public void setContent(String content){
        this.content = content;
    }
    
    public void setChauffeurId(Long chauffeurId){
        this.chauffeurId = chauffeurId;
    }
}
