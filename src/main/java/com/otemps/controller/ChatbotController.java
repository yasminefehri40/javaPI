package com.otemps.controller;

import com.otemps.entity.Event;
import com.otemps.service.EventService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ChatbotController implements Initializable {

    @FXML private VBox chatBox;
    @FXML private ScrollPane chatScroll;
    @FXML private TextField inputField;
    @FXML private Button sendBtn;
    @FXML private FlowPane suggestionsPane;

    private EventService eventService;

    private final String[][] LOCAL_KNOWLEDGE = {
        {"BONJOUR", "Bonjour ! Je suis ravi de vous voir. Comment puis-je vous aider aujourd'hui ?"},
        {"SALUT", "Salut ! Je suis votre assistant OTEMPS. Posez-moi vos questions !"},
        {"HELLO", "Hi there! I can help you with events, bookings, and more. Try asking in French for best results!"},
        {"QUI ES-TU", "Je suis l'Assistant Virtuel OTEMPS, programmé pour vous guider dans vos activités culturelles."},
        {"NOM", "On m'appelle l'Assistant OTEMPS, votre guide événementiel personnel."},
        {"OTEMPS", "OTEMPS est une application innovante pour la gestion et la découverte d'événements culturels d'exception."},
        {"RESERVER", "Pour réserver : Cliquez sur 'Détails' sur l'événement, choisissez votre utilisateur et validez. Votre billet sera généré automatiquement."},
        {"EVENEMENT", "Utilisez le bouton 'Liste événement' ou tapez 'Disponibles' pour voir les activités du moment."},
        {"NOTER", "Vous pouvez attribuer de 1 à 5 étoiles et laisser un commentaire dans la vue 'Détails' de l'événement."},
        {"BOARDING PASS", "C'est votre ticket PDF ! Il contient toutes les infos et un QR Code de sécurité pour l'entrée."},
        {"QR CODE", "Le QR Code sur votre reçu permet aux organisateurs de scanner votre entrée rapidement."},
        {"METEO", "Nous surveillons la météo ! En cas de pluie ou de froid intense (<14°C), les événements en extérieur sont annulés par sécurité."},
        {"LIEU", "Les lieux varient : musées, parcs, ou salles de concert. Les adresses exactes sont sur le Boarding Pass."},
        {"PRIX", "La plupart des événements culturels sur OTEMPS sont gratuits, sauf mention contraire dans la description."},
        {"ADMIN", "Le mode Admin permet de gérer le catalogue, de modifier les dates et de suivre les participations."},
        {"PROFIL", "Votre profil contient votre nom et permet de lier vos réservations et vos futurs avis."},
        {"CONTACT", "Pour toute aide supplémentaire, contactez notre support technique via l'adresse support@otemps.com."},
        {"ANNULER", "Une réservation peut être annulée par l'admin ou automatiquement selon les conditions météo."},
        {"DATE", "Les dates de début et de fin sont affichées sur chaque carte d'événement."},
        {"AIDE", "Je peux vous aider sur : Réservations, Météo, Liste des évènements, et Notation. Cliquez sur les boutons !"},
        {"MERCI", "Je vous en prie ! Profitez bien de vos activités culturelles avec OTEMPS."},
        {"AU REVOIR", "Au revoir ! À bientôt pour de nouvelles découvertes culturelles."}
    };

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        eventService = new EventService();
        addMessage("🤖 Assistant : Bonjour ! Je suis l'expert local OTEMPS. Regardez mes suggestions ou posez une question !", false);
        
        chatBox.heightProperty().addListener((observable, oldValue, newValue) -> chatScroll.setVvalue(1.0));
        
        setupSuggestions();
    }

    private void setupSuggestions() {
        String[] suggestions = {
            "Bonjour",
            "C'est quoi OTEMPS ?",
            "Comment réserver ?",
            "Liste événement",
            "Boarding Pass ?",
            "Météo & Annulation",
            "Comment noter ?",
            "Contact Support",
            "Interface Admin",
            "Lieux & Dates",
            "Merci !"
        };

        for (String s : suggestions) {
            Button btn = new Button(s);
            btn.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 15; -fx-background-radius: 15; -fx-font-size: 10px; -fx-cursor: hand;");
            btn.setOnAction(e -> {
                inputField.setText(s);
                handleSend();
            });
            suggestionsPane.getChildren().add(btn);
        }
    }

    @FXML
    private void handleSend() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;
        
        inputField.clear();
        addMessage("💬 Vous : " + msg, true);
        
        String response = findLocalResponse(msg);
        addMessage("🤖 Assistant : " + response, false);
    }

    private String findLocalResponse(String userMsg) {
        String upperMsg = userMsg.toUpperCase();
        
        // Match base de connaissance
        for (String[] entry : LOCAL_KNOWLEDGE) {
            if (upperMsg.contains(entry[0])) {
                return entry[1];
            }
        }

        // Cas spécial pour la liste des événements
        if (upperMsg.contains("LISTE") || upperMsg.contains("DISPONIBLE")) {
            List<Event> events = eventService.findAll();
            if (events.isEmpty()) return "Il n'y a pas d'événements enregistrés actuellement.";
            StringBuilder sb = new StringBuilder("Voici les événements actuels :\n");
            for (Event e : events) {
                sb.append("- ").append(e.getTitre()).append(" (Lieu: ").append(e.getLieu()).append(")\n");
            }
            return sb.toString();
        }

        return "Désolé, je n'ai pas de réponse précise pour cela. Essayez un mot-clé comme 'Réserver', 'Météo' ou 'OTEMPS'.";
    }

    private void addMessage(String text, boolean isUser) {
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(280);
        label.setPadding(new Insets(10));
        
        if (isUser) {
            label.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-background-radius: 12 12 0 12;");
        } else {
            label.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-background-radius: 12 12 12 0;");
        }

        messageContainer.getChildren().add(label);
        chatBox.getChildren().add(messageContainer);
    }
}
