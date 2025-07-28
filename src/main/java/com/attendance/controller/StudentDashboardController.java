package com.attendance.controller;

import com.attendance.DatabaseManager;
import com.attendance.SceneManager;
import com.attendance.model.Schedule;
import com.attendance.model.Attendance;
import com.attendance.model.User;
import com.attendance.model.Notification;
import com.attendance.model.Module;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.StageStyle;

import javax.swing.*;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.stream.Collectors;

public class StudentDashboardController implements Initializable {

    // ==================== DEKLARASI KOMPONEN FXML ====================
    public Button logoutButton;
    @FXML private Label welcomeLabel;          // Label sambutan siswa
    @FXML private Label currentTimeLabel;      // Label waktu real-time
    @FXML private VBox scheduleContainer;      // Container untuk kartu jadwal
    @FXML private Label messageLabel;          // Label untuk pesan notifikasi

    // ==================== SIDEBAR COMPONENTS ====================
    @FXML private Button buttonShowDashboard;  // Tombol menu dashboard
    @FXML private Button buttonShowModules;    // Tombol menu modul
    @FXML private Button buttonShowHistory;    // Tombol menu riwayat
    @FXML private Button buttonRefreshSidebar; // Tombol refresh sidebar

    // ==================== CONTENT SECTIONS ====================
    @FXML private HBox dashboardContent;       // Konten dashboard utama
    @FXML private VBox modulesContent;         // Konten modul
    @FXML private VBox historyContent;         // Konten riwayat lengkap

    // ==================== MODULES COMPONENTS ====================
    @FXML private ComboBox<String> comboBoxSubjectFilter; // Filter mata pelajaran
    @FXML private VBox modulesContainer;       // Container untuk kartu modul

    // ==================== HISTORY COMPONENTS ====================
    @FXML private TableView<Attendance> fullHistoryTable;  // Tabel riwayat lengkap
    @FXML private TableColumn<Attendance, String> fullSubjectColumn;
    @FXML private TableColumn<Attendance, String> fullDateColumn;
    @FXML private TableColumn<Attendance, String> fullTimeColumn;
    @FXML private TableColumn<Attendance, String> fullStatusColumn;
    @FXML private TableColumn<Attendance, String> fullTeacherColumn;

    // ==================== STATISTICS LABELS ====================
    @FXML private Label labelTotalAttendance;  // Total absensi
    @FXML private Label labelPresentCount;     // Jumlah hadir
    @FXML private Label labelLateCount;        // Jumlah terlambat
    @FXML private Label labelAttendanceRate;   // Tingkat kehadiran

    // ==================== NOTIFICATION COMPONENTS ====================
    @FXML private Button notificationButton;   // Tombol notifikasi
    @FXML private Label notificationBadge;     // Badge jumlah notifikasi belum dibaca
    @FXML private VBox notificationPanel;      // Panel notifikasi
    @FXML private VBox notificationContainer;  // Container untuk daftar notifikasi
    @FXML private Button markAllReadButton;    // Tombol tandai semua dibaca

    // ==================== VARIABEL INSTANCE ====================
    private User currentUser;                                      // Data user yang sedang login
    private ObservableList<Attendance> attendanceHistory;         // List riwayat absensi
    private ObservableList<Notification> notifications;           // List notifikasi
    private ObservableList<Module> modules;                       // List modul
    private Timer clockTimer;                                      // Timer untuk jam real-time
    private Timer refreshTimer;                                    // Timer untuk refresh otomatis
    private Timer notificationTimer;                               // Timer untuk refresh notifikasi

//Metode yang otomatis terpanggil
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("StudentDashboardController initialized"); // Hanya debug

        // Inisialisasi list
        attendanceHistory = FXCollections.observableArrayList();
        notifications = FXCollections.observableArrayList();
        modules = FXCollections.observableArrayList();

        setupFullHistoryTable();

        // Memulai jam real-time
        startClock();

        // Kosongkan pesan awal
        messageLabel.setText("");

        // Setup notification badge
        notificationBadge.setVisible(false);

        // Set default active menu
        showDashboard();
    }

// Mengatur Data User Yang Sedang Login
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("Setting current student: " + user.getName()); // Debug log

        // Menampilkan sambutan dengan nama dan kelas siswa
        welcomeLabel.setText("Selamat Datang, " + user.getName() + " (" + user.getClassName() + ")");

        // Memuat data
        loadTodaySchedules();
        loadAttendanceHistory();
        loadNotifications();
        loadModules();
        setupSubjectFilter();
        updateStatistics();

        // Memulai refresh otomatis
        startAutoRefresh();
        startNotificationRefresh();
    }

// Tabel Riwayat Absen
    private void setupFullHistoryTable() {
        fullSubjectColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSubject()));

        fullDateColumn.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDate();
            return new SimpleStringProperty(
                    date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );
        });

        fullTimeColumn.setCellValueFactory(cellData -> {
            LocalTime time = cellData.getValue().getTimeIn();
            return new SimpleStringProperty(
                    time.format(DateTimeFormatter.ofPattern("HH:mm"))
            );
        });

        fullStatusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus()));

        fullTeacherColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty("Tentor")); // Placeholder

        fullStatusColumn.setCellFactory(column -> new TableCell<Attendance, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("status-hadir", "status-terlambat", "status-tidak-hadir");
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status);
                    switch (status) {
                        case "Hadir":
                            getStyleClass().add("status-hadir");
                            break;
                        case "Terlambat":
                            getStyleClass().add("status-terlambat");
                            break;
                        case "Tidak Hadir":
                            getStyleClass().add("status-tidak-hadir");
                            break;
                    }
                }
            }
        });

        fullHistoryTable.setItems(attendanceHistory);
    }

// Filter Mata Pelajaran pada Modul
    private void setupSubjectFilter() {
        if (currentUser == null) return;

        List<String> subjects = modules.stream()
                .map(Module::getSubject)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        ObservableList<String> subjectList = FXCollections.observableArrayList();
        subjectList.add("Semua Mata Pelajaran");
        subjectList.addAll(subjects);

        comboBoxSubjectFilter.setItems(subjectList);
        comboBoxSubjectFilter.setValue("Semua Mata Pelajaran");
    }

// Ringkasan informasi Kehadiran Siswa
    private void updateStatistics() {
        if (attendanceHistory.isEmpty()) {
            labelTotalAttendance.setText("0");
            labelPresentCount.setText("0");
            labelLateCount.setText("0");
            labelAttendanceRate.setText("0%");
            return;
        }

        long totalAttendance = attendanceHistory.size();
        long presentCount = attendanceHistory.stream().filter(a -> "Hadir".equals(a.getStatus())).count();
        long lateCount = attendanceHistory.stream().filter(a -> "Terlambat".equals(a.getStatus())).count();

        double attendanceRate = totalAttendance > 0 ?
                ((double)(presentCount + lateCount) / totalAttendance) * 100 : 0;

        labelTotalAttendance.setText(String.valueOf(totalAttendance));
        labelPresentCount.setText(String.valueOf(presentCount));
        labelLateCount.setText(String.valueOf(lateCount));
        labelAttendanceRate.setText(Math.round(attendanceRate) + "%");
    }

// Jam Real Time
    private void startClock() {
        clockTimer = new Timer();
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    LocalTime now = LocalTime.now();
                    LocalDate today = LocalDate.now();
                    // Format tampilan: "Waktu Sekarang: HH:mm:ss | dd/MM/yyyy"
                    currentTimeLabel.setText("Waktu Sekarang: " +
                            now.format(DateTimeFormatter.ofPattern("HH:mm:ss")) +
                            " | " + today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                });
            }
        }, 0, 1000); // Mulai segera, update setiap 1000ms (1 detik)
    }

// Memulai refresh otomatis jadwal setiap 30 detik
    private void startAutoRefresh() {
        // Hentikan timer sebelumnya jika ada
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Update UI di JavaFX Application Thread
                Platform.runLater(() -> {
                    loadTodaySchedules();
                });
            }
        }, 30000, 30000); // Tunggu 30 detik, lalu update setiap 30 detik
    }

// Memulai refresh otomatis notifikasi setiap 15 detik
    private void startNotificationRefresh() {
        if (notificationTimer != null) {
            notificationTimer.cancel();
        }
        notificationTimer = new Timer();
        notificationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    loadNotifications();
                });
            }
        }, 15000, 15000); // Update setiap 15 detik
    }

// Memuat dan menampilkan jadwal hari ini berdasarkan kelas siswa
    private void loadTodaySchedules() {
        // Cek apakah user sudah login
        if (currentUser == null) return;

        // Kosongkan container jadwal
        scheduleContainer.getChildren().clear();

        LocalDate today = LocalDate.now();
        var schedules = DatabaseManager.getSchedulesByClass(currentUser.getClassName(), today);

        System.out.println("Loading schedules for class: " + currentUser.getClassName() +
                ", found: " + schedules.size()); // Debug log

        // Jika tidak ada jadwal hari ini
        if (schedules.isEmpty()) {
            Label noScheduleLabel = new Label("Tidak ada jadwal hari ini");
            noScheduleLabel.getStyleClass().add("no-schedule");
            scheduleContainer.getChildren().add(noScheduleLabel);
            return;
        }

        // Buat kartu untuk setiap jadwal
        for (Schedule schedule : schedules) {
            VBox scheduleCard = createScheduleCard(schedule);
            scheduleContainer.getChildren().add(scheduleCard);
        }
    }

// Membuat kartu jadwal untuk setiap mata pelajaran
    private VBox createScheduleCard(Schedule schedule) {
        VBox card = new VBox(10);
        card.getStyleClass().add("schedule-card");
        Label subjectLabel = new Label(schedule.getSubject());
        subjectLabel.getStyleClass().add("subject-label");
        Label teacherLabel = new Label("Pengajar: " + schedule.getTeacher());
        Label timeLabel = new Label("Waktu: " +
                schedule.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                " - " +
                schedule.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        Label roomLabel = new Label("Ruangan: " + schedule.getRoom());

        Button attendButton = new Button("ABSEN");
        attendButton.getStyleClass().add("attend-button");

        // ================== LOGIKA GABUNGAN YANG AKURAT ==================
        boolean isClassActive = schedule.isActive();
        boolean alreadyAttended = DatabaseManager.checkAttendanceExists(currentUser.getId(), schedule.getId());

        if (alreadyAttended) {
            // Jika sudah absen, tombol selalu nonaktif.
            attendButton.setText("SUDAH ABSEN");
            attendButton.setDisable(true);
            attendButton.getStyleClass().setAll("button", "attended");

        } else if (!isClassActive) {
            // Jika belum absen DAN kelas belum aktif, tombol nonaktif.
            attendButton.setText("KELAS BELUM DIMULAI");
            attendButton.setDisable(true);
            attendButton.getStyleClass().setAll("button", "not-available");

        } else {
            // elas sudah aktif dan siswa belum absen.
            LocalTime now = LocalTime.now();
            LocalTime attendanceStart = schedule.getStartTime().minusMinutes(15);
            LocalTime attendanceEnd = schedule.getStartTime().plusMinutes(30);

            if (now.isAfter(attendanceStart) && now.isBefore(attendanceEnd)) {
                // Jika dalam rentang waktu yang tepat
                attendButton.setText("ABSEN SEKARANG");
                attendButton.setDisable(false);
                attendButton.getStyleClass().setAll("button", "attend-button");
                attendButton.setOnAction(e -> handleAttendance(schedule, attendButton));
            } else {
                // Jika di luar rentang waktu
                if (now.isBefore(attendanceStart)) {
                    long minutesUntil = Duration.between(now, attendanceStart).toMinutes();
                    attendButton.setText("TERLALU AWAL (" + minutesUntil + " menit lagi)");
                } else { // now.isAfter(attendanceEnd)
                    attendButton.setText("WAKTU HABIS");
                }
                attendButton.setDisable(true);
                attendButton.getStyleClass().setAll("button", "not-available");
            }
        }
        card.getChildren().addAll(subjectLabel, teacherLabel, timeLabel, roomLabel, attendButton);
        return card;
    }

// Menangani proses absensi ketika tombol absen ditekan
    private void handleAttendance(Schedule schedule, Button button) {
        LocalTime now = LocalTime.now();
        String status = "Hadir";

        // Siswa dianggap terlambat jika absen lebih dari 5 menit setelah kelas dimulai
        if (now.isAfter(schedule.getStartTime().plusMinutes(5))) {
            status = "Terlambat";
        }

        // Buat objek absensi
        Attendance attendance = new Attendance(
                currentUser.getId(),
                schedule.getId(),
                LocalDate.now(),
                now,
                status
        );

        // Disable tombol sementara untuk mencegah double-click
        button.setDisable(true);
        button.setText("Memproses...");

        // Proses absensi di background thread
        String finalStatus = status;
        new Thread(() -> {
            boolean success = DatabaseManager.addAttendance(attendance);

            Platform.runLater(() -> {
                if (success) {
                    // Jika berhasil
                    button.setText("SUDAH ABSEN");
                    button.getStyleClass().remove("attend-button");
                    button.getStyleClass().add("attended");
                    showMessage("Absensi berhasil dicatat! Status: " + finalStatus, "success");
                    loadAttendanceHistory(); // Refresh riwayat absensi
                    updateStatistics(); // Update statistik
                } else {
                    // Jika gagal
                    button.setDisable(false);
                    button.setText("ABSEN");
                    showMessage("Gagal mencatat absensi! Coba lagi.", "error");
                }
            });
        }).start();
    }

// Memuat riwayat absensi user dari database
    private void loadAttendanceHistory() {
        if (currentUser == null) return;

        new Thread(() -> {
            var history = DatabaseManager.getUserAttendanceHistory(currentUser.getId());

            Platform.runLater(() -> {
                attendanceHistory.clear();
                attendanceHistory.addAll(history);
                updateStatistics();
            });
        }).start();
    }

// Memuat modul dari database
    private void loadModules() {
        if (currentUser == null) return;

        new Thread(() -> {
            List<Module> moduleList = DatabaseManager.getModulesByClass(currentUser.getClassName());

            Platform.runLater(() -> {
                modules.clear();
                modules.addAll(moduleList);
                refreshModulesContainer();
                setupSubjectFilter();
            });
        }).start();
    }

//Refresh container modul
    private void refreshModulesContainer() {
        modulesContainer.getChildren().clear();

        String selectedSubject = comboBoxSubjectFilter.getValue();
        List<Module> filteredModules = modules;

        if (selectedSubject != null && !"Semua Mata Pelajaran".equals(selectedSubject)) {
            filteredModules = modules.stream()
                    .filter(m -> m.getSubject().equals(selectedSubject))
                    .collect(Collectors.toList());
        }

        if (filteredModules.isEmpty()) {
            Label noModuleLabel = new Label("Tidak ada modul tersedia");
            noModuleLabel.getStyleClass().add("no-notification");
            modulesContainer.getChildren().add(noModuleLabel);
            return;
        }

        for (Module module : filteredModules) {
            VBox moduleCard = createModuleCard(module);
            modulesContainer.getChildren().add(moduleCard);
        }
    }

// Membuat kartu modul
    private VBox createModuleCard(Module module) {
        VBox card = new VBox(10);
        card.getStyleClass().add("module-card");

        // Header dengan judul dan mata pelajaran
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(module.getTitle());
        titleLabel.getStyleClass().add("module-title");

        Label subjectLabel = new Label(module.getSubject());
        subjectLabel.getStyleClass().add("module-subject");

        header.getChildren().addAll(titleLabel, subjectLabel);

        // Deskripsi
        Label descriptionLabel = new Label(
                module.getDescription() != null ? module.getDescription() : "Tidak ada deskripsi"
        );
        descriptionLabel.getStyleClass().add("module-description");
        descriptionLabel.setWrapText(true);

        // Info file
        HBox fileInfo = new HBox(15);
        fileInfo.setAlignment(Pos.CENTER_LEFT);

        Label fileNameLabel = new Label("📄 " + module.getFileName());
        fileNameLabel.getStyleClass().add("module-filename");

        Label fileSizeLabel = new Label(module.getFormattedFileSize());
        fileSizeLabel.getStyleClass().add("module-filesize");

        Label uploadDateLabel = new Label("📅 " +
                module.getUploadedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        uploadDateLabel.getStyleClass().add("module-date");

        fileInfo.getChildren().addAll(fileNameLabel, fileSizeLabel, uploadDateLabel);

        // Action buttons
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        Button downloadButton = new Button("📥 Download");
        downloadButton.getStyleClass().add("primary-button");
        downloadButton.setOnAction(e -> downloadModule(module));

        Button viewButton = new Button("👁️ Lihat");
        viewButton.getStyleClass().add("secondary-button");
        viewButton.setOnAction(e -> viewModule(module));

        actionBox.getChildren().addAll(downloadButton, viewButton);

        card.getChildren().addAll(header, descriptionLabel, fileInfo, actionBox);
        return card;
    }

// Download modul
    private void downloadModule(Module module) {
        try {
            File sourceFile = new File(module.getFilePath());
            if (!sourceFile.exists()) {
                showMessage("File tidak ditemukan di server!", "error");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Simpan File");
                fileChooser.setInitialFileName(module.getFileName());

                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("All Files", "*.*");
                fileChooser.getExtensionFilters().add(extFilter);

                File destFile = fileChooser.showSaveDialog(null); // atau window/scene yang aktif
                if (destFile != null) {
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    showMessage("File berhasil didownload ke: " + destFile.getAbsolutePath(), "success");
                }

            } else {
                showMessage("Tidak dapat membuka file secara otomatis", "error");
            }

        } catch (IOException e) {
            System.err.println("Error opening file: " + e.getMessage());
            showMessage("Gagal membuka file: " + e.getMessage(), "error");
        }
    }

    private void viewModule(Module module) {
        try {
            File sourceFile = new File(module.getFilePath());
            if (!sourceFile.exists()) {
                showMessage("File tidak ditemukan di server!", "error");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(sourceFile);
                showMessage("File berhasil dibuka: " + module.getFileName(), "success");
            } else {
                showMessage("Tidak dapat membuka file secara otomatis", "error");
            }

        } catch (IOException e) {
            System.err.println("Error opening file: " + e.getMessage());
            showMessage("Gagal membuka file: " + e.getMessage(), "error");
        }
    }

// ==================== NOTIFICATION METHODS ====================
    private void loadNotifications() {
        if (currentUser == null) return;

        new Thread(() -> {
            List<Notification> notificationList = DatabaseManager.getNotificationsByClass(currentUser.getClassName());
            int unreadCount = DatabaseManager.getUnreadNotificationCount(currentUser.getClassName());

            Platform.runLater(() -> {
                notifications.clear();
                notifications.addAll(notificationList);
                updateNotificationBadge(unreadCount);
                refreshNotificationContainer();
            });
        }).start();
    }

// Update badge notifikasi
    private void updateNotificationBadge(int unreadCount) {
        if (unreadCount > 0) {
            notificationBadge.setText(String.valueOf(unreadCount));
            notificationBadge.setVisible(true);
            notificationButton.getStyleClass().add("has-notifications");
        } else {
            notificationBadge.setVisible(false);
            notificationButton.getStyleClass().remove("has-notifications");
        }
    }

// Refresh container notifikasi
    private void refreshNotificationContainer() {
        notificationContainer.getChildren().clear();

        if (notifications.isEmpty()) {
            Label noNotificationLabel = new Label("Tidak ada notifikasi");
            noNotificationLabel.getStyleClass().add("no-notification");
            notificationContainer.getChildren().add(noNotificationLabel);
            return;
        }

        for (Notification notification : notifications) {
            VBox notificationCard = createNotificationCard(notification);
            notificationContainer.getChildren().add(notificationCard);
        }
    }

// Membuat kartu notifikasi
    private VBox createNotificationCard(Notification notification) {
        VBox card = new VBox(8);
        card.getStyleClass().add("notification-card");

        if (!notification.isRead()) {
            card.getStyleClass().add("unread");
        }

        Label messageLabel =  new Label(notification.getMessage());
        messageLabel.getStyleClass().add("notification-message");
        messageLabel.setWrapText(true);

        Label timeLabel = new Label(notification.getCreatedAt().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        ));
        timeLabel.getStyleClass().add("notification-time");

        HBox actionBox = new HBox(10);

        if (!notification.isRead()) {
            Button markReadButton = new Button("Tandai Dibaca");
            markReadButton.getStyleClass().add("mark-read-button");
            markReadButton.setOnAction(e -> markNotificationAsRead(notification));
            actionBox.getChildren().add(markReadButton);
        }

        card.getChildren().addAll(messageLabel, timeLabel, actionBox);
        return card;
    }

// Tandai notifikasi sebagai dibaca
    private void markNotificationAsRead(Notification notification) {
        new Thread(() -> {
            boolean success = DatabaseManager.markNotificationAsRead(notification.getId());
            if (success) {
                Platform.runLater(() -> {
                    notification.setRead(true);
                    loadNotifications(); // Refresh
                });
            }
        }).start();
    }

    // ==================== SIDEBAR NAVIGATION ====================

// Tampilkan dashboard
    private void showDashboard() {
        dashboardContent.setVisible(true);
        dashboardContent.setManaged(true);
        modulesContent.setVisible(false);
        modulesContent.setManaged(false);
        historyContent.setVisible(false);
        historyContent.setManaged(false);

        updateSidebarButtonStyles(buttonShowDashboard);
    }

// Tampilkan modul
    private void showModules() {
        dashboardContent.setVisible(false);
        dashboardContent.setManaged(false);
        modulesContent.setVisible(true);
        modulesContent.setManaged(true);
        historyContent.setVisible(false);
        historyContent.setManaged(false);

        updateSidebarButtonStyles(buttonShowModules);
        loadModules();
    }

// Tampilkan riwayat lengkap
    private void showHistory() {
        dashboardContent.setVisible(false);
        dashboardContent.setManaged(false);
        modulesContent.setVisible(false);
        modulesContent.setManaged(false);
        historyContent.setVisible(true);
        historyContent.setManaged(true);

        updateSidebarButtonStyles(buttonShowHistory);
        updateStatistics();
    }

// Update style tombol sidebar
    private void updateSidebarButtonStyles(Button activeButton) {
        // Reset semua tombol
        buttonShowDashboard.getStyleClass().removeAll("sidebar-button-active");
        buttonShowModules.getStyleClass().removeAll("sidebar-button-active");
        buttonShowHistory.getStyleClass().removeAll("sidebar-button-active");

        // Set tombol aktif
        activeButton.getStyleClass().add("sidebar-button-active");
    }

// ==================== EVENT HANDLERS ====================

//menu dashboard
    @FXML
    private void handleShowDashboard() {
        showDashboard();
    }

// menu modul
    @FXML
    private void handleShowModules() {
        showModules();
    }

//menu riwayat
    @FXML
    private void handleShowHistory() {
        showHistory();
    }

// filter mata pelajaran
    @FXML
    private void handleSubjectFilter() {
        refreshModulesContainer();
    }

// Refresh Modul
    @FXML
    private void handleRefreshModules() {
        loadModules();
        showMessage("Daftar modul berhasil diperbarui!", "success");
    }

// Handler untuk menampilkan panel notifikasi
    @FXML
    private void handleShowNotifications() {
        boolean isVisible = notificationPanel.isVisible();

        notificationPanel.setVisible(!isVisible);
        notificationPanel.setManaged(!isVisible);

        if (!isVisible) {
            loadNotifications(); // Refresh saat dibuka
        }
    }

// Handler Mengatur tutupnya notifikasi
    @FXML
    private void handleCloseNotifications() {
        notificationPanel.setVisible(false);
        notificationPanel.setManaged(false);
    }

    @FXML
    private void handleMarkAllAsRead() {
        if (currentUser == null) return;

        new Thread(() -> {
            boolean success = DatabaseManager.markAllNotificationsAsRead(currentUser.getClassName());
            if (success) {
                Platform.runLater(() -> {
                    loadNotifications(); // Refresh
                    showMessage("Semua notifikasi telah ditandai sebagai dibaca", "success");
                });
            }
        }).start();
    }

    @FXML
    private void handleRefresh() {
        loadTodaySchedules();
        loadAttendanceHistory();
        loadNotifications();
        loadModules();
        showMessage("Data berhasil diperbarui!", "success");
    }


    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().clear();
        messageLabel.getStyleClass().add("message");
        messageLabel.getStyleClass().add(type.equals("success") ? "success" : "error");

        // Hapus pesan setelah 3 detik
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    // Cek apakah pesan masih sama (untuk menghindari menghapus pesan baru)
                    if (messageLabel.getText().equals(message)) {
                        messageLabel.setText("");
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @FXML
    private void handleLogout() {
        try {
            // Hentikan semua timer
            if (clockTimer != null) {
                clockTimer.cancel();
            }
            if (refreshTimer != null) {
                refreshTimer.cancel();
            }
            if (notificationTimer != null) {
                notificationTimer.cancel();
            }

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            SceneManager.handleLogout(stage);

        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

}

