package com.otemps.entity;

import java.time.LocalDateTime;

public class Participation {
    private int id;
    private Event event;
    private User user;
    private LocalDateTime dateInscription;
    private String statut;

    public Participation() {
        this.dateInscription = LocalDateTime.now();
        this.statut = "confirmée";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
