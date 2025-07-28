package com.attendance.model;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private String className;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;

    public Notification() {}

    public Notification(String className, String message) {
        this.className = className;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", className='" + className + '\'' +
                ", message='" + message + '\'' +
                ", createdAt=" + createdAt +
                ", isRead=" + isRead +
                '}';
    }
}
