package com.attendance;

import com.attendance.model.Attendance;
import com.attendance.model.Schedule;
import com.attendance.model.User;
import com.attendance.model.Notification;
import com.attendance.model.Module;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/attendance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    public static void initializeDatabase() {
        try {
            try (Connection ignored = getConnection()) {
                System.out.println("Database initialized successfully!");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // === MODULE METHODS ===

    public static boolean insertModule(Module module) {
        String sql = "INSERT INTO modules (title, description, file_name, file_path, file_type, file_size, subject, teacher_name, class_name, uploaded_at, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), TRUE)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, module.getTitle());
            ps.setString(2, module.getDescription());
            ps.setString(3, module.getFileName());
            ps.setString(4, module.getFilePath());
            ps.setString(5, module.getFileType());
            ps.setLong(6, module.getFileSize());
            ps.setString(7, module.getSubject());
            ps.setString(8, module.getTeacherName());
            ps.setString(9, module.getClassName());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting module: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mengambil semua modul berdasarkan mata pelajaran tentor
     */
    public static List<Module> getModulesBySubject(String subject) {
        List<Module> modules = new ArrayList<>();
        String sql = "SELECT * FROM modules WHERE subject = ? AND is_active = TRUE ORDER BY uploaded_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Module module = new Module();
                module.setId(rs.getInt("id"));
                module.setTitle(rs.getString("title"));
                module.setDescription(rs.getString("description"));
                module.setFileName(rs.getString("file_name"));
                module.setFilePath(rs.getString("file_path"));
                module.setFileType(rs.getString("file_type"));
                module.setFileSize(rs.getLong("file_size"));
                module.setSubject(rs.getString("subject"));
                module.setTeacherName(rs.getString("teacher_name"));
                module.setClassName(rs.getString("class_name"));
                module.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
                module.setActive(rs.getBoolean("is_active"));
                modules.add(module);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving modules by subject: " + e.getMessage());
        }

        return modules;
    }

    /**
     * Mengambil semua modul berdasarkan kelas siswa
     */
    public static List<Module> getModulesByClass(String className) {
        List<Module> modules = new ArrayList<>();
        String sql = "SELECT * FROM modules WHERE class_name = ? AND is_active = TRUE ORDER BY uploaded_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, className);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Module module = new Module();
                module.setId(rs.getInt("id"));
                module.setTitle(rs.getString("title"));
                module.setDescription(rs.getString("description"));
                module.setFileName(rs.getString("file_name"));
                module.setFilePath(rs.getString("file_path"));
                module.setFileType(rs.getString("file_type"));
                module.setFileSize(rs.getLong("file_size"));
                module.setSubject(rs.getString("subject"));
                module.setTeacherName(rs.getString("teacher_name"));
                module.setClassName(rs.getString("class_name"));
                module.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
                module.setActive(rs.getBoolean("is_active"));
                modules.add(module);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving modules by class: " + e.getMessage());
        }

        return modules;
    }

    /**
     * Menghapus modul (soft delete)
     */
    public static boolean deleteModule(int moduleId) {
        String sql = "UPDATE modules SET is_active = FALSE WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, moduleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting module: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mengambil modul berdasarkan ID
     */
    public static Module getModuleById(int moduleId) {
        String sql = "SELECT * FROM modules WHERE id = ? AND is_active = TRUE";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, moduleId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Module module = new Module();
                module.setId(rs.getInt("id"));
                module.setTitle(rs.getString("title"));
                module.setDescription(rs.getString("description"));
                module.setFileName(rs.getString("file_name"));
                module.setFilePath(rs.getString("file_path"));
                module.setFileType(rs.getString("file_type"));
                module.setFileSize(rs.getLong("file_size"));
                module.setSubject(rs.getString("subject"));
                module.setTeacherName(rs.getString("teacher_name"));
                module.setClassName(rs.getString("class_name"));
                module.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
                module.setActive(rs.getBoolean("is_active"));
                return module;
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving module by ID: " + e.getMessage());
        }
        return null;
    }

    // === NOTIFICATION METHODS ===

    /**
     * Mengirim notifikasi ke kelas tertentu
     */
    public static void sendNotificationToClass(String className, String message) {
        String sql = "INSERT INTO notifications (class_name, message, created_at, is_read) VALUES (?, ?, NOW(), FALSE)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, className);
            ps.setString(2, message);
            ps.executeUpdate();
            System.out.println("Notification sent to class " + className + ": " + message);
        } catch (SQLException e) {
            System.err.println("Error sending notification: " + e.getMessage());
        }
    }

    /**
     * Mengambil notifikasi untuk kelas tertentu
     */
    public static List<Notification> getNotificationsByClass(String className) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE class_name = ? ORDER BY created_at DESC LIMIT 10";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, className);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Notification notification = new Notification();
                notification.setId(rs.getInt("id"));
                notification.setClassName(rs.getString("class_name"));
                notification.setMessage(rs.getString("message"));
                notification.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                notification.setRead(rs.getBoolean("is_read"));
                notifications.add(notification);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving notifications: " + e.getMessage());
        }

        return notifications;
    }

    /**
     * Menghitung jumlah notifikasi yang belum dibaca
     */
    public static int getUnreadNotificationCount(String className) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE class_name = ? AND is_read = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, className);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("Error counting unread notifications: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Menandai notifikasi sebagai sudah dibaca
     */
    public static boolean markNotificationAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error marking notification as read: " + e.getMessage());
            return false;
        }
    }

    /**
     * Menandai semua notifikasi kelas sebagai sudah dibaca
     */
    public static boolean markAllNotificationsAsRead(String className) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE class_name = ? AND is_read = FALSE";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, className);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error marking all notifications as read: " + e.getMessage());
            return false;
        }
    }

    /**
     * Menghapus notifikasi lama (lebih dari 7 hari)
     */
    public static void cleanupOldNotifications() {
        String sql = "DELETE FROM notifications WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int deleted = ps.executeUpdate();
            System.out.println("Cleaned up " + deleted + " old notifications");
        } catch (SQLException e) {
            System.err.println("Error cleaning up notifications: " + e.getMessage());
        }
    }

    // === EXISTING METHODS  ===

    public static User authenticateUser(String emailOrNim, String password) {
        String sql = "SELECT * FROM users WHERE email = ? OR nim = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, emailOrNim);
            ps.setString(2, emailOrNim);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                if (password.equals(hashedPassword)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    user.setNim(rs.getString("nim"));
                    user.setClassName(rs.getString("class_name"));
                    user.setSubject(rs.getString("subject"));
                    String status = rs.getString("status");
                    user.setStatus(status != null ? status : "pending");
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return null;
    }

    public static String generateNim() {
        String prefix = "2021";
        int nextNumber = 1;
        String sql = "SELECT nim FROM users WHERE role = 'student' AND nim LIKE ? ORDER BY nim DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String lastNim = rs.getString("nim");
                if (lastNim != null && lastNim.length() >= 7) {
                    nextNumber = Integer.parseInt(lastNim.substring(4)) + 1;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating NIM: " + e.getMessage());
        }
        return prefix + String.format("%03d", nextNumber);
    }

    public static boolean startClass(int scheduleId) {
        String sql = "UPDATE schedules SET is_active = TRUE, class_started = TRUE, started_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error starting class: " + e.getMessage());
            return false;
        }
    }

    public static boolean endClass(int scheduleId) {
        String sql = "UPDATE schedules SET is_active = FALSE, class_finished = TRUE, finished_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error ending class: " + e.getMessage());
            return false;
        }
    }

    public static boolean isClassStarted(int scheduleId) {
        String sql = "SELECT class_started FROM schedules WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getBoolean("class_started");
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean isClassFinished(int scheduleId) {
        String sql = "SELECT class_finished FROM schedules WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getBoolean("class_finished");
        } catch (SQLException e) {
            return false;
        }
    }

    public static void setClassActiveForStudents(int scheduleId, boolean active) {
        System.out.println("Setting class " + scheduleId + " active status to: " + active);
    }

    public static long getTotalStudentsByClass(String className) {
        String sql = "SELECT COUNT(*) FROM users WHERE class_name = ? AND role = 'student'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, className);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            System.err.println("Error getting total students: " + e.getMessage());
            return 0;
        }
    }

    public static List<Schedule> getSchedulesByClass(String className, LocalDate date) {
        List<Schedule> schedules = new ArrayList<>();
        String sql = "SELECT * FROM schedules WHERE class_name = ? AND date = ? ORDER BY start_time";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, className);
            ps.setDate(2, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Schedule s = new Schedule();
                s.setId(rs.getInt("id"));
                s.setSubject(rs.getString("subject"));
                s.setTeacher(rs.getString("teacher"));
                s.setClassName(rs.getString("class_name"));
                s.setDate(rs.getDate("date").toLocalDate());
                s.setStartTime(rs.getTime("start_time").toLocalTime());
                s.setEndTime(rs.getTime("end_time").toLocalTime());
                s.setRoom(rs.getString("room"));
                s.setActive(rs.getBoolean("is_active"));
                schedules.add(s);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving schedules: " + e.getMessage());
        }
        return schedules;
    }

    public static List<Schedule> getSchedulesByTeacher(String teacherName, LocalDate date) {
        List<Schedule> schedules = new ArrayList<>();
        String sql = "SELECT * FROM schedules WHERE teacher = ? AND date = ? ORDER BY start_time";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teacherName);
            ps.setDate(2, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Schedule schedule = new Schedule();
                schedule.setId(rs.getInt("id"));
                schedule.setSubject(rs.getString("subject"));
                schedule.setTeacher(rs.getString("teacher"));
                schedule.setClassName(rs.getString("class_name"));
                schedule.setDate(rs.getDate("date").toLocalDate());
                schedule.setStartTime(rs.getTime("start_time").toLocalTime());
                schedule.setEndTime(rs.getTime("end_time").toLocalTime());
                schedule.setRoom(rs.getString("room"));
                schedule.setActive(rs.getBoolean("is_active"));
                schedules.add(schedule);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving schedules: " + e.getMessage());
        }
        return schedules;
    }

    public static List<Attendance> getAttendanceBySubject(String subject) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT a.*, u.name AS user_name, u.nim, s.subject FROM attendance a JOIN users u ON a.user_id = u.id JOIN schedules s ON a.schedule_id = s.id WHERE s.subject = ? ORDER BY a.date DESC, a.time_in DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Attendance a = new Attendance();
                a.setId(rs.getInt("id"));
                a.setUserId(rs.getInt("user_id"));
                a.setScheduleId(rs.getInt("schedule_id"));
                a.setUserName(rs.getString("user_name"));
                a.setUserNim(rs.getString("nim"));
                a.setSubject(rs.getString("subject"));
                a.setDate(rs.getDate("date").toLocalDate());
                a.setTimeIn(rs.getTime("time_in").toLocalTime());
                a.setStatus(rs.getString("status"));
                list.add(a);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving attendance: " + e.getMessage());
        }
        return list;
    }

    public static List<Attendance> getAttendanceBySchedule(int scheduleId) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT a.*, u.name AS user_name, u.nim, s.subject FROM attendance a JOIN users u ON a.user_id = u.id JOIN schedules s ON a.schedule_id = s.id WHERE a.schedule_id = ? ORDER BY a.date DESC, a.time_in DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Attendance a = new Attendance();
                a.setId(rs.getInt("id"));
                a.setUserId(rs.getInt("user_id"));
                a.setScheduleId(rs.getInt("schedule_id"));
                a.setUserName(rs.getString("user_name"));
                a.setUserNim(rs.getString("nim"));
                a.setSubject(rs.getString("subject"));
                a.setDate(rs.getDate("date").toLocalDate());
                a.setTimeIn(rs.getTime("time_in").toLocalTime());
                a.setStatus(rs.getString("status"));
                list.add(a);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving attendance: " + e.getMessage());
        }
        return list;
    }

    public static List<Attendance> getAttendanceByScheduleId(int scheduleId) {
        return getAttendanceBySchedule(scheduleId);
    }

    public static List<Attendance> getUserAttendanceHistory(int userId) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT a.*, s.subject, u.name, u.nim FROM attendance a JOIN schedules s ON a.schedule_id = s.id JOIN users u ON a.user_id = u.id WHERE a.user_id = ? ORDER BY a.date DESC, a.time_in DESC LIMIT 50";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Attendance a = new Attendance();
                a.setId(rs.getInt("id"));
                a.setUserId(rs.getInt("user_id"));
                a.setScheduleId(rs.getInt("schedule_id"));
                a.setUserName(rs.getString("name"));
                a.setUserNim(rs.getString("nim"));
                a.setSubject(rs.getString("subject"));
                a.setDate(rs.getDate("date").toLocalDate());
                a.setTimeIn(rs.getTime("time_in").toLocalTime());
                a.setStatus(rs.getString("status"));
                list.add(a);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving attendance history: " + e.getMessage());
        }
        return list;
    }

    public static boolean checkAttendanceExists(int userId, int scheduleId) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE user_id = ? AND schedule_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, scheduleId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean addAttendance(Attendance attendance) {
        String sql = "INSERT INTO attendance (user_id, schedule_id, date, time_in, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendance.getUserId());
            ps.setInt(2, attendance.getScheduleId());
            ps.setDate(3, Date.valueOf(attendance.getDate()));
            ps.setTime(4, Time.valueOf(attendance.getTimeIn()));
            ps.setString(5, attendance.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Mendapatkan siswa yang menunggu approval
     */
    public static List<User> getPendingStudents() {
        List<User> students = new ArrayList<>();
        String sql = "SELECT id, name, email, nim, class_name, profile_image, created_at FROM users WHERE role = 'student' AND status = 'PENDING' ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User student = new User();
                student.setId(rs.getInt("id"));
                student.setName(rs.getString("name"));
                student.setEmail(rs.getString("email"));
                student.setNim(rs.getString("nim"));
                student.setClassName(rs.getString("class_name"));
                students.add(student);
            }
        } catch (SQLException e) {
            System.err.println("Error getting pending students: " + e.getMessage());
        }
        return students;
    }

    /**
     * Approve siswa tanpa mengubah kelas
     */
    public static boolean approveStudent(int studentId, int adminId) {
        String sql = "UPDATE users SET status = 'APPROVED', approved_date = NOW(), approved_by = ? WHERE id = ? AND role = 'student'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setInt(2, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error approving student: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reject siswa
     */
    public static boolean rejectStudent(int studentId, int adminId) {
        String sql = "UPDATE users SET status = 'REJECTED', approved_by = ? WHERE id = ? AND role = 'student'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setInt(2, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error rejecting student: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mendapatkan siswa yang sudah approved
     */
    public static List<User> getApprovedStudents() {
        List<User> students = new ArrayList<>();
        String sql = "SELECT id, name, email, nim, class_name, profile_image, approved_date FROM users WHERE role = 'student' AND status = 'APPROVED' ORDER BY approved_date DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User student = new User();
                student.setId(rs.getInt("id"));
                student.setName(rs.getString("name"));
                student.setEmail(rs.getString("email"));
                student.setNim(rs.getString("nim"));
                student.setClassName(rs.getString("class_name"));
                students.add(student);
            }
        } catch (SQLException e) {
            System.err.println("Error getting approved students: " + e.getMessage());
        }
        return students;
    }

    /**
     * Mendapatkan daftar kelas yang tersedia (dari data yang sudah ada)
     */
    public static List<String> getAvailableClasses() {
        List<String> classes = new ArrayList<>();
        String sql = "SELECT DISTINCT class_name FROM users WHERE role = 'student' AND class_name IS NOT NULL AND class_name != '' AND class_name != '-' ORDER BY class_name";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String className = rs.getString("class_name");
                if (className != null && !className.trim().isEmpty() && !className.equals("-")) {
                    classes.add(className);
                }
            }
            // Tambahkan kelas-kelas standar jika belum ada
            if (!classes.contains("Kelas A")) classes.add("Kelas A");
            if (!classes.contains("Kelas B")) classes.add("Kelas B");
            if (!classes.contains("Kelas C")) classes.add("Kelas C");
        } catch (SQLException e) {
            System.err.println("Error getting available classes: " + e.getMessage());
        }
        return classes;
    }

    /**
     * Update kelas siswa yang sudah approved
     */
    public static boolean updateStudentClass(int studentId, String newClassName) {
        String sql = "UPDATE users SET class_name = ? WHERE id = ? AND role = 'student' AND status = 'APPROVED'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newClassName);
            ps.setInt(2, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating student class: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mendapatkan statistik approval
     */
    public static int[] getApprovalStatistics() {
        int[] stats = new int[3]; // [pending, approved, rejected]
        String sql = "SELECT status, COUNT(*) as count FROM users WHERE role = 'student' GROUP BY status";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String status = rs.getString("status");
                int count = rs.getInt("count");
                switch (status) {
                    case "PENDING":
                        stats[0] = count;
                        break;
                    case "APPROVED":
                        stats[1] = count;
                        break;
                    case "REJECTED":
                        stats[2] = count;
                        break;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting approval statistics: " + e.getMessage());
        }
        return stats;
    }

    public static List<String> getAllTeacherNames() {
        List<String> teacherNames = new ArrayList<>();
        String query = "SELECT name FROM users WHERE role = 'tentor' ORDER BY name";

        System.out.println("Getting teacher names from database...");

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                teacherNames.add(name);
                System.out.println("Found teacher: " + name);
            }

            System.out.println("Total teachers found: " + teacherNames.size());

        } catch (SQLException e) {
            System.err.println("Error getting teacher names: " + e.getMessage());
            e.printStackTrace();
        }

        return teacherNames;
    }

    // Method untuk mengecek konflik jadwal
    //PERBAIKAN 2: Method hasScheduleConflict disesuaikan dengan kolom 'teacher'
    public static boolean hasScheduleConflict(String teacherName, LocalDate date,
                                              LocalTime startTime, LocalTime endTime) {
        String query = """
            SELECT COUNT(*) FROM schedules 
            WHERE teacher = ? AND date = ? AND (
                (start_time <= ? AND end_time > ?) OR
                (start_time < ? AND end_time >= ?) OR
                (start_time >= ? AND end_time <= ?)
            )
        """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, teacherName);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.setString(3, startTime.toString());
            pstmt.setString(4, startTime.toString());
            pstmt.setString(5, endTime.toString());
            pstmt.setString(6, endTime.toString());
            pstmt.setString(7, startTime.toString());
            pstmt.setString(8, endTime.toString());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error checking schedule conflict: " + e.getMessage());
        }

        return false;
    }

    private static boolean tableExists(String tableName) {
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"});
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking table existence: " + e.getMessage());
            return false;
        }
    }

    public static boolean checkIfTeachersExist() {
        String query = "SELECT COUNT(*) as count FROM users WHERE role = 'tentor'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("Number of teachers in database: " + count);
                return count > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error checking teachers: " + e.getMessage());
        }

        return false;
    }

    // Update method insertSchedule untuk menerima LocalTime
    public static boolean insertSchedule(String subject, String teacherName, String className,
                                         LocalDate date, LocalTime startTime, LocalTime endTime, String room) {
        String query = """
            INSERT INTO schedules (subject, teacher, class_name, date, start_time, end_time, room) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        System.out.println("=== DEBUG INSERT SCHEDULE ===");
        System.out.println("Subject: " + subject);
        System.out.println("Teacher: " + teacherName);
        System.out.println("Class: " + className);
        System.out.println("Date: " + date);
        System.out.println("Start Time: " + startTime);
        System.out.println("End Time: " + endTime);
        System.out.println("Room: " + room);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, subject);
            pstmt.setString(2, teacherName);
            pstmt.setString(3, className);
            pstmt.setDate(4, Date.valueOf(date));
            pstmt.setString(5, startTime.toString());
            pstmt.setString(6, endTime.toString());
            pstmt.setString(7, room);

            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected);

            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error inserting schedule: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
