package com.otemps.service;

import com.otemps.entity.Event;
import com.otemps.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventService {
    private Connection connection;
    private UserService userService;

    public EventService() {
        connection = DatabaseConnection.getInstance();
        userService = new UserService();
    }

    public List<Event> findAll() {
        List<Event> events = new ArrayList<>();
        String query = "SELECT * FROM events ORDER BY date_debut DESC";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                events.add(extractFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error findAll Events: " + e.getMessage());
        }
        return events;
    }

    public List<Event> search(String term) {
        List<Event> events = new ArrayList<>();
        String query = "SELECT * FROM events WHERE titre LIKE ? OR description LIKE ? OR lieu LIKE ? ORDER BY date_debut DESC";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            String pattern = "%" + term + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(extractFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error search Events: " + e.getMessage());
        }
        return events;
    }

    public Event findById(int id) {
        String query = "SELECT * FROM events WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error findById Event: " + e.getMessage());
        }
        return null;
    }

    public void add(Event event) {
        String query = "INSERT INTO events (titre, description, date_debut, date_fin, lieu, nb_places, statut, creator_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, event.getTitre());
            ps.setString(2, event.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(event.getDateDebut()));
            ps.setTimestamp(4, Timestamp.valueOf(event.getDateFin()));
            ps.setString(5, event.getLieu());
            ps.setInt(6, event.getNbPlaces());
            ps.setString(7, event.getStatut());
            ps.setInt(8, event.getCreator().getId());
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    event.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error add Event: " + e.getMessage());
        }
    }

    public void update(Event event) {
        String query = "UPDATE events SET titre = ?, description = ?, date_debut = ?, date_fin = ?, lieu = ?, nb_places = ?, statut = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, event.getTitre());
            ps.setString(2, event.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(event.getDateDebut()));
            ps.setTimestamp(4, Timestamp.valueOf(event.getDateFin()));
            ps.setString(5, event.getLieu());
            ps.setInt(6, event.getNbPlaces());
            ps.setString(7, event.getStatut());
            ps.setInt(8, event.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error update Event: " + e.getMessage());
        }
    }

    public void delete(int id) {
        String query = "DELETE FROM events WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error delete Event: " + e.getMessage());
        }
    }

    public List<Event> findPaged(int page, int size, String sortBy, String direction) {
        List<Event> events = new ArrayList<>();
        int offset = (page - 1) * size;
        String query = "SELECT * FROM events ORDER BY " + sortBy + " " + direction + " LIMIT ? OFFSET ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, size);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(extractFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error findPaged Events: " + e.getMessage());
        }
        return events;
    }

    public int countAll() {
        String query = "SELECT COUNT(*) FROM events";
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery(query)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public boolean existsByTitre(String titre, int excludeId) {
        String query = "SELECT COUNT(*) FROM events WHERE titre = ? AND id != ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            if (connection == null) return false;
            ps.setString(1, titre);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Event extractFromResultSet(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getInt("id"));
        event.setTitre(rs.getString("titre"));
        event.setDescription(rs.getString("description"));
        event.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
        event.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
        event.setLieu(rs.getString("lieu"));
        event.setNbPlaces(rs.getInt("nb_places"));
        event.setStatut(rs.getString("statut"));
        
        int creatorId = rs.getInt("creator_id");
        event.setCreator(userService.findById(creatorId));
        
        return event;
    }
}
