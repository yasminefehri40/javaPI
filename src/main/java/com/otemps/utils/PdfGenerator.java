package com.otemps.utils;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.otemps.entity.Event;
import com.otemps.entity.User;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;

public class PdfGenerator {
    
    public static void generateParticipationReceipt(Event event, User participant, File targetFile) {
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 40, 40);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(targetFile));
            document.open();

            try {
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Font.BOLD, Color.WHITE);
                Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.NORMAL, new Color(100, 116, 139));
                Font dataFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD, new Color(15, 23, 42));

                PdfPTable masterTable = new PdfPTable(2);
                masterTable.setWidthPercentage(100);
                masterTable.setWidths(new float[]{75f, 25f});
                
                // HEADER ROW
                PdfPCell mainHeader = new PdfPCell(new Paragraph("✈ BOARDING PASS", titleFont));
                mainHeader.setBackgroundColor(new Color(14, 165, 233));
                mainHeader.setPadding(15);
                masterTable.addCell(mainHeader);
                
                PdfPCell stubHeader = new PdfPCell(new Paragraph("ENTRY", titleFont));
                stubHeader.setBackgroundColor(new Color(2, 132, 199));
                stubHeader.setPadding(15);
                stubHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
                masterTable.addCell(stubHeader);

                // BODY LEFT
                PdfPCell leftCell = new PdfPCell();
                leftCell.setPadding(15);
                leftCell.setBackgroundColor(new Color(248, 250, 252));
                
                PdfPTable grid = new PdfPTable(2);
                grid.setWidthPercentage(100);
                
                grid.addCell(createSimpleCell("PASSENGER NAME", participant.getName().toUpperCase(), labelFont, dataFont));
                grid.addCell(createSimpleCell("EVENT", event.getTitre().toUpperCase(), labelFont, dataFont));
                
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                grid.addCell(createSimpleCell("DATE & TIME", event.getDateDebut().format(dtf), labelFont, dataFont));
                grid.addCell(createSimpleCell("LOCATION", event.getLieu(), labelFont, dataFont));
                
                leftCell.addElement(grid);
                masterTable.addCell(leftCell);

                // BODY RIGHT
                PdfPCell rightCell = new PdfPCell();
                rightCell.setPadding(10);
                rightCell.setBackgroundColor(new Color(241, 245, 249));
                rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                
                String refStr = "TCK" + event.getId() + "-" + participant.getId();
                
                try {
                    String qrData = "TICKET\nEvénement: " + event.getTitre() + "\nPatient: " + participant.getName() + " (" + refStr + ")";
                    String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + URLEncoder.encode(qrData, "UTF-8");
                    Image qrImage = Image.getInstance(new URL(qrUrl));
                    qrImage.scaleAbsolute(100, 100);
                    qrImage.setAlignment(Element.ALIGN_CENTER);
                    rightCell.addElement(qrImage);
                } catch (Exception ex) {
                    Paragraph err = new Paragraph("[QR NON CHARGÉ]");
                    err.setAlignment(Element.ALIGN_CENTER);
                    rightCell.addElement(err);
                }
                
                Paragraph stubP = new Paragraph("REF: " + refStr, FontFactory.getFont(FontFactory.COURIER_BOLD, 9));
                stubP.setAlignment(Element.ALIGN_CENTER);
                stubP.setSpacingBefore(10);
                rightCell.addElement(stubP);

                masterTable.addCell(rightCell);
                document.add(masterTable);
                
                Paragraph footer = new Paragraph("Present this boarding pass at the entrance.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
                footer.setAlignment(Element.ALIGN_CENTER);
                footer.setSpacingBefore(20);
                document.add(footer);

            } catch (Throwable t) {
                System.err.println("Fatal Error inside PDF construction:");
                t.printStackTrace();
            }

        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (document.isOpen()) {
                try {
                    document.close();
                } catch (Exception docEx) {
                    docEx.printStackTrace();
                }
            }
        }
    }

    private static PdfPCell createSimpleCell(String label, String data, Font labelFont, Font dataFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(15);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(data, dataFont));
        return cell;
    }
}
