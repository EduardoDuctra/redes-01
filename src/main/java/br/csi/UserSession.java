package br.csi;

import br.csi.ChatP2P;

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

        // Área de chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // Campo de mensagem e botão enviar
        messageField = new JTextField();
        JButton sendButton = new JButton("Enviar");

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        // Layout
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // Fechar janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                parent.sendEndChat(user);
                dispose();
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

    public void addMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
}