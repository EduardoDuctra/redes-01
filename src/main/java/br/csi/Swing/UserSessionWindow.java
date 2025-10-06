package br.csi.Swing;

import br.csi.Model.User;
import br.csi.Service.ChatService;

import javax.swing.*;
import java.awt.*;

public class UserSessionWindow extends JFrame {

    private User usuario;
    private ChatService chatService;
    private JTextArea chatArea;
    private JTextField mensagemArea;
    private JButton enviarBotao;
    private boolean chatGrupo;

    //chat individual
    public UserSessionWindow(User usuario, ChatService service) {
        this.usuario = usuario;
        this.chatService = service;
        this.chatGrupo = false;

        initializeUI();
        atualizarBotaoStatus();
    }

    //chat grupo
    public UserSessionWindow(ChatService service) {
        this.usuario = null;
        this.chatService = service;
        this.chatGrupo = true;

        initializeUI();
    }

    private void initializeUI() {
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        mensagemArea = new JTextField();
        mensagemArea.addActionListener(e -> sendMessage());

        enviarBotao = new JButton("Enviar");
        enviarBotao.addActionListener(e -> sendMessage());

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(mensagemArea, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.add(enviarBotao);

        // Botão de encerrar no chat individual
        if (!chatGrupo) {
            JButton endChatButton = new JButton("Encerrar Chat");
            endChatButton.addActionListener(e -> encerrarChat());
            buttonPanel.add(endChatButton);
        }

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);


        if (chatGrupo) {
            setTitle("Chat em Grupo");
        } else {
            setTitle("Chat com " + usuario.getNome());
        }
    }


    private void encerrarChat() {
        String[] opcoes = {"Sim", "Não"};
        int confirm = JOptionPane.showOptionDialog(
                this,
                "Deseja realmente encerrar o chat com " + usuario.getNome() + "?",
                "Encerrar Chat",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opcoes,
                opcoes[0]
        );

        if (confirm == JOptionPane.YES_OPTION) {
            chatService.enviarFimChat(usuario.getNome());
            dispose();
        }
    }



    private void sendMessage() {
        String msg = mensagemArea.getText().trim();

        chatService.enviarMensagem(msg, usuario, chatGrupo);
        addMessage("Você: " + msg);
        mensagemArea.setText("");
    }


    //exibe a mensagem recebida na área do chat
    public void addMessage(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    // se o status for indisponivel, bloqueia o campo de enviar menagem
    private void atualizarBotaoStatus() {
        if (!chatGrupo && usuario != null) {
            boolean disponivel = !"indisponivel".equalsIgnoreCase(usuario.getStatus());
            enviarBotao.setEnabled(disponivel);
            mensagemArea.setEditable(disponivel);
        } else {

            //chat em grupo sempre pode mandar mensagem
            enviarBotao.setEnabled(true);
            mensagemArea.setEditable(true);
        }
    }


    //atualiza o status do usuario e o nome dele
    public void atualizarUsuario(User usuario) {
        this.usuario = usuario;
        setTitle("Chat com " + usuario.getNome());
        atualizarBotaoStatus();
    }
}
