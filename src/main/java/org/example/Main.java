package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class Main extends JFrame {

    private JTextField projectNameField;
    private JButton downloadButton;

    public Main() {
        setTitle("File Downloader");
        setSize(400, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Główna panela
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Etykieta Nazwa Projektu
        JLabel projectNameLabel = new JLabel("Nazwa projektu:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(projectNameLabel, gbc);

        // Pole tekstowe Nazwa Projektu
        projectNameField = new JTextField();
        projectNameField.setPreferredSize(new Dimension(200, 25));
        gbc.gridx = 1;
        gbc.gridy = 0;
        mainPanel.add(projectNameField, gbc);

        // Przycisk Pobierz Pliki
        downloadButton = new JButton("Pobierz pliki");
        downloadButton.setPreferredSize(new Dimension(150, 25));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(downloadButton, gbc);
        downloadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String projectName = projectNameField.getText().trim();
                if (!projectName.isEmpty()) {
                    downloadFiles(projectName);
                } else {
                    JOptionPane.showMessageDialog(Main.this,
                            "Wprowadź nazwę projektu!",
                            "Błąd",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        getContentPane().add(mainPanel);
        setVisible(true);
    }

    private static final String JDBC_URL = "jdbc:sap://srv-sap10.tarkon.local:30015";
    private static final String USERNAME = "SAPADMIN";
    private static final String PASSWORD = "uep0Edo84s6-d7iE";

    private void downloadFiles(String projectName) {
        try {
            Class.forName("com.sap.db.jdbc.Driver"); // Ładowanie sterownika JDBC

            try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
                String sqlQuery = "SELECT DISTINCT T0.\"BatchNum\" " +
                        "FROM \"TARKON_PROD\".\"IBT1\" T0 " +
                        "INNER JOIN \"TARKON_PROD\".\"IGE1\" T1 ON T0.\"BaseType\" = T1.\"ObjType\" AND T0.\"BaseEntry\" = T1.\"DocEntry\" AND T0.\"BaseLinNum\" = T1.\"LineNum\" " +
                        "INNER JOIN \"TARKON_PROD\".\"OIGE\" T2 ON T1.\"DocEntry\" = T2.\"DocEntry\" " +
                        "INNER JOIN \"TARKON_PROD\".\"OBTN\" T3 ON T3.\"DistNumber\" = T0.\"BatchNum\" AND T3.\"ItemCode\" = T0.\"ItemCode\" " +
                        "INNER JOIN \"TARKON_PROD\".\"OITM\" T4 ON T0.\"ItemCode\" = T4.\"ItemCode\" " +
                        "INNER JOIN \"TARKON_PROD\".\"OPRJ\" T5 ON T1.\"Project\" = T5.\"PrjCode\" " +
                        "WHERE T1.\"Project\" = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sqlQuery)) {
                    stmt.setString(1, projectName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<String> batchNumbers = new ArrayList<>();
                        while (rs.next()) {
                            batchNumbers.add(rs.getString("BatchNum"));
                        }

                        if (!batchNumbers.isEmpty()) {
                            searchAndDownloadFiles(batchNumbers);
                            JOptionPane.showMessageDialog(null, "Pobieranie zakończone!", "Sukces", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(null, "Nie znaleziono plików dla podanego projektu.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Błąd: " + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchAndDownloadFiles(List<String> batchNumbers) {
        String rootFolderPath = "\\\\10.100.100.36\\Projekty\\MATERIAL_CERTICATES"; // Ścieżka systemowa dla sieciowego folderu Windows
        File rootFolder = new File(rootFolderPath);

        if (!rootFolder.exists()) {
            JOptionPane.showMessageDialog(this, "Ścieżka do katalogu nie istnieje.", "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String projectName = projectNameField.getText().trim();
        String downloadFolder = projectName.replaceAll("[^a-zA-Z0-9_-]", "_");
        File projectFolder = new File(downloadFolder);

        if (!projectFolder.exists()) {
            projectFolder.mkdirs();
        }


        for (String batchNumber : batchNumbers) {
            List<File> filesToDownload = findFilesByBatchNumber(rootFolder, batchNumber);

            for (File file : filesToDownload) {
                try (InputStream in = new FileInputStream(file);
                     OutputStream out = new FileOutputStream(new File(projectFolder, file.getName()))) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    private List<File> findFilesByBatchNumber(File rootFolder, String batchNumber) {
        List<File> result = new ArrayList<>();
        try {
            Files.walk(rootFolder.toPath())
                    .filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        String fileName = filePath.getFileName().toString();
                        if (fileName.contains(batchNumber)) {
                            result.add(filePath.toFile());
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Main();
            }
        });
    }
}
