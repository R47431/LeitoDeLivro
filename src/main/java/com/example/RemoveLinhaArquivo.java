package com.example;

import java.io.*;

public class RemoveLinhaArquivo {

    public static void main(String[] args) {
        // Caminho do arquivo
        String caminhoArquivo = "caminho/do/seu/arquivo.txt";
        // Linha que queremos remover
        String linhaParaRemover = "/home/rafael/Downloads/Código limpo - Habilidades práticas do Agile Software - Autor (Robert C. Martin).pdf";

        try {
            // Cria um BufferedReader para ler o arquivo
            BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo));
            
            // Cria um FileWriter para escrever no arquivo temporário
            FileWriter writer = new FileWriter("temporario.txt");
            
            String linhaAtual;

            // Lê cada linha do arquivo original
            while ((linhaAtual = reader.readLine()) != null) {
                // Verifica se é a linha que queremos remover
                if (linhaAtual.equals(linhaParaRemover)) {
                    // Se for, não escreve essa linha no arquivo temporário
                    continue;
                }
                // Escreve a linha no arquivo temporário
                writer.write(linhaAtual + "\n");
            }

            // Fecha os recursos
            reader.close();
            writer.close();

            // Renomeia o arquivo temporário para o nome original, se preferir
            File arquivoOriginal = new File(caminhoArquivo);
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
}
