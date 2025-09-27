package br.csi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class UserSession extends JFrame {
    private String user;
    private ChatP2P parent;
    private JTextArea chatArea;
    private JTextField messageField;

    public UserSession(String user, ChatP2P parent) {
        this.user = user;
        this.parent = parent;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Chat com " + user);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();

        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        JButton endChatButton = new JButton("Encerrar Chat");
        endChatButton.addActionListener(e -> {
            endChatButton.setEnabled(false);
            encerrarChat();
        });

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        // Painel inferior
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);

        // Painel para os botões, abaixo do campo de texto
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(endChatButton);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        // Fechar janela
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                encerrarChat();
            }
        });
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            parent.sendIndividualMessage(user, message);
            addMessage("Você: " + message);
            messageField.setText("");
        }
    }

    private void encerrarChat() {
        parent.sendEndChat(user);
        parent.getUsuariosConectados().remove(user);
        parent.getSessoesUsuarios().remove(user);
        parent.getModeloUsuariosOnline().removeElement(user);
        dispose();
        System.out.println("🚪 Chat encerrado com " + user);
    }

    public void addMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
}
