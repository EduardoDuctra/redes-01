package br.csi;

import br.csi.Service.ChatService;
import br.csi.Swing.ChatP2PUI;

import javax.swing.*;
import java.net.SocketException;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String nomeUsuario = JOptionPane.showInputDialog("Digite seu nome de usuário:");
            if(nomeUsuario == null || nomeUsuario.isEmpty()) return;

            try {
                ChatService chatService = new ChatService(nomeUsuario);
                ChatP2PUI ui = new ChatP2PUI(chatService);
                ui.setVisible(true);
                chatService.iniciarRede();
            } catch (SocketException e) {
                JOptionPane.showMessageDialog(null,
                        "Erro ao iniciar o chat: " + e.getMessage(),
                        "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
