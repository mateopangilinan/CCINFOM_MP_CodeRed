package model.entity;

/**
 * Session.java
 *
 * Holds the ID, display name and role of the signed-in account. Services use it when checking
 * permissions and recheck the account in the database before allowing access.
 */
public record Session(int userId, String name, String role) {
    public boolean admin() {
        return "admin".equals(role);
    }
}
