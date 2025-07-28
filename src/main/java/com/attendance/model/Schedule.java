package com.attendance.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Schedule {
    private int id;
    private String subject;
    private String teacher;
    private String className;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private boolean isActive;

    public Schedule() {}

    public Schedule(String subject, String teacher, String className, LocalDate date,
                    LocalTime startTime, LocalTime endTime, String room) {
        this.subject = subject;
        this.teacher = teacher;
        this.className = className;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.isActive = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isAttendanceAvailable() {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        if (!isActive) {
            return false;
        }

        LocalTime attendanceStart = startTime.minusMinutes(15);
        LocalTime attendanceEnd = startTime.plusMinutes(30);

        return today.equals(date) && now.isAfter(attendanceStart) && now.isBefore(attendanceEnd);
    }
}
