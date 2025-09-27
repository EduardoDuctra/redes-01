package br.csi;

import br.csi.ChatP2P;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GroupChatWindow extends JFrame {
    private ChatP2P parent;
    private JTextArea chatArea;
    private JTextField messageField;

    public GroupChatWindow(ChatP2P parent) {
        this.parent = parent;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Chat em Grupo");
        setSize(500, 400);
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
            parent.sendEndChat("grupo"); // ou alguma lógica específica para grupo
            dispose();
        });

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        // Painel inferior com campo de mensagem
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);

        // Painel para botões abaixo do campo de mensagem
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(endChatButton);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            parent.sendGroupMessage(message);
            addMessage("Você: " + message);
            messageField.setText("");
        }
    }

    public void addMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
}