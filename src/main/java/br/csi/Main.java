package br.csi;

import javax.swing.*;
import java.net.SocketException;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String nomeUsuario = JOptionPane.showInputDialog("Digite seu nome de usuário:");
            if(nomeUsuario == null || nomeUsuario.isEmpty()) return;

            try {
                // Cria o serviço de chat
                ChatService chatService = new ChatService(nomeUsuario);

                // Cria a interface gráfica
                ChatP2PUI ui = new ChatP2PUI(chatService);

                // Adiciona a interface como listener
                chatService.addListener(ui);

                // Mostra a janela
                ui.setVisible(true);

            } catch (SocketException e) {
                JOptionPane.showMessageDialog(null,
                        "Erro ao iniciar o chat: " + e.getMessage(),
                        "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
