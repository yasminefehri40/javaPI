package com.otemps.service;

import com.otemps.entity.Review;
import com.otemps.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewService {
    private Connection connection;
    private EventService eventService;
    private UserService userService;

    public ReviewService() {
        connection = DatabaseConnection.getInstance();
        eventService = new EventService();
        userService = new UserService();
    }

    public void add(Review review) {
        String query = "INSERT INTO reviews (event_id, user_id, comment, rating, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, review.getEvent().getId());
            ps.setInt(2, review.getUser().getId());
            ps.setString(3, review.getComment());
            ps.setInt(4, review.getRating());
            ps.setTimestamp(5, Timestamp.valueOf(review.getCreatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error add Review: " + e.getMessage());
        }
    }

    public List<Review> findByEvent(int eventId) {
        List<Review> reviews = new ArrayList<>();
        String query = "SELECT * FROM reviews WHERE event_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(extractFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error findByEvent Reviews: " + e.getMessage());
        }
        return reviews;
    }

    public void update(Review review) {
        String query = "UPDATE reviews SET comment = ?, rating = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, review.getComment());
            ps.setInt(2, review.getRating());
            ps.setInt(3, review.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error update Review: " + e.getMessage());
        }
    }

    public void delete(int id) {
        String query = "DELETE FROM reviews WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error delete Review: " + e.getMessage());
        }
    }

    private Review extractFromResultSet(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getInt("id"));
        r.setEvent(eventService.findById(rs.getInt("event_id")));
        r.setUser(userService.findById(rs.getInt("user_id")));
        r.setComment(rs.getString("comment"));
        r.setRating(rs.getInt("rating"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return r;
    }
}
