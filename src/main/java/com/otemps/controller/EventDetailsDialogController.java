package com.otemps.controller;

import com.otemps.entity.Event;
import com.otemps.entity.Participation;
import com.otemps.entity.Review;
import com.otemps.entity.User;
import com.otemps.service.EventService;
import com.otemps.service.ParticipationService;
import com.otemps.service.ReviewService;
import com.otemps.service.UserService;
import com.otemps.utils.PdfGenerator;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;

public class EventDetailsDialogController {

    @FXML private Label titleLabel, statusLabel, dateSubtitleLabel, locationLabel, placesLabel, periodLabel, descriptionLabel;
    @FXML private ComboBox<User> userComboBox;
    @FXML private ComboBox<Integer> ratingComboBox;
    @FXML private TextArea commentArea;
    @FXML private VBox reviewsContainer;
    @FXML private Button registerButton;

    private Event event;
    private EventService eventService = new EventService();
    private ParticipationService participationService = new ParticipationService();
    private ReviewService reviewService = new ReviewService();
    private UserService userService = new UserService();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public void setEvent(Event event) {
        this.event = event;
        updateUI();
    }

    private void updateUI() {
        if (event == null) return;

        titleLabel.setText(event.getTitre());
        statusLabel.setText(event.getStatut().toUpperCase());
        
        String dateText = "Du " + formatter.format(event.getDateDebut()) + " au " + formatter.format(event.getDateFin());
        dateSubtitleLabel.setText(dateText);
        
        locationLabel.setText(event.getLieu());
        
        int count = participationService.countByEvent(event.getId());
        int rem = event.getNbPlaces() - count;
        placesLabel.setText((rem > 0 ? rem : 0) + " places restantes");
        
        long days = Duration.between(event.getDateDebut(), event.getDateFin()).toDays();
        periodLabel.setText((days <= 0 ? 1 : days) + " jour(s)");
        
        descriptionLabel.setText(event.getDescription());

        userComboBox.getItems().setAll(userService.findAll());
        ratingComboBox.getItems().setAll(1, 2, 3, 4, 5);

        loadReviews();
    }

    private void loadReviews() {
        reviewsContainer.getChildren().clear();
        List<Review> reviews = reviewService.findByEvent(event.getId());
        
        if (reviews.isEmpty()) {
            Label noReview = new Label("Aucun avis pour le moment. Soyez le premier !");
            noReview.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
            reviewsContainer.getChildren().add(noReview);
        } else {
            for (Review r : reviews) {
                reviewsContainer.getChildren().add(createReviewItem(r));
            }
        }
    }

    private VBox createReviewItem(Review r) {
        VBox item = new VBox(8);
        item.getStyleClass().add("review-item");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label userName = new Label(r.getUser().getName());
        userName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label stars = new Label(createStars(r.getRating()));
        stars.setStyle("-fx-text-fill: #f59e0b;");
        
        header.getChildren().addAll(userName, stars);

        Label comment = new Label(r.getComment());
        comment.setWrapText(true);
        comment.setStyle("-fx-text-fill: #475569;");

        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button editBtn = new Button("✏️");
        editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEditReview(r));

        Button delBtn = new Button("🗑️");
        delBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            if (new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet avis ?").showAndWait().get() == ButtonType.OK) {
                reviewService.delete(r.getId());
                loadReviews();
            }
        });

        actions.getChildren().addAll(editBtn, delBtn);
        item.getChildren().addAll(header, comment, actions);
        
        return item;
    }

    private String createStars(int rating) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < rating ? "⭐" : "☆");
        }
        return sb.toString();
    }

    @FXML
    private void handleRegister() {
        User u = userComboBox.getValue();
        if (u == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un profil !").show();
            return;
        }

        FileChooser fc = new FileChooser();
        String safeTitle = event.getTitre().replaceAll("[^a-zA-Z0-9.-]", "_");
        fc.setInitialFileName("recu_" + safeTitle + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf"));
        
        File file = fc.showSaveDialog(registerButton.getScene().getWindow());
        if (file != null) {
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getParentFile(), file.getName() + ".pdf");
            }
            
            Participation p = new Participation();
            p.setEvent(event);
            p.setUser(u);
            
            boolean added = participationService.add(p);
            PdfGenerator.generateParticipationReceipt(event, u, file);
            
            if (added) {
                new Alert(Alert.AlertType.INFORMATION, "Inscription réussie !").show();
            } else {
                new Alert(Alert.AlertType.INFORMATION, "Vous êtes déjà inscrit ! Reçu généré.").show();
            }
            updateUI();
        }
    }

    @FXML
    private void handleSubmitReview() {
        User u = userComboBox.getValue();
        Integer rating = ratingComboBox.getValue();
        String comment = commentArea.getText().trim();
        
        if (u == null || rating == null || comment.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs (Profil, Note, Commentaire) !").show();
            return;
        }

        // Bloquer temporairement pour l'analyse
        commentArea.setDisable(true);
        
        new Thread(() -> {
            try {
                String encodedComment = java.net.URLEncoder.encode(comment, "UTF-8");
                String url = "https://www.purgomalum.com/service/containsprofanity?text=" + encodedComment;
                
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                boolean hasBadWords = Boolean.parseBoolean(response.body());
                
                Platform.runLater(() -> {
                    commentArea.setDisable(false);
                    if (hasBadWords) {
                        new Alert(Alert.AlertType.ERROR, "Votre commentaire contient des mots inappropriés. Veuillez rester poli !").show();
                    } else {
                        Review r = new Review();
                        r.setEvent(event);
                        r.setUser(u);
                        r.setRating(rating);
                        r.setComment(comment);
                        
                        reviewService.add(r);
                        commentArea.clear();
                        ratingComboBox.getSelectionModel().clearSelection();
                        loadReviews();
                        new Alert(Alert.AlertType.INFORMATION, "Merci pour votre avis !").show();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    commentArea.setDisable(false);
                    // Si l'API échoue, on laisse passer ou on bloque ? Généralement on laisse passer avec un log
                    Review r = new Review();
                    r.setEvent(event);
                    r.setUser(u);
                    r.setRating(rating);
                    r.setComment(comment);
                    reviewService.add(r);
                    commentArea.clear();
                    loadReviews();
                });
            }
        }).start();
    }

    private void handleEditReview(Review r) {
        TextInputDialog dialog = new TextInputDialog(r.getComment());
        dialog.setTitle("Modifier l'avis");
        dialog.setHeaderText("Modifiez votre commentaire :");
        Optional<String> res = dialog.showAndWait();
        if (res.isPresent()) {
            r.setComment(res.get());
            reviewService.update(r);
            loadReviews();
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) titleLabel.getScene().getWindow()).close();
    }
}
