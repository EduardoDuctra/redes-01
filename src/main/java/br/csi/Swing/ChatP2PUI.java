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
    private JComboBox<String> comboStatus; // ComboBox para status

    public ChatP2PUI(ChatService chatService) {
        this.chatService = chatService;
        this.sessoes = new HashMap<>();
        this.modeloUsuarios = new DefaultListModel<>();

        // Registra listeners
        chatService.addListenerMensagem(this);
        chatService.addListenerUsuario(this);

        inicializarInterface();
    }

    private void inicializarInterface() {
        setTitle("Chat P2P - " + chatService.getNomeUsuario());
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Lista de usuários
        listaUsuarios = new JList<>(modeloUsuarios);
        listaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Renderiza usuários com bolinha verde (ativo) ou amarela (inativo)
        listaUsuarios.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {

                JPanel panel = new JPanel(new BorderLayout());
                panel.setOpaque(true);

                JLabel nomeLabel = new JLabel(value.toString());
                User u = chatService.getUsuariosConectados().get(value.toString());

                // Bolinha de status
                JLabel statusLabel = new JLabel("\u25CF"); // círculo Unicode
                if (u != null) {
                    statusLabel.setForeground("disponivel".equalsIgnoreCase(u.getStatus()) ? Color.GREEN : Color.YELLOW);
                } else {
                    statusLabel.setForeground(Color.GRAY);
                }

                panel.add(statusLabel, BorderLayout.WEST);
                panel.add(nomeLabel, BorderLayout.CENTER);

                // Mantém a seleção visual
                if (isSelected) {
                    panel.setBackground(list.getSelectionBackground());
                    nomeLabel.setForeground(list.getSelectionForeground());
                } else {
                    panel.setBackground(list.getBackground());
                    nomeLabel.setForeground(list.getForeground());
                }

                return panel;
            }
        });

        listaUsuarios.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) abrirJanelaChat(listaUsuarios.getSelectedValue());
            }
        });

        // Botão para chat em grupo
        JButton botaoGrupo = new JButton("Chat em Grupo");
        botaoGrupo.addActionListener(e -> abrirChatGrupo());

        // ComboBox para status
        comboStatus = new JComboBox<>(new String[]{"Disponível", "Indisponível"});
        comboStatus.setSelectedItem("Disponível");
        comboStatus.addActionListener(e -> {
            String statusSelecionado = ((String) comboStatus.getSelectedItem()).toLowerCase();
            chatService.setStatus(statusSelecionado);

            // Atualiza a bolinha e a lista imediatamente
            listaUsuarios.repaint();
        });

        JPanel topoPanel = new JPanel(new BorderLayout());
        topoPanel.add(comboStatus, BorderLayout.WEST);
        topoPanel.add(botaoGrupo, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(new JScrollPane(listaUsuarios), BorderLayout.CENTER);
        add(topoPanel, BorderLayout.NORTH);
    }

    // ===== Abrir chat individual =====
    public void abrirJanelaChat(String nomeUsuario) {
        User usuario = chatService.getUsuariosConectados().get(nomeUsuario);

        if (usuario == null) {
            JOptionPane.showMessageDialog(this,
                    "Usuário não está mais conectado.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Bloqueia se indisponível
        if (!"disponivel".equalsIgnoreCase(usuario.getStatus())) {
            JOptionPane.showMessageDialog(this, "Usuário indisponível ou inativo.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UserSessionWindow sessao = sessoes.get(usuario.getNome());
        if (sessao == null) {
            sessao = new UserSessionWindow(usuario, chatService);
            sessoes.put(usuario.getNome(), sessao);
        }
        sessao.setVisible(true);
    }

    // ===== Abrir chat em grupo =====
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
            listaUsuarios.repaint();
            UserSessionWindow sessao = sessoes.get(usuario.getNome());
            if (sessao != null) {
                sessao.atualizarUsuario(usuario);
            }
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

                // 🔹 Se a janela já existe, só adiciona a mensagem
                if (sessao != null) {
                    sessao.addMessage(remetente.getNome() + ": " + mensagem);
                    sessao.setVisible(true);
                    sessao.toFront(); // garante que fique visível
                    return;
                }

                // 🔹 Caso não exista, cria e adiciona ao mapa
                sessao = new UserSessionWindow(remetente, chatService);
                sessao.addMessage(remetente.getNome() + ": " + mensagem);
                sessoes.put(remetente.getNome(), sessao);
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
