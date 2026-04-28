package otemps.main;

import com.otemps.entity.User;
import com.otemps.session.UserSession;

public final class SessionBridge {

    private SessionBridge() {
    }

    public static boolean isLoggedIn() {
        return UserSession.isLoggedIn();
    }

    public static boolean isAdmin() {
        return UserSession.isAdmin();
    }

    public static User getCurrentUser() {
        return UserSession.getCurrentUser();
    }

    public static String getDisplayName() {
        User user = getCurrentUser();
        return user == null ? "Invite" : user.getName();
    }

    public static String getRoleLabel() {
        User user = getCurrentUser();
        if (user == null) {
            return "Aucune session";
        }
        return user.isAdmin() ? "Administrateur" : "Participant";
    }

    public static void logout() {
        UserSession.logout();
    }
}
