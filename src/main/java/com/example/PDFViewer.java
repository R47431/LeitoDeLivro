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
    private Modelos modelos;

    public PDFViewer() {
        modelos = new Modelos();
        setTitle("PDF Viewer");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initializeComponents();
        loadBookmarks();
        setDarkTheme();
    }

    private void initializeComponents() {
        modelos.setLabelBookTitle(new JLabel("", SwingConstants.CENTER));
        modelos.setLabel(new JLabel("", SwingConstants.CENTER));

        modelos.setLabelInfo(new JLabel("", SwingConstants.CENTER));
        modelos.getLabelInfo().setPreferredSize(new Dimension(200, 30));

        modelos.setButtonOpen(new JButton("Abrir PDF"));
        modelos.getButtonOpen().addActionListener(e -> openPDF());

        modelos.setButtonPrevious(new JButton("Anterior"));
        modelos.getButtonPrevious().addActionListener(e -> showPreviousPage());

        modelos.setButtonNext(new JButton("Próxima"));
        modelos.getButtonNext().addActionListener(e -> showNextPage());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.BLACK);
        panel.add(modelos.getLabelBookTitle(), BorderLayout.NORTH);
        panel.add(new JScrollPane(modelos.getLabel()), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.BLACK);
        buttonPanel.add(modelos.getButtonOpen());
        buttonPanel.add(modelos.getButtonPrevious());
        buttonPanel.add(modelos.getButtonNext());

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(Color.BLACK); // Fundo preto
        infoPanel.add(buttonPanel, BorderLayout.CENTER);
        infoPanel.add(modelos.getLabelInfo(), BorderLayout.SOUTH);

        panel.add(infoPanel, BorderLayout.SOUTH);

        modelos.setBookmarksPanel(new JPanel());
        modelos.getBookmarksPanel().setBackground(Color.DARK_GRAY);
        panel.add(new JScrollPane(modelos.getBookmarksPanel()), BorderLayout.WEST);
        panel.add(modelos.getBookmarksPanel(), BorderLayout.WEST);

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
            modelos.setSelectedFile(fileChooser.getSelectedFile());

            try {
                loadPDF(modelos.getSelectedFile());
            } catch (IOException e) {
                showErrorDialog("Erro ao abrir PDF");
            }
        }
    }

    private void loadPDF(File file) throws IOException {
        modelos.setPdfDocument(PDDocument.load(file));
        modelos.setPdfRenderer(new PDFRenderer(modelos.getPdfDocument()));
        modelos.setCurrentPage(loadBookmark());
        displayPage(modelos.getCurrentPage());
        updateBookInfo();
        updateNavigationButtons();
        copyFileToPDFDirectory(file);
        modelos.getLabelBookTitle().setText(file.getName());

    }

    private void displayPage(int pageNumber) {
        try {
            if (modelos.getPdfDocument() != null && pageNumber >= 0
                    && pageNumber < modelos.getPdfDocument().getNumberOfPages()) {
                BufferedImage image = modelos.getPdfRenderer().renderImageWithDPI(pageNumber, 300);
                modelos.getLabel().setIcon(new ImageIcon(scaleImage(image)));
            }
        } catch (IOException e) {
            showErrorDialog("Erro ao exibir página");
        }
    }

    private Image scaleImage(BufferedImage image) {
        float scale = Math.min(1f,
                Math.min((float) modelos.getLabel().getWidth() / image.getWidth(),
                        (float) modelos.getLabel().getHeight() / image.getHeight()));
        return image.getScaledInstance((int) (image.getWidth() * scale), (int) (image.getHeight() * scale),
                Image.SCALE_SMOOTH);
    }

    private void showPreviousPage() {
        if (modelos.getCurrentPage() > 0) {
            modelos.setCurrentPage(modelos.getCurrentPage() - 1);
            updatePage();
        }
    }

    private void showNextPage() {
        if (modelos.getPdfDocument() != null
                && modelos.getCurrentPage() < modelos.getPdfDocument().getNumberOfPages() - 1) {
            modelos.setCurrentPage(modelos.getCurrentPage() + 1);
            updatePage();
        }
    }

    private void updatePage() {
        displayPage(modelos.getCurrentPage());
        updateNavigationButtons();
        updateBookInfo();
        saveBookmark(modelos.getCurrentPage());
    }

    private void updateNavigationButtons() {
        modelos.getButtonPrevious().setEnabled(modelos.getCurrentPage() > 0);
        modelos.getButtonNext()
                .setEnabled(modelos.getPdfDocument() != null
                        && modelos.getCurrentPage() < modelos.getPdfDocument().getNumberOfPages() - 1);
    }

    private void updateBookInfo() {
        if (modelos.getPdfDocument() != null) {
            modelos.getLabelInfo().setText(
                    "Página " + (modelos.getCurrentPage() + 1) + " de " + modelos.getPdfDocument().getNumberOfPages());
        }
    }

    private int loadBookmark() {
        try (BufferedReader reader = new BufferedReader(new FileReader(modelos.getBookmarkFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(modelos.getSelectedFile().getAbsolutePath())) {
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
        try (BufferedReader reader = new BufferedReader(new FileReader(modelos.getBookmarkFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(modelos.getSelectedFile().getAbsolutePath())) {
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
            lines.add(modelos.getSelectedFile().getAbsolutePath());
            lines.add(String.valueOf(pageNumber));
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(modelos.getBookmarkFile()))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            showErrorDialog("Erro ao salvar marcador");
        }
    }

    private void loadBookmarks() {
        modelos.getBookmarksPanel().setPreferredSize(new Dimension(200, 0));
        try (BufferedReader reader = new BufferedReader(new FileReader(modelos.getBookmarkFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String filePath = line;
                JButton button = createBookmarkButton(filePath);
                modelos.getBookmarksPanel().add(button);
                reader.readLine();
            }
        } catch (IOException e) {
            showErrorDialog("Erro ao carregar marcadores");
        }
    }

    private boolean bookmarksPanelExpanded = false;

    private JButton createBookmarkButton(String filePath) {
        JButton button = new JButton(new File(filePath).getName());
        button.setPreferredSize(new Dimension(200, 30));
        button.addActionListener(e -> {
            if (bookmarksPanelExpanded) {
                modelos.getBookmarksPanel().setPreferredSize(new Dimension(200, 0));
                bookmarksPanelExpanded = false;

            } else {
                openFilePDF(new File(filePath));
                modelos.getBookmarksPanel().setPreferredSize(new Dimension(20, 0));
                bookmarksPanelExpanded = true;
            }
            modelos.getBookmarksPanel().revalidate();
        });

        return button;
    }

    private void openFilePDF(File file) {
        modelos.setSelectedFile(file);
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

    public void apagarLivrosDaLista() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(modelos.getBookmarkFile()));
            FileWriter writer = new FileWriter("temporario.txt");
            String linhaAtual;
            while ((linhaAtual = reader.readLine()) != null) {
                if (linhaAtual.equals(modelos.getSelectedFile().toString())) {
                    reader.readLine();
                    continue;
                }
                writer.write(linhaAtual + "\n");
            }

            reader.close();
            writer.close();

            File arquivoOriginal = new File(modelos.getBookmarkFile());
            File temporario = new File("temporario.txt");
            if (arquivoOriginal.delete()) {
                temporario.renameTo(arquivoOriginal);
            } else {
                throw new IOException("Falha ao renomear arquivo temporário para o original.");
            }

            System.out.println("Linha removida com sucesso.");

        } catch (FileNotFoundException e) {
            System.err.println("Arquivo não encontrado: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro de I/O: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PDFViewer viewer = new PDFViewer();
            viewer.setVisible(true);
        });
    }
}
