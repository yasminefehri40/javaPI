package otemps.services;

import otemps.entites.Media;
import otemps.main.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MediaService implements IService<Media> {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getCnx();
    }

    @Override
    public int ajouter(Media media) throws SQLException {
        String req = "INSERT INTO media (lien_fichier, type_media, objet_id) VALUES (?, ?, ?)";

        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, media.getLienFichier());
            ps.setString(2, media.getType());
            ps.setInt(3, media.getIdObjet());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public void delete(int id) throws SQLException {
        String req = "DELETE FROM media WHERE id = ?";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Media> afficher() {
        List<Media> medias = new ArrayList<>();
        String req = "SELECT * FROM media";

        try (Connection cnx = getConnection();
             Statement statement = cnx.createStatement();
             ResultSet rs = statement.executeQuery(req)) {

            while (rs.next()) {
                medias.add(new Media(
                        rs.getInt("id"),
                        rs.getString("lien_fichier"),
                        rs.getString("type_media"),
                        rs.getInt("objet_id")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
        return medias;
    }

    @Override
    public void update(Media media) throws SQLException {
        String req = "UPDATE media SET lien_fichier=?, type_media=?, objet_id=? WHERE id=?";

        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, media.getLienFichier());
            ps.setString(2, media.getType());
            ps.setInt(3, media.getIdObjet());
            ps.setInt(4, media.getIdMedia());
            ps.executeUpdate();
        }
    }

    public Media getById(int id) {
        String query = "SELECT * FROM media WHERE id = ?";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Media(
                        rs.getInt("id"),
                        rs.getString("lien_fichier"),
                        rs.getString("type_media"),
                        rs.getInt("objet_id")
                );
            }
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
        return null;
    }

    public List<Media> getByObjet(int idObjet) {
        List<Media> medias = new ArrayList<>();
        String req = "SELECT * FROM media WHERE objet_id = ?";

        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, idObjet);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                medias.add(new Media(
                        rs.getInt("id"),
                        rs.getString("lien_fichier"),
                        rs.getString("type_media"),
                        rs.getInt("objet_id")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
        return medias;
    }

    public int getTotalCount() {
        String sql = "SELECT COUNT(*) as total FROM media";

        try (Connection conn = DatabaseConnection.getInstance().getCnx();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.out.println("Erreur getTotalCount Media: " + e.getMessage());
        }

        return 0;
    }

    public int getCountByObjet(int idObjet) {
        String sql = "SELECT COUNT(*) as total FROM media WHERE objet_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getCnx();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idObjet);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.out.println("Erreur getCountByObjet: " + e.getMessage());
        }

        return 0;
    }
}
