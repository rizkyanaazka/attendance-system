package com.attendance.controller;

import com.attendance.DatabaseManager;
import com.attendance.SceneManager;
import com.attendance.model.Attendance;
import com.attendance.model.Schedule;
import com.attendance.model.User;
import com.attendance.model.Module;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class TentorDashboardController {
    public Button logoutButton;
    @FXML private Label labelTentorName;

    // === TABEL JADWAL ===
    @FXML private TableView<Schedule> tableSchedule;
    @FXML private TableColumn<Schedule, String> columnSubjectSchedule;
    @FXML private TableColumn<Schedule, String> columnClassName;
    @FXML private TableColumn<Schedule, String> columnDate;
    @FXML private TableColumn<Schedule, String> columnTime;
    @FXML private TableColumn<Schedule, String> columnRoom;
    @FXML private TableColumn<Schedule, String> columnStatus;
    @FXML private TableColumn<Schedule, Void> columnActions;

    // === TABEL PRESENSI ===
    @FXML private TableView<Attendance> tableAttendance;
    @FXML private TableColumn<Attendance, String> columnStudentName;
    @FXML private TableColumn<Attendance, String> columnStudentNim;
    @FXML private TableColumn<Attendance, String> columnSubjectAttendance;
    @FXML private TableColumn<Attendance, String> columnStatusAttendance;
    @FXML private TableColumn<Attendance, LocalDate> columnDateAttendance;

    // === TABEL MODUL ===
    @FXML private TableView<Module> tableModules;
    @FXML private TableColumn<Module, String> columnModuleTitle;
    @FXML private TableColumn<Module, String> columnModuleClass;
    @FXML private TableColumn<Module, String> columnModuleFile;
    @FXML private TableColumn<Module, String> columnModuleSize;
    @FXML private TableColumn<Module, String> columnModuleDate;
    @FXML private TableColumn<Module, Void> columnModuleActions;

    // === KONTROL TAMPILAN ===
    @FXML private VBox mainContent;
    @FXML private VBox scheduleSection;
    @FXML private VBox attendanceSection;
    @FXML private ScrollPane moduleSection;
    @FXML private Button buttonShowSchedule;
    @FXML private Button buttonShowAttendance;
    @FXML private Button buttonShowModules;

    // === UPLOAD FORM ===
    @FXML private TextField textFieldModuleTitle;
    @FXML private ComboBox<String> comboBoxTargetClass;
    @FXML private TextArea textAreaModuleDescription;
    @FXML private Button buttonSelectFile;
    @FXML private Button buttonUploadModule;
    @FXML private Label labelSelectedFile;

    private final ObservableList<Schedule> scheduleList = FXCollections.observableArrayList();
    private final ObservableList<Module> moduleList = FXCollections.observableArrayList();
    private User currentUser;
    private final ObservableList<Integer> finishedClasses = FXCollections.observableArrayList();
    private final ObservableList<Integer> activeClasses = FXCollections.observableArrayList();
    private File selectedFile;

    // Directory untuk menyimpan file upload
    private static final String UPLOAD_DIRECTORY = "uploads/modules/";

    public void setCurrentUser(User user) {
        this.currentUser = user;
        labelTentorName.setText("Selamat datang, " + user.getName());

        // Initialize upload directory
        createUploadDirectory();

        // Load data
        loadTodaySchedule(user.getName());
        loadAttendanceData(user.getSubject());
        loadModules();
        loadClassStates();
        setupModuleTable();
        setupTargetClassComboBox();

        System.out.println(">>> setCurrentUser() DIPANGGIL");
    }

    private void createUploadDirectory() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Upload directory created: " + UPLOAD_DIRECTORY);
            }
        } catch (IOException e) {
            System.err.println("Error creating upload directory: " + e.getMessage());
        }
    }

    private void setupTargetClassComboBox() {
        ObservableList<String> classes = FXCollections.observableArrayList(
                "Kelas A", "Kelas B", "Kelas C"
        );
        comboBoxTargetClass.setItems(classes);
    }

    private void setupModuleTable() {
        columnModuleTitle.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTitle()));
        columnModuleClass.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getClassName()));
        columnModuleFile.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getFileName()));
        columnModuleSize.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getFormattedFileSize()));
        columnModuleDate.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getUploadedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        ));

        columnModuleActions.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button("🗑️ Hapus");

            {
                deleteButton.getStyleClass().addAll("end-button", "secondary-button");
                deleteButton.setOnAction(e -> {
                    Module module = getTableView().getItems().get(getIndex());
                    deleteModule(module);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                }
            }
        });

        tableModules.setItems(moduleList);
    }

    private void loadModules() {
        if (currentUser != null) {
            List<Module> modules = DatabaseManager.getModulesBySubject(currentUser.getSubject());
            moduleList.clear();
            moduleList.addAll(modules);
            System.out.println("Loaded " + modules.size() + " modules for subject: " + currentUser.getSubject());
        }
    }

    @FXML
    private void handleSelectFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih File Modul");

        FileChooser.ExtensionFilter allFiles = new FileChooser.ExtensionFilter("All Files", "*.*");
        FileChooser.ExtensionFilter pdfFiles = new FileChooser.ExtensionFilter("PDF Files", "*.pdf");
        FileChooser.ExtensionFilter docFiles = new FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx");
        FileChooser.ExtensionFilter pptFiles = new FileChooser.ExtensionFilter("PowerPoint Files", "*.ppt", "*.pptx");
        FileChooser.ExtensionFilter imageFiles = new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg");

        fileChooser.getExtensionFilters().addAll(pdfFiles, docFiles, pptFiles, imageFiles, allFiles);

        Stage stage = (Stage) buttonSelectFile.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            labelSelectedFile.setText("File: " + selectedFile.getName() + " (" + formatFileSize(selectedFile.length()) + ")");
            buttonUploadModule.setDisable(false);
        } else {
            labelSelectedFile.setText("Belum ada file dipilih");
            buttonUploadModule.setDisable(true);
        }
    }

    @FXML
    private void handleUploadModule(ActionEvent event) {
        String title = textFieldModuleTitle.getText().trim();
        String targetClass = comboBoxTargetClass.getValue();
        String description = textAreaModuleDescription.getText().trim();

        if (title.isEmpty()) {
            showError("Judul modul tidak boleh kosong!");
            return;
        }

        if (targetClass == null || targetClass.isEmpty()) {
            showError("Pilih kelas tujuan!");
            return;
        }

        if (selectedFile == null) {
            showError("Pilih file yang akan diupload!");
            return;
        }

        // Check file size (max 50MB)
        long maxSize = 50 * 1024 * 1024; // 50MB
        if (selectedFile.length() > maxSize) {
            showError("Ukuran file terlalu besar! Maksimal 50MB.");
            return;
        }

        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String originalName = selectedFile.getName();
            String extension = "";
            int lastDot = originalName.lastIndexOf('.');
            if (lastDot > 0) {
                extension = originalName.substring(lastDot);
            }
            String uniqueFileName = currentUser.getSubject().replaceAll("\\s+", "_") + "_" +
                    targetClass + "_" + timestamp + extension;

            Path targetPath = Paths.get(UPLOAD_DIRECTORY + uniqueFileName);
            Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            Module module = new Module(
                    title,
                    description.isEmpty() ? null : description,
                    originalName,
                    targetPath.toString(),
                    getFileType(originalName),
                    selectedFile.length(),
                    currentUser.getSubject(),
                    currentUser.getName(),
                    targetClass
            );

            boolean success = DatabaseManager.insertModule(module);

            if (success) {
                String notificationMessage = "📚 Modul baru tersedia: " + title +
                        " untuk mata pelajaran " + currentUser.getSubject();
                DatabaseManager.sendNotificationToClass(targetClass, notificationMessage);

                clearUploadForm();

                loadModules();

                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Berhasil");
                info.setHeaderText("Modul Berhasil Diupload");
                info.setContentText("Modul '" + title + "' telah berhasil diupload!\n\n" +
                        "✓ File tersimpan dengan aman\n" +
                        "✓ Notifikasi telah dikirim ke siswa kelas " + targetClass + "\n" +
                        "✓ Siswa dapat mengakses modul melalui dashboard mereka");
                info.showAndWait();

                System.out.println(">>> Module uploaded successfully: " + title);
            } else {
                Files.deleteIfExists(targetPath);
                showError("Gagal menyimpan modul ke database!");
            }

        } catch (IOException e) {
            System.err.println("Error uploading file: " + e.getMessage());
            showError("Gagal mengupload file: " + e.getMessage());
        }
    }

    private void clearUploadForm() {
        textFieldModuleTitle.clear();
        comboBoxTargetClass.setValue(null);
        textAreaModuleDescription.clear();
        selectedFile = null;
        labelSelectedFile.setText("Belum ada file dipilih");
        buttonUploadModule.setDisable(true);
    }

    private void deleteModule(Module module) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Konfirmasi Hapus");
        confirmation.setHeaderText("Hapus Modul");
        confirmation.setContentText("Apakah Anda yakin ingin menghapus modul '" + module.getTitle() + "'?\n\n" +
                "File akan dihapus dari sistem dan siswa tidak dapat mengaksesnya lagi.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = DatabaseManager.deleteModule(module.getId());

            if (success) {
                // Delete physical file
                try {
                    Path filePath = Paths.get(module.getFilePath());
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    System.err.println("Error deleting physical file: " + e.getMessage());
                }

                // Send notification
                String notificationMessage = "📚 Modul '" + module.getTitle() +
                        "' telah dihapus dari mata pelajaran " + currentUser.getSubject();
                DatabaseManager.sendNotificationToClass(module.getClassName(), notificationMessage);

                loadModules();

                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Berhasil");
                info.setHeaderText("Modul Dihapus");
                info.setContentText("Modul '" + module.getTitle() + "' telah berhasil dihapus!");
                info.showAndWait();

                System.out.println(">>> Module deleted: " + module.getTitle());
            } else {
                showError("Gagal menghapus modul dari database!");
            }
        }
    }

    private String getFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "FILE";
        }
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "pdf": return "PDF";
            case "doc":
            case "docx": return "WORD";
            case "ppt":
            case "pptx": return "POWERPOINT";
            case "jpg":
            case "jpeg":
            case "png": return "IMAGE";
            case "txt": return "TEXT";
            default: return "FILE";
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    @FXML
    private void handleShowModules(ActionEvent event) {
        scheduleSection.setVisible(false);
        scheduleSection.setManaged(false);
        attendanceSection.setVisible(false);
        attendanceSection.setManaged(false);
        moduleSection.setVisible(true);
        moduleSection.setManaged(true);

        // Update button styles
        updateButtonStyles(buttonShowModules);

        // Load modules
        loadModules();
    }

    private void updateButtonStyles(Button activeButton) {
        // Reset all buttons
        buttonShowSchedule.getStyleClass().removeAll("sidebar-button-active");
        buttonShowAttendance.getStyleClass().removeAll("sidebar-button-active");
        buttonShowModules.getStyleClass().removeAll("sidebar-button-active");

        // Set active button
        activeButton.getStyleClass().add("sidebar-button-active");
    }

    //Memunculkan status class
    private void loadClassStates() {
        // Load status kelas dari database
        List<Schedule> allSchedules = DatabaseManager.getSchedulesByTeacher(currentUser.getName(), LocalDate.now());
        for (Schedule schedule : allSchedules) {
            if (schedule.isActive()) {
                // Cek apakah kelas masih berlangsung atau sudah selesai
                if (DatabaseManager.isClassFinished(schedule.getId())) {
                    finishedClasses.add(schedule.getId());
                } else if (DatabaseManager.isClassStarted(schedule.getId())) {
                    activeClasses.add(schedule.getId());
                }
            }
        }
    }

    //Ketika jadwal di tambah di admin akan muncul ini
    private void loadTodaySchedule(String teacherName) {
        List<Schedule> schedules = DatabaseManager.getSchedulesByTeacher(teacherName, LocalDate.now());
        scheduleList.setAll(schedules);
        columnSubjectSchedule.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getSubject()));
        columnClassName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getClassName()));
        columnDate.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        ));
        columnTime.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getStartTime() + " - " + data.getValue().getEndTime()
        ));
        columnRoom.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getRoom()));
        columnStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                getScheduleStatus(data.getValue())
        ));
        columnActions.setCellFactory(col -> new TableCell<>() {
            private final Button startButton = new Button("Mulai Kelas");
            private final Button endButton = new Button("Akhiri Kelas");
            private final Button viewButton = new Button("Lihat Absensi");
            {

                startButton.getStyleClass().addAll("start-button", "primary-button");
                endButton.getStyleClass().addAll("end-button", "secondary-button");
                viewButton.getStyleClass().addAll("primary-button");
                startButton.setOnAction(e -> {
                    Schedule schedule = getTableView().getItems().get(getIndex());
                    startClass(schedule);
                });
                endButton.setOnAction(e -> {
                    Schedule schedule = getTableView().getItems().get(getIndex());
                    endClass(schedule);
                });
                viewButton.setOnAction(e -> {
                    Schedule schedule = getTableView().getItems().get(getIndex());
                    viewClassAttendance(schedule);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Schedule schedule = getTableView().getItems().get(getIndex());
                    HBox buttonBox = new HBox(5);
                    if (finishedClasses.contains(schedule.getId())) {
                        // Kelas sudah selesai: hanya tampilkan tombol "Lihat Absensi"
                        buttonBox.getChildren().add(viewButton);
                    } else if (activeClasses.contains(schedule.getId())) {
                        // Kelas sedang berlangsung: tampilkan tombol "Akhiri Kelas" dan "Lihat Absensi"
                        buttonBox.getChildren().addAll(endButton, viewButton);
                    } else {
                        // Kelas belum dimulai: tampilkan tombol "Mulai Kelas"
                        buttonBox.getChildren().add(startButton);
                    }
                    setGraphic(buttonBox);
                }
            }
        });
        tableSchedule.setItems(scheduleList);
        System.out.println("Jadwal ditemukan: " + schedules.size());
    }

    private String getScheduleStatus(Schedule schedule) {
        if (finishedClasses.contains(schedule.getId())) {
            return "Selesai";
        } else if (activeClasses.contains(schedule.getId())) {
            return "Sedang Berlangsung";
        } else {
            return "Belum Dimulai";
        }
    }

    private void startClass(Schedule schedule) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Konfirmasi");
        confirmation.setHeaderText("Mulai Kelas");
        confirmation.setContentText("Apakah Anda yakin ingin memulai kelas " + schedule.getSubject() +
                "?\n\nSetelah kelas dimulai:\n- Siswa dapat melakukan absensi\n- Jadwal akan dikirim ke semua siswa di kelas " +
                schedule.getClassName());
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Update status di database
            boolean success = DatabaseManager.startClass(schedule.getId());
            if (success) {
                activeClasses.add(schedule.getId());
                // Kirim notifikasi ke semua student yang terdaftar di kelas ini
                notifyStudentsClassStarted(schedule);
                // Refresh tabel
                tableSchedule.refresh();
                // Tampilkan pesan sukses
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Berhasil");
                info.setHeaderText("Kelas Dimulai");
                info.setContentText("Kelas " + schedule.getSubject() + " telah dimulai!\n\n" +
                        "✓ Notifikasi telah dikirim ke semua siswa\n" +
                        "✓ Siswa sekarang dapat melakukan absensi\n" +
                        "✓ Jadwal telah tersedia di dashboard siswa");
                info.showAndWait();
                System.out.println(">>> Kelas dimulai: " + schedule.getSubject());
            } else {
                showError("Gagal memulai kelas. Silakan coba lagi.");
            }
        }
    }

    private void endClass(Schedule schedule) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Konfirmasi");
        confirmation.setHeaderText("Akhiri Kelas");
        confirmation.setContentText("Apakah Anda yakin ingin mengakhiri kelas " + schedule.getSubject() +
                "?\n\nSetelah kelas diakhiri:\n- Siswa tidak dapat lagi melakukan absensi\n" +
                "- Status kelas akan berubah menjadi 'Selesai'");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Update status di database
            boolean success = DatabaseManager.endClass(schedule.getId());
            if (success) {
                activeClasses.remove(Integer.valueOf(schedule.getId()));
                finishedClasses.add(schedule.getId());
                // Kirim notifikasi ke semua student bahwa kelas telah berakhir
                notifyStudentsClassEnded(schedule);
                // Refresh tabel
                tableSchedule.refresh();
                // Tampilkan pesan sukses dengan statistik
                List<Attendance> classAttendances = DatabaseManager.getAttendanceByScheduleId(schedule.getId());
                long presentCount = classAttendances.stream().filter(a -> "Hadir".equals(a.getStatus())).count();
                long totalStudents = DatabaseManager.getTotalStudentsByClass(schedule.getClassName());
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Berhasil");
                info.setHeaderText("Kelas Diakhiri");
                info.setContentText("Kelas " + schedule.getSubject() + " telah diakhiri!\n\n" +
                        "📊 Statistik Kehadiran:\n" +
                        "• Siswa hadir: " + presentCount + " dari " + totalStudents + " siswa\n" +
                        "• Persentase kehadiran: " + Math.round((double)presentCount/totalStudents*100) + "%\n\n" +
                        "✓ Siswa tidak dapat lagi melakukan absensi\n" +
                        "✓ Data absensi telah tersimpan");
                info.showAndWait();
                System.out.println(">>> Kelas diakhiri: " + schedule.getSubject());
            } else {
                showError("Gagal mengakhiri kelas. Silakan coba lagi.");
            }
        }
    }

    private void viewClassAttendance(Schedule schedule) {
        try {
            // Load attendance data untuk kelas tertentu
            List<Attendance> classAttendances = DatabaseManager.getAttendanceByScheduleId(schedule.getId());
            // Buat dialog untuk menampilkan absensi kelas
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Absensi Kelas - " + schedule.getSubject());
            dialog.setHeaderText("Kelas: " + schedule.getClassName() + " | Tanggal: " +
                    schedule.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    " | Jam: " + schedule.getStartTime() + "-" + schedule.getEndTime());
            // Buat tabel untuk dialog
            TableView<Attendance> dialogTable = new TableView<>();
            dialogTable.setPrefWidth(700);
            dialogTable.setPrefHeight(400);
            dialogTable.getStyleClass().add("table-view");
            TableColumn<Attendance, String> nameCol = new TableColumn<>("Nama Siswa");
            nameCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getStudentName()));
            nameCol.setPrefWidth(200);
            TableColumn<Attendance, String> nimCol = new TableColumn<>("NIM");
            nimCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getStudentNim()));
            nimCol.setPrefWidth(120);
            TableColumn<Attendance, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getStatus()));
            statusCol.setPrefWidth(100);
            statusCol.setCellFactory(column -> new TableCell<Attendance, String>() {
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
            TableColumn<Attendance, String> timeCol = new TableColumn<>("Waktu Absen");
            timeCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                    cellData.getValue().getTimeIn() != null ?
                            cellData.getValue().getTimeIn().toString() : "-"
            ));
            timeCol.setPrefWidth(130);
            dialogTable.getColumns().addAll(nameCol, nimCol, statusCol, timeCol);
            dialogTable.getItems().setAll(classAttendances);
            // Tambahkan statistik
            long presentCount = classAttendances.stream().filter(a -> "Hadir".equals(a.getStatus())).count();
            long absentCount = classAttendances.stream().filter(a -> "Tidak Hadir".equals(a.getStatus())).count();
            long lateCount = classAttendances.stream().filter(a -> "Terlambat".equals(a.getStatus())).count();
            long totalStudents = DatabaseManager.getTotalStudentsByClass(schedule.getClassName());
            long notYetAttend = totalStudents - classAttendances.size();
            VBox statsBox = new VBox(5);
            statsBox.getStyleClass().add("stats-box");
            Label statsTitle = new Label("📊 STATISTIK KEHADIRAN");
            statsTitle.getStyleClass().addAll("stats-title", "subtitle");
            Label presentLabel = new Label("✅ Hadir: " + presentCount + " siswa");
            presentLabel.getStyleClass().addAll("stats-present", "message", "success");
            Label lateLabel = new Label("⏰ Terlambat: " + lateCount + " siswa");
            lateLabel.getStyleClass().add("stats-late");
            Label absentLabel = new Label("❌ Tidak Hadir: " + absentCount + " siswa");
            absentLabel.getStyleClass().addAll("stats-absent", "message", "error");
            Label notAttendLabel = new Label("⏳ Belum Absen: " + notYetAttend + " siswa");
            notAttendLabel.getStyleClass().add("stats-not-attend");
            Label totalLabel = new Label("👥 Total Siswa: " + totalStudents + " siswa");
            totalLabel.getStyleClass().addAll("stats-total", "subtitle");
            double attendanceRate = totalStudents > 0 ? (double)(presentCount + lateCount) / totalStudents * 100 : 0;
            Label rateLabel = new Label("📈 Tingkat Kehadiran: " + Math.round(attendanceRate) + "%");
            rateLabel.getStyleClass().addAll("stats-rate", "headline");
            statsBox.getChildren().addAll(statsTitle, presentLabel, lateLabel, absentLabel, notAttendLabel, totalLabel, rateLabel);
            VBox dialogContent = new VBox(10);
            dialogContent.getStyleClass().add("dialog-content");
            dialogContent.getChildren().addAll(statsBox, dialogTable);
            dialogContent.setPadding(new Insets(10));
            dialog.getDialogPane().setContent(dialogContent);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Gagal memuat data absensi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void notifyStudentsClassStarted(Schedule schedule) {
        System.out.println(">>> Mengirim notifikasi ke siswa: Kelas " + schedule.getSubject() + " dimulai");
        // Update status di database agar student bisa melihat kelas yang aktif
        DatabaseManager.setClassActiveForStudents(schedule.getId(), true);
        // Kirim notifikasi ke semua siswa di kelas tersebut
        DatabaseManager.sendNotificationToClass(schedule.getClassName(),
                "Kelas " + schedule.getSubject() + " telah dimulai! Silakan lakukan absensi sekarang.");
    }

    private void notifyStudentsClassEnded(Schedule schedule) {
        System.out.println(">>> Mengirim notifikasi ke siswa: Kelas " + schedule.getSubject() + " berakhir");
        // Update status di database
        DatabaseManager.setClassActiveForStudents(schedule.getId(), false);
        // Kirim notifikasi ke semua siswa di kelas tersebut
        DatabaseManager.sendNotificationToClass(schedule.getClassName(),
                "Kelas " + schedule.getSubject() + " telah berakhir. Absensi ditutup.");
    }

    private void loadAttendanceData(String subject) {
        List<Attendance> attendances = DatabaseManager.getAttendanceBySubject(subject);
        columnStudentName.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getStudentName()));
        columnStudentNim.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getStudentNim()));
        columnSubjectAttendance.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getSubject()));
        columnStatusAttendance.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getStatus()));
        columnDateAttendance.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue().getDate()));
        // Style status column with CSS classes
        columnStatusAttendance.setCellFactory(column -> new TableCell<Attendance, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                // Clear previous styles
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
        tableAttendance.getItems().setAll(attendances);
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        if (currentUser != null) {
            loadTodaySchedule(currentUser.getName());
            loadAttendanceData(currentUser.getSubject());
            loadModules();
            loadClassStates(); // Refresh status kelas
            // Clear filter jika ada
            if (attendanceSection.getChildren().size() > 3) {
                attendanceSection.getChildren().removeIf(node ->
                        node instanceof Label && ((Label) node).getText().startsWith("📋"));
            }
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Refresh Berhasil");
            info.setHeaderText(null);
            info.setContentText("Data telah diperbarui!");
            info.showAndWait();
            System.out.println(">>> Data berhasil di-refresh oleh tentor");
        } else {
            System.out.println(">>> Tidak bisa refresh: currentUser null");
        }
    }

    @FXML
    private void handleShowSchedule(ActionEvent event) {
        scheduleSection.setVisible(true);
        scheduleSection.setManaged(true);
        attendanceSection.setVisible(false);
        attendanceSection.setManaged(false);
        moduleSection.setVisible(false);
        moduleSection.setManaged(false);

        // Update button styles
        updateButtonStyles(buttonShowSchedule);
    }

    @FXML
    private void handleShowAttendance(ActionEvent event) {
        scheduleSection.setVisible(false);
        scheduleSection.setManaged(false);
        attendanceSection.setVisible(true);
        attendanceSection.setManaged(true);
        moduleSection.setVisible(false);
        moduleSection.setManaged(false);

        // Update button styles
        updateButtonStyles(buttonShowAttendance);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // MENGATASI LOG OUT
    @FXML
    public void handleLogout(ActionEvent event) {
        try {

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            SceneManager.handleLogout(stage);

        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
