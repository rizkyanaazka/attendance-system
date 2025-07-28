module com.demo5 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires jbcrypt;
    requires java.sql;
    requires java.desktop;

    opens com.attendance.controller to javafx.fxml;

    opens com.attendance to javafx.fxml;
    exports com.attendance;
}