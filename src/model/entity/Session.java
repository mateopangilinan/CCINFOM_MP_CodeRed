package model.entity;

public record Session(int userId, String name, String role) {
    public boolean admin() {
        return "admin".equals(role);
    }
}
