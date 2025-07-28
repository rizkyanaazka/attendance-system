package com.attendance.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Attendance {
    private int id;
    private int userId;
    private int scheduleId;
    private String userName;
    private String userNim;
    private String subject;
    private LocalDate date;
    private LocalTime timeIn;
    private String status;

    public Attendance() {
    }

    public Attendance(int userId, int scheduleId, LocalDate date, LocalTime timeIn, String status) {
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.date = date;
        this.timeIn = timeIn;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserNim() {
        return userNim;
    }

    public void setUserNim(String userNim) {
        this.userNim = userNim;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(LocalTime timeIn) {
        this.timeIn = timeIn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStudentName() {
        return getUserName();
    }

    public void setStudentName(String name) {
        setUserName(name);
    }

    public String getStudentNim() {
        return getUserNim();
    }

    public void setStudentNim(String nim) {
        setUserNim(nim);
    }
}
