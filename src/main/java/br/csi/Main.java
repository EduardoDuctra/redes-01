package br.csi;

import br.csi.Service.ChatService;
import br.csi.Swing.ChatP2PUI;

import javax.swing.*;
import java.net.SocketException;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String nomeUsuario = JOptionPane.showInputDialog("Digite seu nome de usuário:");

            if(!nomeUsuario.isEmpty()){
                try {
                    ChatService chatService = new ChatService(nomeUsuario);
                    ChatP2PUI ui = new ChatP2PUI(chatService);
                    ui.setVisible(true);
                    chatService.iniciarRede();
                } catch (SocketException e) {
                    System.out.println("Erro ao inicializar");
                }
            } else {
                JOptionPane.showMessageDialog(null, "Nome inválido!", "Aviso", JOptionPane.WARNING_MESSAGE);
                System.out.println("Nome inválido. Finalizando chat");
                return;
            }

        });
    }
}
