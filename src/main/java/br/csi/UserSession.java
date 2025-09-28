package br.csi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class UserSession extends JFrame {

    private JTextArea areaChat;
    private JTextField campoMensagem;
    private String usuario;
    private ChatService chatService;

    public UserSession(String usuario, ChatService service) {
        super("Chat com " + usuario);
        this.usuario = usuario;
        this.chatService = service;

        areaChat = new JTextArea();
        areaChat.setEditable(false);
        campoMensagem = new JTextField();
        campoMensagem.addActionListener(this::enviarMensagem);

        setLayout(new BorderLayout());
        add(new JScrollPane(areaChat), BorderLayout.CENTER);
        add(campoMensagem, BorderLayout.SOUTH);
        setSize(400,300);
    }

    private void enviarMensagem(ActionEvent e) {
        String msg = campoMensagem.getText();
        if(msg.isEmpty()) return;
        chatService.enviarMensagemIndividual(usuario, msg);
        addMessage("Eu: " + msg);
        campoMensagem.setText("");
    }

    public void addMessage(String msg) {
        areaChat.append(msg + "\n");
    }
}

