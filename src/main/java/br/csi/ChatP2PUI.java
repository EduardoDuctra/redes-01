package br.csi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class ChatP2PUI extends JFrame implements ChatService.ChatListener {

    private ChatService chatService;
    private DefaultListModel<String> modeloUsuarios;
    private JList<String> listaUsuarios;
    private Map<String, UserSession> sessoes;
    private GroupChatWindow janelaGrupo;

    public ChatP2PUI(ChatService chatService) {
        this.chatService = chatService;
        this.sessoes = new HashMap<>();
        this.modeloUsuarios = new DefaultListModel<>();
        inicializarInterface();
        chatService.addListener(this);
    }

    private void inicializarInterface() {
        setTitle("Chat P2P - " + chatService.getNomeUsuario());
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        listaUsuarios = new JList<>(modeloUsuarios);
        listaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaUsuarios.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) abrirJanelaChat(listaUsuarios.getSelectedValue());
            }
        });

        JButton botaoGrupo = new JButton("Chat em Grupo");
        botaoGrupo.addActionListener(e -> abrirChatGrupo());

        setLayout(new BorderLayout());
        add(new JScrollPane(listaUsuarios), BorderLayout.CENTER);
        add(botaoGrupo, BorderLayout.SOUTH);
    }

    public void abrirJanelaChat(String usuario) {
        UserSession sessao = sessoes.get(usuario);
        if (sessao == null) {
            sessao = new UserSession(usuario, chatService);
            sessoes.put(usuario, sessao);
        }
        sessao.setVisible(true);
    }

    public void abrirChatGrupo() {
        if (janelaGrupo == null) janelaGrupo = new GroupChatWindow(chatService);
        janelaGrupo.setVisible(true);
    }

    @Override
    public void usuarioAdicionado(UsuarioOnline usuario) {
        SwingUtilities.invokeLater(() -> modeloUsuarios.addElement(usuario.getUsuario()));
    }

    @Override
    public void usuarioRemovido(String usuario) {
        SwingUtilities.invokeLater(() -> {
            modeloUsuarios.removeElement(usuario);
            UserSession sessao = sessoes.remove(usuario);
            if (sessao != null) sessao.dispose();
        });
    }

    @Override
    public void mensagemIndividualRecebida(String remetente, String mensagem) {
        SwingUtilities.invokeLater(() -> {
            UserSession sessao = sessoes.get(remetente);
            if (sessao == null) {
                sessao = new UserSession(remetente, chatService);
                sessoes.put(remetente, sessao);
            }
            sessao.addMessage(remetente + ": " + mensagem);
            sessao.setVisible(true);
        });
    }

    @Override
    public void mensagemGrupoRecebida(String remetente, String mensagem) {
        SwingUtilities.invokeLater(() -> {
            if (janelaGrupo == null) janelaGrupo = new GroupChatWindow(chatService);
            janelaGrupo.addMessage(remetente + ": " + mensagem);
            janelaGrupo.setVisible(true);
        });
    }

    @Override
    public void fimChatRecebido(String usuario) {
        SwingUtilities.invokeLater(() -> {
            UserSession sessao = sessoes.remove(usuario);
            if (sessao != null) {
                sessao.addMessage("⚠️ O usuário encerrou o chat.");
                new Timer(3000, e -> sessao.dispose()).start();
            }
            modeloUsuarios.removeElement(usuario);
        });
    }
}
