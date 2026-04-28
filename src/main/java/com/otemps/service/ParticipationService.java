package com.otemps.service;

import com.otemps.entity.Participation;
import com.otemps.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService {
    private Connection connection;
    private EventService eventService;
    private UserService userService;

    public ParticipationService() {
        connection = DatabaseConnection.getInstance();
        eventService = new EventService();
        userService = new UserService();
    }

    public List<Participation> findByEvent(int eventId) {
        List<Participation> participations = new ArrayList<>();
        String query = "SELECT * FROM participations WHERE event_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    participations.add(extractFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error findByEvent Participations: " + e.getMessage());
        }
        return participations;
    }

    public List<Participation> findByUser(int userId) {
        List<Participation> participations = new ArrayList<>();
        String query = "SELECT * FROM participations WHERE user_id = ? ORDER BY date_inscription DESC";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    participations.add(extractFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error findByUser Participations: " + e.getMessage());
        }
        return participations;
    }

    public int countByEvent(int eventId) {
        String query = "SELECT COUNT(*) FROM participations WHERE event_id = ? AND statut = 'confirmée'";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            if (connection == null) return 0;
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean add(Participation participation) {
        String query = "INSERT INTO participations (event_id, user_id, date_inscription, statut) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            if (connection == null) return false;
            
            ps.setInt(1, participation.getEvent().getId());
            ps.setInt(2, participation.getUser().getId());
            ps.setTimestamp(3, Timestamp.valueOf(participation.getDateInscription()));
            ps.setString(4, participation.getStatut());
            
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) return false;

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    participation.setId(rs.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void delete(int id) {
        String query = "DELETE FROM participations WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error delete Participation: " + e.getMessage());
        }
    }

    private Participation extractFromResultSet(ResultSet rs) throws SQLException {
        Participation p = new Participation();
        p.setId(rs.getInt("id"));
        p.setEvent(eventService.findById(rs.getInt("event_id")));
        p.setUser(userService.findById(rs.getInt("user_id")));
        p.setDateInscription(rs.getTimestamp("date_inscription").toLocalDateTime());
        p.setStatut(rs.getString("statut"));
        return p;
    }
}
