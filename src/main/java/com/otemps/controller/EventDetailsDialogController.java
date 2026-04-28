package com.otemps.controller;

import com.otemps.entity.Event;
import com.otemps.entity.Participation;
import com.otemps.entity.Review;
import com.otemps.entity.User;
import com.otemps.service.EventService;
import com.otemps.service.ParticipationService;
import com.otemps.service.ReviewService;
import com.otemps.service.UserService;
import com.otemps.session.UserSession;
import com.otemps.utils.PdfGenerator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
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

public class EventDetailsDialogController {

    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label dateSubtitleLabel;
    @FXML private Label locationLabel;
    @FXML private Label placesLabel;
    @FXML private Label periodLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label sessionUserLabel;
    @FXML private ComboBox<Integer> ratingComboBox;
    @FXML private TextArea commentArea;
    @FXML private VBox reviewsContainer;
    @FXML private Button registerButton;

    private Event event;
    private final EventService eventService = new EventService();
    private final ParticipationService participationService = new ParticipationService();
    private final ReviewService reviewService = new ReviewService();
    private final UserService userService = new UserService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");

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

        User currentUser = UserSession.getCurrentUser();
        sessionUserLabel.setText(currentUser == null
                ? "Aucune session active"
                : currentUser.getName() + " - " + currentUser.getEmail());
        ratingComboBox.getItems().setAll(1, 2, 3, 4, 5);
        registerButton.setDisable(currentUser == null || rem <= 0);

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

        User currentUser = UserSession.getCurrentUser();
        if (currentUser != null && r.getUser() != null && r.getUser().getId() == currentUser.getId()) {
            Button editBtn = new Button("Modifier");
            editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            editBtn.setOnAction(e -> handleEditReview(r));

            Button delBtn = new Button("Supprimer");
            delBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            delBtn.setOnAction(e -> {
                if (new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet avis ?").showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    reviewService.delete(r.getId());
                    loadReviews();
                }
            });

            actions.getChildren().addAll(editBtn, delBtn);
        }

        item.getChildren().addAll(header, comment, actions);
        return item;
    }

    private String createStars(int rating) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < rating ? "*" : "-");
        }
        return sb.toString();
    }

    @FXML
    private void handleRegister() {
        User user = UserSession.getCurrentUser();
        if (user == null) {
            new Alert(Alert.AlertType.WARNING, "Aucune session utilisateur active.").show();
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
            p.setUser(user);

            boolean added = participationService.add(p);
            PdfGenerator.generateParticipationReceipt(event, user, file);

            if (added) {
                new Alert(Alert.AlertType.INFORMATION, "Inscription reussie !").show();
            } else {
                new Alert(Alert.AlertType.INFORMATION, "Vous etes deja inscrit. Recu genere.").show();
            }
            updateUI();
        }
    }

    @FXML
    private void handleSubmitReview() {
        User user = UserSession.getCurrentUser();
        Integer rating = ratingComboBox.getValue();
        String comment = commentArea.getText().trim();

        if (user == null || rating == null || comment.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs (note et commentaire).").show();
            return;
        }

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
                        new Alert(Alert.AlertType.ERROR, "Votre commentaire contient des mots inappropries.").show();
                    } else {
                        saveReview(user, rating, comment, true);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    commentArea.setDisable(false);
                    saveReview(user, rating, comment, false);
                });
            }
        }).start();
    }

    private void saveReview(User user, Integer rating, String comment, boolean showConfirmation) {
        Review r = new Review();
        r.setEvent(event);
        r.setUser(user);
        r.setRating(rating);
        r.setComment(comment);

        reviewService.add(r);
        commentArea.clear();
        ratingComboBox.getSelectionModel().clearSelection();
        loadReviews();

        if (showConfirmation) {
            new Alert(Alert.AlertType.INFORMATION, "Merci pour votre avis !").show();
        }
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
