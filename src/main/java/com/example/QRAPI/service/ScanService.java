/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.QRAPI.service;

/**
 *
 * @author Roddier
 */

import com.example.QRAPI.model.Chauffeur;
import com.example.QRAPI.model.Client;
import com.example.QRAPI.model.Course;
import com.example.QRAPI.model.QRCode;
import com.example.QRAPI.repository.ChauffeurRepository;
import com.example.QRAPI.repository.ClientRepository;
import com.example.QRAPI.repository.CourseRepository;
import com.example.QRAPI.repository.QRCodeRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ScanService {

    private final CourseRepository courseRepository;
    private final ChauffeurRepository ch;
    private final ClientRepository cl;
    private final QRCodeRepository q;

    public ScanService(CourseRepository courseRepository, ChauffeurRepository ch, ClientRepository cl, QRCodeRepository q) {
        this.courseRepository = courseRepository;
        this.ch = ch;
        this.cl = cl;
        this.q = q;
    }

    public String processScan(String qrCodeData) {
        // 1. Vérifier que les données QR sont valides (format attendu : chauffeurId:courseId)
        String[] parts = qrCodeData.split(":");
        if (parts.length != 13) {
            return "Format QR Code invalide. Veuillez vérifier les données.";
        }

        // 2. Récupérer les IDs chauffeur et course
        Long clientId;
        Long chauffeurId;
        Long courseId;
        String clientNom;
        String clientPrenom;
        String clientPhone;
        String chauffeurNom;
        String chauffeurPrenom;
        String chauffeurPhone;
        String courseDepart;
        String courseArrivee;
        String courseDate;
        Double coursePrix;
        try {
            clientId = Long.valueOf(parts[0]);
            chauffeurId = Long.valueOf(parts[1]);
            courseId = Long.valueOf(parts[2]);
            clientNom = String.valueOf(parts[3]);
            clientPrenom = String.valueOf(parts[4]);
            clientPhone = String.valueOf(parts[5]);
            chauffeurNom = String.valueOf(parts[6]);
            chauffeurPrenom = String.valueOf(parts[7]);
            chauffeurPhone = String.valueOf(parts[8]);
            courseDepart = String.valueOf(parts[9]);
            courseArrivee = String.valueOf(parts[10]);
            courseDate = String.valueOf(parts[11]);
            coursePrix = Double.valueOf(parts[12]);
        } catch (NumberFormatException e) {
            return "Données QR invalides : IDs incorrects.";
        }
        
        try{
            Optional<QRCode> qr = q.findByCourseId(courseId);
            QRCode qrc = qr.get();
            // 3. Vérifier que la course existe
        Optional<Course> optionalCourse = courseRepository.findById(courseId);
        Optional<Client> optionalCl = cl.findById(clientId);
        Optional<Chauffeur> optionalCh = ch.findById(chauffeurId);
        if (optionalCl.isEmpty() || optionalCourse.isEmpty() || optionalCh.isEmpty()) {
            return "Course non trouvée pour l'ID donné.";
        }
        
        Course course = optionalCourse.get();

        // 4. Vérifier que le chauffeur est celui qui a proposé la course
        if (!(chauffeurId.equals(course.getChauffeur().getId()) && clientId.equals(course.getClient().getId()))) {
            return "Le chauffeur n'est pas autorisé à marquer cette course comme complétée.";
        }

        // 5. Vérifier si la course est déjà complétée
        if (course.isCompleted()) {
            return "La course a déjà été marquée comme complétée.";
        }

        // 6. Marquer la course comme complétée
        
        if((clientNom.equals(optionalCl.get().getNom()) 
                && clientPrenom.equals(optionalCl.get().getPrenom()) 
                && clientPhone.equals(optionalCl.get().getPhone())
                && chauffeurNom.equals(optionalCh.get().getNom())
                && chauffeurPrenom.equals(optionalCh.get().getPrenom())
                && chauffeurPhone.equals(optionalCh.get().getPhone())
                && courseDepart.equals(course.getDepart())
                && courseArrivee.equals(course.getArrivee())
                && courseDate.equals(course.getDateDepart())
                && coursePrix.equals(course.getPrix()))){
            
            course.setCompleted(true);
            courseRepository.save(course);
        }else {
            return "La vérification a échoué.";
        }
        
        q.delete(qrc);        
        // 7. Retourner un message de succès
        return "Le scan a été effectué avec succès et la course est maintenant complétée.";
        }catch(Exception e){
                return "Ce code QR n'est plus valide";
        }        
    }
}
