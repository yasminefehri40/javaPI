package com.otemps;

import com.otemps.entity.Event;
import com.otemps.entity.User;
import com.otemps.utils.PdfGenerator;

import java.io.File;
import java.time.LocalDateTime;

public class TestPdf {
    public static void main(String[] args) {
        try {
            Event event = new Event();
            event.setId(1);
            event.setTitre("Test Event");
            event.setLieu("Paris");
            event.setDateDebut(LocalDateTime.now());
            event.setDescription("Test Desc");

            User u = new User();
            u.setId(1);
            u.setName("Test User");
            u.setEmail("test@test.com");

            File f = new File("test_direct.pdf");
            System.out.println("Generating to " + f.getAbsolutePath());
            PdfGenerator.generateParticipationReceipt(event, u, f);
            System.out.println("Done! Length: " + f.length());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
