package com.nathankruiz3.blog.Model;

public class Blog {

    public String title, description, image, timestamp, userID;

    public Blog() {
    }

    public Blog(String title, String description, String image, String timestamp, String userID) {
        this.title = title;
        this.description = description;
        this.image = image;
        this.timestamp = timestamp;
        this.userID = userID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }
}
