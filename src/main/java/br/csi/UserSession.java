package br.csi;

import javax.swing.*;
import java.awt.*;

public class UserSession extends JFrame {

    private String usuario;
    private ChatService chatService;
    private JTextArea chatArea;
    private JTextField messageField;

    public UserSession(String usuario, ChatService service) {
        this.usuario = usuario;
        this.chatService = service;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Chat com " + usuario);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());

        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendMessage());

        JButton endChatButton = new JButton("Encerrar Chat");
        endChatButton.addActionListener(e -> {
            chatService.enviarFimChat(usuario);
            dispose();
        });

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1,2,5,0));
        buttonPanel.add(sendButton);
        buttonPanel.add(endChatButton);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if(!msg.isEmpty()) {
            chatService.enviarMensagemIndividual(usuario, msg);
            addMessage("Você: " + msg);
            messageField.setText("");
        }
    }

    public void addMessage(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
}
