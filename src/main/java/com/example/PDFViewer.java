package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PDFViewer extends JFrame {
    private JLabel labelBookTitle;
    private JLabel label;
    private JButton buttonOpen;
    private JButton buttonPrevious;
    private JButton buttonNext;
    private JLabel labelInfo;
    private JPanel bookmarksPanel;
    private PDDocument pdfDocument;
    private PDFRenderer pdfRenderer;
    private int currentPage = 0;
    private String bookmarkFile = "bookmark.txt";
    private File selectedFile;

    public PDFViewer() {
        setTitle("PDF Viewer");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initializeComponents();
        loadBookmarks();
        setDarkTheme();
    }

    private void initializeComponents() {
        labelBookTitle = new JLabel("", SwingConstants.CENTER);
        label = new JLabel("", SwingConstants.CENTER);
        labelInfo = new JLabel("", SwingConstants.CENTER);
        labelInfo.setPreferredSize(new Dimension(200, 30));

        buttonOpen = new JButton("Abrir PDF");
        buttonOpen.addActionListener(e -> openPDF());

        buttonPrevious = new JButton("Anterior");
        buttonPrevious.addActionListener(e -> showPreviousPage());

        buttonNext = new JButton("Próxima");
        buttonNext.addActionListener(e -> showNextPage());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.BLACK); // Fundo preto
        panel.add(labelBookTitle, BorderLayout.NORTH);
        panel.add(new JScrollPane(label), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.BLACK); // Fundo preto
        buttonPanel.add(buttonOpen);
        buttonPanel.add(buttonPrevious);
        buttonPanel.add(buttonNext);

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(Color.BLACK); // Fundo preto
        infoPanel.add(buttonPanel, BorderLayout.CENTER);
        infoPanel.add(labelInfo, BorderLayout.SOUTH);

        panel.add(infoPanel, BorderLayout.SOUTH);

        bookmarksPanel = new JPanel();
        bookmarksPanel.setBackground(Color.DARK_GRAY); // Fundo cinza escuro
        panel.add(new JScrollPane(bookmarksPanel), BorderLayout.WEST);
        panel.add(bookmarksPanel, BorderLayout.WEST);

        getContentPane().setBackground(Color.BLACK); // Fundo preto
        add(panel);
    }

    private void setDarkTheme() {
        UIManager.put("Label.foreground", Color.WHITE); // Texto branco para rótulos
        UIManager.put("Button.background", Color.DARK_GRAY); // Fundo cinza escuro para botões
        UIManager.put("Button.foreground", Color.WHITE); // Texto branco para botões
        UIManager.put("Panel.background", Color.BLACK); // Fundo preto para painéis
        UIManager.put("ScrollPane.background", Color.BLACK); // Fundo preto para rolagem
        UIManager.put("ScrollBar.background", Color.BLACK); // Fundo preto para barras de rolagem
        UIManager.put("SplitPane.background", Color.BLACK); // Fundo preto para divisórias
        UIManager.put("Viewport.background", Color.BLACK); // Fundo preto para viewport
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void openPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Abrir PDF");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File file) {
                return file.getName().toLowerCase().endsWith(".pdf") || file.isDirectory();
            }

            public String getDescription() {
                return "Arquivos PDF (*.pdf)";
            }
        });

        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            try {
                loadPDF(selectedFile);
            } catch (IOException e) {
                showErrorDialog("Erro ao abrir PDF");
            }
        }
    }

    private void loadPDF(File file) throws IOException {
        pdfDocument = PDDocument.load(file);
        pdfRenderer = new PDFRenderer(pdfDocument);
        currentPage = loadBookmark();
        displayPage(currentPage);
        updateBookInfo();
        updateNavigationButtons();
        copyFileToPDFDirectory(file);
        labelBookTitle.setText(file.getName());
    }

    private void displayPage(int pageNumber) {
        try {
            if (pdfDocument != null && pageNumber >= 0 && pageNumber < pdfDocument.getNumberOfPages()) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageNumber, 300);
                label.setIcon(new ImageIcon(scaleImage(image)));
            }
        } catch (IOException e) {
            showErrorDialog("Erro ao exibir página");
        }
    }

    private Image scaleImage(BufferedImage image) {
        float scale = Math.min(1f, Math.min((float) label.getWidth() / image.getWidth(), (float) label.getHeight() / image.getHeight()));
        return image.getScaledInstance((int) (image.getWidth() * scale), (int) (image.getHeight() * scale), Image.SCALE_SMOOTH);
    }

    private void showPreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePage();
        }
    }

    private void showNextPage() {
        if (pdfDocument != null && currentPage < pdfDocument.getNumberOfPages() - 1) {
            currentPage++;
            updatePage();
        }
    }

    private void updatePage() {
        displayPage(currentPage);
        updateNavigationButtons();
        updateBookInfo();
        saveBookmark(currentPage);
    }

    private void updateNavigationButtons() {
        buttonPrevious.setEnabled(currentPage > 0);
        buttonNext.setEnabled(pdfDocument != null && currentPage < pdfDocument.getNumberOfPages() - 1);
    }

    private void updateBookInfo() {
        if (pdfDocument != null) {
            labelInfo.setText("Página " + (currentPage + 1) + " de " + pdfDocument.getNumberOfPages());
        }
    }

    private int loadBookmark() {
        try (BufferedReader reader = new BufferedReader(new FileReader(bookmarkFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(selectedFile.getAbsolutePath())) {
                    return Integer.parseInt(reader.readLine());
                }
            }
        } catch (IOException | NumberFormatException e) {
        }
        return 0;
    }

    private void saveBookmark(int pageNumber) {
        List<String> lines = new ArrayList<>();
        boolean found = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(bookmarkFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(selectedFile.getAbsolutePath())) {
                    lines.add(line);
                    lines.add(String.valueOf(pageNumber));
                    reader.readLine();
                    found = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            showErrorDialog("Erro ao salvar marcador");
        }

        if (!found) {
            lines.add(selectedFile.getAbsolutePath());
            lines.add(String.valueOf(pageNumber));
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(bookmarkFile))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            showErrorDialog("Erro ao salvar marcador");
        }
    }

    private void loadBookmarks() {
        bookmarksPanel.setPreferredSize(new Dimension(200, 0));
        try (BufferedReader reader = new BufferedReader(new FileReader(bookmarkFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String filePath = line;
                JButton button = createBookmarkButton(filePath);
                bookmarksPanel.add(button);
                reader.readLine();
            }
        } catch (IOException e) {
            showErrorDialog("Erro ao carregar marcadores");
        }
    }

    private JButton createBookmarkButton(String filePath) {
        JButton button = new JButton(new File(filePath).getName());
        button.setPreferredSize(new Dimension(200, 30));
        button.addActionListener(e -> {
            openFilePDF(new File(filePath));
            bookmarksPanel.setVisible(false);
        });
        return button;
    }

    private void openFilePDF(File file) {
        selectedFile = file;
        try {
            loadPDF(file);
        } catch (IOException e) {
            showErrorDialog("Erro ao abrir PDF");
        }
    }

    private void copyFileToPDFDirectory(File file) {
        try {
            String directory = Paths.get(System.getProperty("user.dir")).getParent().resolve("PDFs").toString();
            copyFileOrDirectory(file.getAbsolutePath(), directory + File.separator + file.getName());
        } catch (IOException e) {
            showErrorDialog("Erro ao copiar arquivo");
        }
    }

    private void copyFileOrDirectory(String sourcePathStr, String destinationPathStr) throws IOException {
        Path sourcePath = Paths.get(sourcePathStr);
        Path destinationPath = Paths.get(destinationPathStr);

        if (Files.isDirectory(sourcePath)) {
            copyDirectory(sourcePath, destinationPath);
        } else {
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyDirectory(Path source, Path destination) throws IOException {
        if (!Files.exists(destination)) {
            Files.createDirectory(destination);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
            for (Path file : stream) {
                Path target = destination.resolve(file.getFileName());
                if (Files.isDirectory(file)) {
                    copyDirectory(file, target);
                } else {
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PDFViewer viewer = new PDFViewer();
            viewer.setVisible(true);
        });
    }
}
