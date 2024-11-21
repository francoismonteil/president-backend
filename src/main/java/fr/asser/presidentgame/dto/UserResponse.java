package fr.asser.presidentgame.dto;

import java.util.List;

public class UserResponse {
    private String username;
    private String avatarUrl;
    private Integer gamesPlayed;
    private Integer gamesWon;
    private List<String> roles;

    public UserResponse(String username, String avatarUrl, Integer gamesPlayed, Integer gamesWon, List<String> roles) {
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.roles = roles;
    }

    // Getters et setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(Integer gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public Integer getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(Integer gamesWon) {
        this.gamesWon = gamesWon;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
