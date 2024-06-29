package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
//import org.apache.pdfbox.pdmodel.PDPage;
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

        labelBookTitle = new JLabel();
        labelBookTitle.setHorizontalAlignment(SwingConstants.CENTER);

        label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);

        buttonOpen = new JButton("Abrir PDF");
        buttonOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openPDF();
            }
        });

        buttonPrevious = new JButton("Anterior");
        buttonPrevious.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showPreviousPage();
            }
        });

        buttonNext = new JButton("Próxima");
        buttonNext.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showNextPage();
            }
        });

        labelInfo = new JLabel();
        labelInfo.setHorizontalAlignment(SwingConstants.CENTER);
        labelInfo.setPreferredSize(new Dimension(200, 30));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(labelBookTitle, BorderLayout.NORTH);
        panel.add(new JScrollPane(label), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(buttonOpen);
        buttonPanel.add(buttonPrevious);
        buttonPanel.add(buttonNext);

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(buttonPanel, BorderLayout.CENTER);
        infoPanel.add(labelInfo, BorderLayout.SOUTH);

        panel.add(infoPanel, BorderLayout.SOUTH);


        bookmarksPanel = new JPanel();
        panel.add(new JScrollPane(bookmarksPanel), BorderLayout.WEST);
        panel.add(bookmarksPanel, BorderLayout.WEST);
        add(panel);
       
        loadBookmarks();

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
                pdfDocument = PDDocument.load(selectedFile);
                pdfRenderer = new PDFRenderer(pdfDocument);
                currentPage = loadBookmark();


                displayPage(currentPage);
                updateBookInfo();
                updateNavigationButtons();
                String diretorio = Paths.get(System.getProperty("user.dir")).getParent().resolve("PDFs").toString();
                copyFileOrDirectory(selectedFile.getAbsolutePath(), diretorio + File.separator + selectedFile.getName());
                labelBookTitle.setText(selectedFile.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao abrir PDF", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void displayPage(int pageNumber) {
        try {
            if (pdfDocument != null && pageNumber >= 0 && pageNumber < pdfDocument.getNumberOfPages()) {
               // PDPage page = pdfDocument.getPage(pageNumber);
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageNumber, 300);
                float scale = 1f;

                if (image.getWidth() > getWidth() - 50) {
                    scale = (float) (getWidth() - 50) / image.getWidth();
                }

                Image scaledImage = image.getScaledInstance((int) (image.getWidth() * scale), -1, Image.SCALE_SMOOTH);

                label.setIcon(new ImageIcon(scaledImage));
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao exibir página", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            displayPage(currentPage);
            updateNavigationButtons();
            updateBookInfo();
            saveBookmark(currentPage);
        }
    }

    private void showNextPage() {
        if (pdfDocument != null && currentPage < pdfDocument.getNumberOfPages() - 1) {
            currentPage++;
            displayPage(currentPage);
            updateNavigationButtons();
            updateBookInfo();
            saveBookmark(currentPage);
        }
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
                    String pageNumberStr = reader.readLine();
                    return Integer.parseInt(pageNumberStr);
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
                    reader.readLine();
                    lines.add(String.valueOf(pageNumber)); 
                    found = true;
                } else {
                    lines.add(line); 
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Erro ao salvar marcador", "Erro", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(null, "Erro ao salvar marcador", "Erro", JOptionPane.ERROR_MESSAGE);
        }

    }

    private void openFilePDF(File file) {
        selectedFile = file;

        try {
            pdfDocument = PDDocument.load(selectedFile);
            pdfRenderer = new PDFRenderer(pdfDocument);
            currentPage = loadBookmark();

            displayPage(currentPage);
            updateBookInfo();
            updateNavigationButtons();
            labelBookTitle.setText(selectedFile.getName());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao abrir PDF", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBookmarks() {
        bookmarksPanel.setPreferredSize(new Dimension(200,0));
        try (BufferedReader reader = new BufferedReader(new FileReader(bookmarkFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String filePath = line;
                JButton button = new JButton(new File(filePath).getName());
                button.setPreferredSize(new Dimension(200,30));
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        openFilePDF(new File(filePath));
                        bookmarksPanel.setVisible(false); // Redesenha o painel de marcadores

                    }
                });
                bookmarksPanel.add(button);
                reader.readLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Erro ao carregar marcadores", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

     private void copyFileOrDirectory(String sourcePathStr, String destinationPathStr) throws IOException {
        Path sourcePath = Paths.get(sourcePathStr);
        Path destinationPath = Paths.get(destinationPathStr);

        if (Files.isDirectory(sourcePath)) {
            // Se for um diretório, copia o diretório recursivamente
            copyDirectory(sourcePath, destinationPath);
        } else {
            // Se for um arquivo, copia o arquivo
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyDirectory(Path source, Path destination) throws IOException {
        // Cria o diretório de destino se ele não existir
        if (!Files.exists(destination)) {
            Files.createDirectory(destination);
        }

        // Itera sobre todos os arquivos/diretórios no diretório de origem
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
            for (Path file : stream) {
                Path target = destination.resolve(file.getFileName());
                if (Files.isDirectory(file)) {
                    // Se for um diretório, chama recursivamente copyDirectory
                    copyDirectory(file, target);
                } else {
                    // Se for um arquivo, copia o arquivo
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                PDFViewer viewer = new PDFViewer();
                viewer.setVisible(true);
            }
        });
    }   
}
