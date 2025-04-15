module com.udacity.catpoint.securityServices {
    requires com.miglayout.swing;
    requires java.desktop;
    requires java.prefs;
    requires com.google.gson;
    requires com.google.common;
    requires java.sql;
    requires com.udacity.catpoint.images;
    opens com.udacity.catpoint.security.data to com.google.gson;

}