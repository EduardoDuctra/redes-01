package br.csi.Swing;

import br.csi.Listener.MessageListener;
import br.csi.Listener.UserListener;
import br.csi.Model.User;
import br.csi.Service.ChatService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class ChatP2PUI extends JFrame implements MessageListener, UserListener {

    private ChatService chatService;
    private DefaultListModel<String> modeloUsuarios;
    private JList<String> listaUsuarios;
    private Map<String, UserSessionWindow> sessoes;
    private GroupChatWindow janelaGrupo;

    public ChatP2PUI(ChatService chatService) {
        this.chatService = chatService;
        this.sessoes = new HashMap<>();
        this.modeloUsuarios = new DefaultListModel<>();

        // Registra listeners
        chatService.addMessageListener(this);
        chatService.addUserListener(this);

        inicializarInterface();
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

    // ===== Métodos para abrir janelas de chat =====
    public void abrirJanelaChat(String nomeUsuario) {
        User usuario = chatService.getUsuariosConectados().get(nomeUsuario);
        if (usuario == null) return; // usuário não encontrado

        UserSessionWindow sessao = sessoes.get(usuario.getNome());
        if (sessao == null) {
            sessao = new UserSessionWindow(usuario, chatService); // passar o objeto User
            sessoes.put(usuario.getNome(), sessao);
        }
        sessao.setVisible(true);
    }

    public void abrirChatGrupo() {
        if (janelaGrupo == null) janelaGrupo = new GroupChatWindow(chatService);
        janelaGrupo.setVisible(true);
    }

    // ===== UserListener =====
    @Override
    public void usuarioAdicionado(User usuario) {
        SwingUtilities.invokeLater(() -> modeloUsuarios.addElement(usuario.getNome()));
    }

    @Override
    public void usuarioRemovido(User usuario) {
        SwingUtilities.invokeLater(() -> {
            modeloUsuarios.removeElement(usuario.getNome());
            UserSessionWindow sessao = sessoes.remove(usuario.getNome());
            if (sessao != null) sessao.dispose();
        });
    }

    @Override
    public void usuarioAlterado(User usuario) {
        SwingUtilities.invokeLater(() -> {
            // Atualiza status do usuário na lista, se necessário
            listaUsuarios.repaint();
        });
    }

    // ===== MessageListener =====
    @Override
    public void mensagemRecebida(String mensagem, User remetente, boolean chatGeral) {
        SwingUtilities.invokeLater(() -> {
            if (chatGeral) {
                if (janelaGrupo == null) janelaGrupo = new GroupChatWindow(chatService);
                janelaGrupo.addMessage(remetente.getNome() + ": " + mensagem);
                janelaGrupo.setVisible(true);
            } else {
                UserSessionWindow sessao = sessoes.get(remetente.getNome());
                if (sessao == null) {
                    sessao = new UserSessionWindow(remetente, chatService);
                    sessoes.put(remetente.getNome(), sessao);
                }
                sessao.addMessage(remetente.getNome() + ": " + mensagem);
                sessao.setVisible(true);
            }
        });
    }

    public void fimChatRecebido(User usuario) {
        SwingUtilities.invokeLater(() -> {
            // Recupera a sessão correspondente
            UserSessionWindow sessao = sessoes.remove(usuario.getNome());

            if (sessao != null) {
                // Adiciona mensagem informando que o outro usuário encerrou
                sessao.addMessage("⚠️ " + usuario.getNome() + " encerrou o chat.");

                // Fecha a janela após 3 segundos
                new Timer(3000, e -> sessao.dispose()).start();
            }

            // Remove o usuário da lista de contatos
            modeloUsuarios.removeElement(usuario.getNome());
        });
    }

}
