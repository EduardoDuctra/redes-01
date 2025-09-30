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

    private JComboBox<String> comboStatus; // Novo: ComboBox para status

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

        // Novo: ComboBox para status
        comboStatus = new JComboBox<>(new String[]{"Disponível", "Indisponível"});
        comboStatus.setSelectedItem("Disponível");
        comboStatus.addActionListener(e -> {
            String statusSelecionado = (String) comboStatus.getSelectedItem();
            chatService.setStatus(statusSelecionado.toLowerCase()); // atualiza no serviço
        });

        JPanel topoPanel = new JPanel(new BorderLayout());
        topoPanel.add(comboStatus, BorderLayout.WEST);
        topoPanel.add(botaoGrupo, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(new JScrollPane(listaUsuarios), BorderLayout.CENTER);
        add(topoPanel, BorderLayout.NORTH);
    }

    // ===== Métodos para abrir janelas de chat =====
    public void abrirJanelaChat(String nomeUsuario) {
        User usuario = chatService.getUsuariosConectados().get(nomeUsuario);
        if (usuario == null) return; // usuário não encontrado

        UserSessionWindow sessao = sessoes.get(usuario.getNome());
        if (sessao == null) {
            sessao = new UserSessionWindow(usuario, chatService);
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
            UserSessionWindow sessao = sessoes.remove(usuario.getNome());
            if (sessao != null) sessao.dispose();
        });
    }

    @Override
    public void usuarioAlterado(User usuario) {
        SwingUtilities.invokeLater(() -> listaUsuarios.repaint());
    }

    // ===== MessageListener =====
    @Override
    public void mensagemRecebida(String mensagem, User remetente, boolean chatGeral) {
        // Se usuário está indisponível, não processa mensagens individuais
        if (!chatGeral && chatService.getStatus().equalsIgnoreCase("indisponivel")) return;

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

    // ===== Fim chat =====
    public void fimChatRecebido(User usuario) {
        SwingUtilities.invokeLater(() -> {
            UserSessionWindow sessao = sessoes.remove(usuario.getNome());
            if (sessao != null) {
                sessao.addMessage("⚠️ " + usuario.getNome() + " encerrou o chat.");
                new Timer(3000, e -> sessao.dispose()).start();
            }
        });
    }
}
