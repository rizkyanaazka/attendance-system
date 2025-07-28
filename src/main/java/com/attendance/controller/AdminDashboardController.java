package com.attendance.controller;

import com.attendance.DatabaseManager;
import com.attendance.SceneManager;
import com.attendance.model.Attendance;
import com.attendance.model.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;




public class AdminDashboardController implements Initializable {

    public Button logoutButton;
    @FXML private Label welcomeLabel;
    @FXML private Label currentTimeLabel;

    public StackPane contentArea;

    // Navigation buttons
    @FXML private Button btnTambahTentor;
    @FXML private Button btnJadwal;
    @FXML private Button btnLaporan;
    @FXML private Button btnApprovalSiswa;

    // Content panels
    @FXML private VBox tambahTentorPanel;
    @FXML private VBox jadwalPanel;
    @FXML private VBox laporanPanel;
    @FXML private VBox approvalSiswaPanel;

    // Tambah Tentor fields
    @FXML private ImageView tentorImageView;
    @FXML private TextField tentorNameField;
    @FXML private ComboBox<String> tentorSubjectField;

    // Jadwal fields
    @FXML private ComboBox<String> subjectComboBox;
    @FXML private ComboBox<String> teacherComboBox;
    @FXML private ComboBox<String> classComboBox;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<LocalTime> startTimeComboBox;
    @FXML private ComboBox<LocalTime> endTimeComboBox;
    @FXML private TextField roomField;

    // Laporan fields
    @FXML private TextField scheduleIdField;
    @FXML private TableView<Attendance> attendanceTable;
    @FXML private TableColumn<Attendance, String> nameColumn;
    @FXML private TableColumn<Attendance, String> statusColumn;
    @FXML private TableColumn<Attendance, String> dateColumn;

    // Tabel siswa pending
    @FXML private TableView<User> pendingStudentsTable;
    @FXML private TableColumn<User, String> pendingNameColumn;
    @FXML private TableColumn<User, String> pendingEmailColumn;
    @FXML private TableColumn<User, String> pendingNimColumn;
    @FXML private TableColumn<User, String> pendingClassColumn;

    // ComboBox untuk pindah kelas
    @FXML private ComboBox<User> approvedStudentsCombo;
    @FXML private ComboBox<String> newClassCombo;

    // Label statistik
    @FXML private Label pendingCountLabel;
    @FXML private Label approvedCountLabel;
    @FXML private Label rejectedCountLabel;

    //Tempat menyimpan gambar
    private User currentUser;
    private String tentorImagePath;
    private final String UPLOAD_DIR = "uploads/profiles/";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize table columns
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStudentName()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        dateColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDate().toString()));

        // Initialize clock
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalTime now = LocalTime.now();
            currentTimeLabel.setText(now.format(formatter));
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // Create upload directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create upload directory: " + e.getMessage());
        }

        // Initialize approval table columns
        if (pendingNameColumn != null) {
            pendingNameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
            pendingEmailColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
            pendingNimColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNim()));
            pendingClassColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getClassName()));
        }

        // Load initial data
        loadApprovalData();

        // Set initial active panel
        setActivePanel("tentor");
        updateNavigationButtons("tentor");

        setupDropdowns();
        setupTimeValidation();
    }

    private void setupDropdowns() {
        // Setup mata pelajaran tentor (untuk form tambah tentor)
        tentorSubjectField.getItems().addAll("TKP", "TIU", "TWK", "MTK");

        // Setup mata pelajaran jadwal (hanya 4 pilihan)
        subjectComboBox.getItems().addAll("TIU", "TKP", "TWK", "MTK");

        // Setup kelas (hanya A, B, C)
        classComboBox.getItems().addAll("Kelas A", "Kelas B", "Kelas C");

        // Setup nama tentor dari database
        loadTeacherNames();

        // Setup waktu (07:00 - 21:00 dengan interval 30 menit)
        setupTimeDropdowns();
    }

    private void loadTeacherNames() {
        try {
            List<String> teacherNames = DatabaseManager.getAllTeacherNames();
            teacherComboBox.getItems().clear();
            teacherComboBox.getItems().addAll(teacherNames);
        } catch (Exception e) {
            System.err.println("Error loading teacher names: " + e.getMessage());
            alert(Alert.AlertType.ERROR, "Gagal memuat daftar tentor dari database");
        }
    }

    private void setupTimeDropdowns() {
        ObservableList<LocalTime> timeOptions = FXCollections.observableArrayList();

        // Generate waktu dari 07:00 sampai 21:00 dengan interval 30 menit
        for (int hour = 5; hour <= 21; hour++) {
            timeOptions.add(LocalTime.of(hour, 0));
            if (hour < 21) { // Di atas jam 9 terlalu malam
                timeOptions.add(LocalTime.of(hour, 30));
            }
        }

        startTimeComboBox.setItems(timeOptions);
        endTimeComboBox.setItems(timeOptions);
    }

    private void setupTimeValidation() {
        startTimeComboBox.setOnAction(e -> {
            LocalTime startTime = startTimeComboBox.getValue();
            if (startTime != null) {
                // Filter jam selesai menampilkan waktu setelah jam mulai
                ObservableList<LocalTime> validEndTimes = FXCollections.observableArrayList();

                for (int hour = 7; hour <= 21; hour++) {
                    LocalTime time1 = LocalTime.of(hour, 0);
                    LocalTime time2 = LocalTime.of(hour, 30);

                    // Hanya tambahkan waktu yang setelah jam mulai
                    if (time1.isAfter(startTime)) {
                        validEndTimes.add(time1);
                    }
                    if (time2.isAfter(startTime) && hour < 21) {
                        validEndTimes.add(time2);
                    }
                }

                endTimeComboBox.setItems(validEndTimes);

                // Reset pilihan jam selesai jika tidak valid lagi
                LocalTime currentEndTime = endTimeComboBox.getValue();
                if (currentEndTime != null && !currentEndTime.isAfter(startTime)) {
                    endTimeComboBox.setValue(null);
                }

                endTimeComboBox.setPromptText("Pilih jam selesai (setelah " + startTime + ")");
            }
        });

        // Validasi tambahan saat jam selesai dipilih
        endTimeComboBox.setOnAction(e -> {
            LocalTime startTime = startTimeComboBox.getValue();
            LocalTime endTime = endTimeComboBox.getValue();

            if (startTime != null && endTime != null) {
                if (!endTime.isAfter(startTime)) {
                    alert(Alert.AlertType.WARNING, "Jam selesai harus setelah jam mulai!");
                    endTimeComboBox.setValue(null);
                }
            }
        });
    }


    public void setCurrentUser(User u) {
        currentUser = u;
        welcomeLabel.setText("Selamat datang, " + u.getName() + " (Admin)");
    }

    // Sidebar
    @FXML
    private void showApprovalSiswa() {
        setActivePanel("approval");
        updateNavigationButtons("approval");
        loadApprovalData();
    }

    @FXML
    private void showTambahTentor() {
        setActivePanel("tentor");
        updateNavigationButtons("tentor");
    }

    @FXML
    private void showJadwal() {
        setActivePanel("jadwal");
        updateNavigationButtons("jadwal");
    }

    @FXML
    private void showLaporan() {
        setActivePanel("laporan");
        updateNavigationButtons("laporan");
    }

    private void setActivePanel(String panelName) {
        // Sembunyikan tampilan sidebar yang tidak ditekan
        tambahTentorPanel.setVisible(false);
        jadwalPanel.setVisible(false);
        laporanPanel.setVisible(false);
        if (approvalSiswaPanel != null) {
            approvalSiswaPanel.setVisible(false);
        }

        // Show selected panel
        switch (panelName) {
            case "tentor":
                tambahTentorPanel.setVisible(true);
                break;
            case "jadwal":
                jadwalPanel.setVisible(true);
                break;
            case "laporan":
                laporanPanel.setVisible(true);
                break;
            case "approval":
                if (approvalSiswaPanel != null) {
                    approvalSiswaPanel.setVisible(true);
                }
                break;
        }
    }

    private void updateNavigationButtons(String activePanel) {
        // Reset all button styles
        btnTambahTentor.getStyleClass().removeAll("sidebar-button-active");
        btnJadwal.getStyleClass().removeAll("sidebar-button-active");
        btnLaporan.getStyleClass().removeAll("sidebar-button-active");
        if (btnApprovalSiswa != null) {
            btnApprovalSiswa.getStyleClass().removeAll("sidebar-button-active");
        }
        // Set active button style
        switch (activePanel) {
            case "tentor":
                btnTambahTentor.getStyleClass().add("sidebar-button-active");
                break;
            case "jadwal":
                btnJadwal.getStyleClass().add("sidebar-button-active");
                break;
            case "laporan":
                btnLaporan.getStyleClass().add("sidebar-button-active");
                break;
            case "approval":
                if (btnApprovalSiswa != null) {
                    btnApprovalSiswa.getStyleClass().add("sidebar-button-active");
                }
                break;
        }
    }

    // Method untuk load data approval
    private void loadApprovalData() {
        loadPendingStudents();
        loadApprovedStudents();
        loadAvailableClasses();
        loadApprovalStatistics();
    }

    private void loadPendingStudents() {
        if (pendingStudentsTable != null) {
            List<User> pendingStudents = DatabaseManager.getPendingStudents();
            ObservableList<User> data = FXCollections.observableArrayList(pendingStudents);
            pendingStudentsTable.setItems(data);
        }
    }

    private void loadApprovedStudents() {
        if (approvedStudentsCombo != null) {
            List<User> approvedStudents = DatabaseManager.getApprovedStudents();
            ObservableList<User> data = FXCollections.observableArrayList(approvedStudents);
            approvedStudentsCombo.setItems(data);
        }
    }

    private void loadAvailableClasses() {
        List<String> classes = DatabaseManager.getAvailableClasses();
        ObservableList<String> data = FXCollections.observableArrayList(classes);
        if (newClassCombo != null) {
            newClassCombo.setItems(data);
        }
    }

    private void loadApprovalStatistics() {
        int[] stats = DatabaseManager.getApprovalStatistics();
        if (pendingCountLabel != null) {
            pendingCountLabel.setText(String.valueOf(stats[0]));
        }
        if (approvedCountLabel != null) {
            approvedCountLabel.setText(String.valueOf(stats[1]));
        }
        if (rejectedCountLabel != null) {
            rejectedCountLabel.setText(String.valueOf(stats[2]));
        }
    }

    // Handler untuk approve siswa
    @FXML
    private void handleApproveStudent() {
        User selectedStudent = pendingStudentsTable.getSelectionModel().getSelectedItem();
        if (selectedStudent == null) {
            alert(Alert.AlertType.WARNING, "Pilih siswa yang akan di-approve.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Approval");
        confirmAlert.setHeaderText("Approve Siswa");
        confirmAlert.setContentText("Apakah Anda yakin ingin meng-approve siswa " + selectedStudent.getName() + "?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = DatabaseManager.approveStudent(selectedStudent.getId(), currentUser.getId());
            if (success) {
                alert(Alert.AlertType.INFORMATION, "Siswa berhasil di-approve!");
                loadApprovalData();
            } else {
                alert(Alert.AlertType.ERROR, "Gagal approve siswa.");
            }
        }
    }

    // Handler untuk reject siswa
    @FXML
    private void handleRejectStudent() {
        User selectedStudent = pendingStudentsTable.getSelectionModel().getSelectedItem();
        if (selectedStudent == null) {
            alert(Alert.AlertType.WARNING, "Pilih siswa yang akan di-reject.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Rejection");
        confirmAlert.setHeaderText("Reject Siswa");
        confirmAlert.setContentText("Apakah Anda yakin ingin meng-reject siswa " + selectedStudent.getName() + "?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = DatabaseManager.rejectStudent(selectedStudent.getId(), currentUser.getId());
            if (success) {
                alert(Alert.AlertType.INFORMATION, "Siswa berhasil di-reject!");
                loadApprovalData();
            } else {
                alert(Alert.AlertType.ERROR, "Gagal reject siswa.");
            }
        }
    }

    // Handler untuk update kelas siswa yang sudah approved (pindah kelas)
    @FXML
    private void handleUpdateStudentClass() {
        User selectedStudent = approvedStudentsCombo.getSelectionModel().getSelectedItem();
        String newClass = newClassCombo.getSelectionModel().getSelectedItem();

        if (selectedStudent == null) {
            alert(Alert.AlertType.WARNING, "Pilih siswa yang akan dipindah kelas.");
            return;
        }

        if (newClass == null || newClass.trim().isEmpty()) {
            alert(Alert.AlertType.WARNING, "Pilih kelas baru untuk siswa.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Pindah Kelas");
        confirmAlert.setHeaderText("Update Kelas Siswa");
        confirmAlert.setContentText("Pindahkan siswa " + selectedStudent.getName() +
                " ke kelas " + newClass + "?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = DatabaseManager.updateStudentClass(selectedStudent.getId(), newClass);
            if (success) {
                alert(Alert.AlertType.INFORMATION, "Kelas siswa berhasil diupdate!");
                loadApprovalData();
            } else {
                alert(Alert.AlertType.ERROR, "Gagal update kelas siswa.");
            }
        }
    }

    // Handler untuk refresh data
    @FXML
    private void handleRefreshApprovalData() {
        loadApprovalData();
        alert(Alert.AlertType.INFORMATION, "Data berhasil di-refresh!");
    }

    // Image selection methods
    @FXML
    private void handleSelectTentorImage() {
        File selectedFile = showImageChooser("Pilih Foto Tentor");
        if (selectedFile != null) {
            tentorImagePath = saveImageFile(selectedFile, "tentor");
            if (tentorImagePath != null) {
                tentorImageView.setImage(new Image("file:" + tentorImagePath));
            }
        }
    }

    private File showImageChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        return fileChooser.showOpenDialog(welcomeLabel.getScene().getWindow());
    }

    private String saveImageFile(File sourceFile, String prefix) {
        try {
            String fileName = prefix + "_" + System.currentTimeMillis() + "_" + sourceFile.getName();
            Path targetPath = Paths.get(UPLOAD_DIR, fileName);
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            alert(Alert.AlertType.ERROR, "Gagal menyimpan gambar: " + e.getMessage());
            return null;
        }
    }

    private void showTimePickerDialog(String title, TextField targetField) {
        Dialog<LocalTime> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Pilih waktu");

        // Create time picker components
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 8);
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0);

        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);

        VBox timePickerBox = new VBox(10);
        timePickerBox.getChildren().addAll(
                new Label("Jam:"),
                hourSpinner,
                new Label("Menit:"),
                minuteSpinner
        );

        dialog.getDialogPane().setContent(timePickerBox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
            }
            return null;
        });

        Optional<LocalTime> result = dialog.showAndWait();
        result.ifPresent(time -> targetField.setText(time.format(DateTimeFormatter.ofPattern("HH:mm"))));
    }

    // Action handlers
    @FXML
    private void handleLogout() {
        try {

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            SceneManager.handleLogout(stage);

        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddTentor(ActionEvent e) {
        String name = tentorNameField.getText().trim();
        String subject = tentorSubjectField.getValue();

        if (name.isEmpty() || subject.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Nama & mata pelajaran wajib diisi.");
            return;
        }

        try (Connection c = DatabaseManager.getConnection()) {
            PreparedStatement ps = c.prepareStatement("""
                INSERT INTO users (name, email, password, role, nim, class_name, subject, profile_image)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """);
            ps.setString(1, name);
            ps.setString(2, emailFor(name));
            ps.setString(3, "123456");
            ps.setString(4, "tentor");
            ps.setString(5, "-");
            ps.setString(6, "-");
            ps.setString(7, subject);
            ps.setString(8, tentorImagePath != null ? tentorImagePath : "");
            ps.executeUpdate();

            alert(Alert.AlertType.INFORMATION, "Tentor berhasil ditambahkan!");
            clearTentorFields();
        } catch (SQLException ex) {
            alert(Alert.AlertType.ERROR, "Gagal menambah tentor:\n" + ex.getMessage());
        }
    }

    @FXML
    private void handleCreateSchedule(ActionEvent e) {
        String subject = subjectComboBox.getValue();
        String teacher = teacherComboBox.getValue();
        String kelas = classComboBox.getValue();
        LocalDate date = datePicker.getValue();
        LocalTime startTime = startTimeComboBox.getValue();
        LocalTime endTime = endTimeComboBox.getValue();
        String ruang = roomField.getText().trim();

        // Validation
        if (subject == null || subject.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Pilih mata pelajaran!");
            return;
        }

        if (teacher == null || teacher.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Pilih nama tentor!");
            return;
        }

        if (kelas == null || kelas.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Pilih kelas!");
            return;
        }

        if (date == null) {
            alert(Alert.AlertType.WARNING, "Pilih tanggal!");
            return;
        }

        if (date.isBefore(LocalDate.now())) {
            alert(Alert.AlertType.WARNING, "Tanggal tidak boleh di masa lalu!");
            return;
        }

        if (startTime == null) {
            alert(Alert.AlertType.WARNING, "Pilih waktu mulai!");
            return;
        }

        if (endTime == null) {
            alert(Alert.AlertType.WARNING, "Pilih waktu selesai!");
            return;
        }

        if (ruang.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Masukkan ruangan!");
            return;
        }

        // Cek konflik jadwal
        if (DatabaseManager.hasScheduleConflict(teacher, date, startTime, endTime)) {
            alert(Alert.AlertType.WARNING, "Konflik jadwal! Tentor sudah memiliki jadwal pada waktu tersebut.");
            return;
        }

        try {
            boolean ok = DatabaseManager.insertSchedule(subject, teacher, kelas, date, startTime, endTime, ruang);
            if (ok) {

                alert(Alert.AlertType.INFORMATION,
                        "Jadwal berhasil ditambahkan!\n\n" +
                                "✓ Mata Pelajaran: " + subject + "\n" +
                                "✓ Tentor: " + teacher + "\n" +
                                "✓ Kelas: " + kelas + "\n" +
                                "✓ Tanggal: " + date + "\n" +
                                "✓ Waktu: " + startTime + " - " + endTime + "\n" +
                                "✓ Ruangan: " + ruang + "\n\n"
                    );
                clearScheduleFields();
            } else {
                alert(Alert.AlertType.ERROR, "Gagal menambah jadwal (lihat log).");
            }
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage());
        }
    }

    @FXML
    private void handleViewAttendanceReport(ActionEvent e) {
        String txt = scheduleIdField.getText().trim();
        if (txt.isEmpty()) {
            alert(Alert.AlertType.WARNING, "ID jadwal wajib diisi.");
            return;
        }

        try {
            int id = Integer.parseInt(txt);
            List<Attendance> list = DatabaseManager.getAttendanceBySchedule(id);
            ObservableList<Attendance> data = FXCollections.observableArrayList(list);
            attendanceTable.setItems(data);

            if (list.isEmpty()) {
                alert(Alert.AlertType.INFORMATION, "Tidak ada data kehadiran untuk jadwal ini.");
            }
        } catch (NumberFormatException ex) {
            alert(Alert.AlertType.ERROR, "ID jadwal harus berupa angka.");
        }
    }

    // Helper methods
    private void clearTentorFields() {
        tentorNameField.clear();
        tentorSubjectField.getSelectionModel().clearSelection();
        tentorImagePath = null;
        tentorImageView.setImage(new Image(getClass().getResourceAsStream("/images/default-avatar.png")));
    }

    private void clearScheduleFields() {
        subjectComboBox.getSelectionModel().clearSelection();
        teacherComboBox.getSelectionModel().clearSelection();
        classComboBox.getSelectionModel().clearSelection();
        datePicker.setValue(null);
        startTimeComboBox.getSelectionModel().clearSelection();
        endTimeComboBox.getSelectionModel().clearSelection();
        roomField.clear();

        // Reset dropdown waktu selesai
        setupTimeDropdowns();
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String emailFor(String name) {
        return name.toLowerCase().replaceAll("\\s+", "") + "@email.com";
    }

//  ==== Refresh Start ===
    @FXML
    private void handleRefresh() {
        try {
            // 1. Refresh dropdown data
            refreshDropdowns();

            // 2. Refresh table data
            refreshTables();

            // 3. Clear form fields
            clearAllFields();

            // Tampilkan notifikasi sukses
            alert(Alert.AlertType.INFORMATION, "Data berhasil di-refresh!");

        } catch (Exception e) {
            System.err.println("Error during refresh: " + e.getMessage());
        }
    }

    // Refresh Dropdown
    private void refreshDropdowns() {
        // Refresh mata pelajaran tentor
        tentorSubjectField.getItems().clear();
        tentorSubjectField.getItems().addAll("TKP", "TIU", "TWK", "MTK");

        // Refresh mata pelajaran jadwal
        subjectComboBox.getItems().clear();
        subjectComboBox.getItems().addAll("TIU", "TKP", "TWK", "MTK");

        // Refresh kelas
        classComboBox.getItems().clear();
        classComboBox.getItems().addAll("Kelas A", "Kelas B", "Kelas C");

        // Refresh nama tentor dari database
        loadTeacherNames();

        // Refresh waktu
        setupTimeDropdowns();
    }

    //Refresh tabel
    private void refreshTables() {

        if (attendanceTable != null) {
            attendanceTable.getItems().clear();
        }

        // Refresh data approval (pending students, approved students, statistics)
        if (approvalSiswaPanel != null && approvalSiswaPanel.isVisible()) {
            loadApprovalData();
        }
    }

    //Clear form
    private void clearAllFields() {

        tentorNameField.clear();
        tentorSubjectField.getSelectionModel().clearSelection();
        tentorImagePath = null;
        if (tentorImageView != null) {
            tentorImageView.setImage(new Image(getClass().getResourceAsStream("/images/default-avatar.png")));
        }

        subjectComboBox.getSelectionModel().clearSelection();
        teacherComboBox.getSelectionModel().clearSelection();
        classComboBox.getSelectionModel().clearSelection();
        datePicker.setValue(null);
        startTimeComboBox.getSelectionModel().clearSelection();
        endTimeComboBox.getSelectionModel().clearSelection();
        roomField.clear();

        scheduleIdField.clear();

        if (approvedStudentsCombo != null) {
            approvedStudentsCombo.getSelectionModel().clearSelection();
        }
        if (newClassCombo != null) {
            newClassCombo.getSelectionModel().clearSelection();
        }
        if (pendingStudentsTable != null) {
            pendingStudentsTable.getSelectionModel().clearSelection();
        }
    }
// === Refresh End ===
}