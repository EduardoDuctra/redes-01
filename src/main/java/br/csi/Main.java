package br.csi;

import javax.swing.*;
import java.net.*;
import java.io.*;

import br.csi.ChatP2P;
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) {
        String username = null;

        // Se houver argumento, usa ele
        if (args.length >= 1) {
            username = args[0];
        } else {
            // Senão, pede via diálogo
            username = JOptionPane.showInputDialog(null, "Digite seu nome de usuário:", "Login", JOptionPane.QUESTION_MESSAGE);
        }

        // Verifica se o nome é válido
        if (username == null || username.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nome de usuário inválido. Encerrando o chat.");
            System.exit(0);
        }

        // Inicializa o chat na Event Dispatch Thread
        String finalUsername = username.trim();
        SwingUtilities.invokeLater(() -> {
            ChatP2P chat = new ChatP2P(finalUsername);
            chat.setVisible(true);
        });
    }
}