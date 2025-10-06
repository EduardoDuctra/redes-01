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
    private JList<String> listaUsuariosConectados;
    private Map<String, UserSessionWindow> sessoes;
    private UserSessionWindow janelaGrupo;
    private JComboBox<String> comboStatus;

    public ChatP2PUI(ChatService chatService) {
        this.chatService = chatService;
        this.sessoes = new HashMap<>();
        this.modeloUsuarios = new DefaultListModel<>();


        chatService.addListenerMensagem(this);
        chatService.addListenerUsuario(this);

        inicializarInterface();
    }


    private void inicializarInterface() {
        setTitle("Chat P2P - " + chatService.getNomeUsuario());
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        listaUsuariosConectados = new JList<>(modeloUsuarios);
        listaUsuariosConectados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Renderiza usuários com bolinha verde (ativo) ou amarela (inativo)
        listaUsuariosConectados.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {

                JPanel panel = new JPanel(new BorderLayout());
                panel.setOpaque(true);

                JLabel nomeLabel = new JLabel(value.toString());
                //Pega o User através do nome exibido na lista (value.toString();
                // O método getUsuariosConectados() retorna um Map<String, User> com todos os usuários online.
                User u = chatService.getUsuariosConectados().get(value.toString());

                //visualização do disponível/indisponível - status
                JLabel statusLabel = new JLabel("\u25CF");
                if (u != null) {
                    statusLabel.setForeground("disponivel".equalsIgnoreCase(u.getStatus()) ? Color.GREEN : Color.YELLOW);
                }
                else {
                    statusLabel.setForeground(Color.GRAY);
                }

                panel.add(statusLabel, BorderLayout.WEST);
                panel.add(nomeLabel, BorderLayout.CENTER);


                if (isSelected) {
                    panel.setBackground(list.getSelectionBackground());
                    nomeLabel.setForeground(list.getSelectionForeground());
                }
                else {
                    panel.setBackground(list.getBackground());
                    nomeLabel.setForeground(list.getForeground());
                }

                return panel;
            }
        });

        //ouvinte do clique do mouse
        //clicando duas vezes em cima do nome do User, abre a janela
        listaUsuariosConectados.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirChatIndividual(listaUsuariosConectados.getSelectedValue());
                }
            }
        });

        // Botão para chat em grupo
        JButton botaoGrupo = new JButton("Chat em Grupo");
        botaoGrupo.addActionListener(e -> abrirChatGrupo());

        // ComboBox para status
        comboStatus = new JComboBox<>(new String[]{"disponivel", "indisponivel"});
        comboStatus.setSelectedItem("disponivel");

        //ouvinte para quando o User muda de status.
        //Atualiza o status no chatService e renderiza novamente a bolinha de status
        comboStatus.addActionListener(e -> {
            String statusSelecionado = ((String) comboStatus.getSelectedItem()).toLowerCase();
            chatService.setStatus(statusSelecionado);

            // Atualiza a bolinha e a lista imediatamente
            listaUsuariosConectados.repaint();
        });

        JPanel topoPanel = new JPanel(new BorderLayout());
        topoPanel.add(comboStatus, BorderLayout.WEST);
        topoPanel.add(botaoGrupo, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(new JScrollPane(listaUsuariosConectados), BorderLayout.CENTER);
        add(topoPanel, BorderLayout.NORTH);
    }

    public void abrirChatIndividual(String nomeUsuario) {

        User usuario = chatService.getUsuariosConectados().get(nomeUsuario);

        UserSessionWindow janelaSessao = sessoes.get(usuario.getNome());
        if (janelaSessao == null) {
            janelaSessao = new UserSessionWindow(usuario, chatService);
            sessoes.put(usuario.getNome(), janelaSessao);
        }
        janelaSessao.setVisible(true);
    }

    public void abrirChatGrupo() {
        if (janelaGrupo == null){
            janelaGrupo = new UserSessionWindow(chatService);
        }
        janelaGrupo.setVisible(true);
    }


    //UserListener
    //sempre que um novo User entra, coloca ele na lista de Users ativos
    @Override
    public void usuarioAdicionado(User usuario) {
        SwingUtilities.invokeLater(() -> modeloUsuarios.addElement(usuario.getNome()));
    }

    //UserListener
    @Override
    public void usuarioRemovido(User usuario) {
        SwingUtilities.invokeLater(() -> {
            //retira da lista de Usuários Ativos
            modeloUsuarios.removeElement(usuario.getNome());
            UserSessionWindow janelaSessao = sessoes.remove(usuario.getNome());
            //fecha a janela
            if (janelaSessao != null) {
                janelaSessao.dispose();
            }
        });
    }

    //UserListener
    //Se o UsuarioMudar o nome ou status altera a aba do chat
    //Porque o mesmo usuario pode sair e entrar com nomes diferentes (uso o IP do pc como identificador)
    @Override
    public void usuarioAlterado(User usuario) {
        SwingUtilities.invokeLater(() -> {
            listaUsuariosConectados.repaint();
            UserSessionWindow JanelaSessao = sessoes.get(usuario.getNome());
            if (JanelaSessao != null) {
                JanelaSessao.atualizarUsuario(usuario);
            }
        });
    }


    //MessageListener
    @Override
    public void mensagemRecebida(String mensagem, User remetente, boolean chatGeral) {
        SwingUtilities.invokeLater(() -> {
            if (chatGeral) {
                if (janelaGrupo == null) janelaGrupo = new UserSessionWindow(chatService);
                janelaGrupo.addMessage(remetente.getNome() + ": " + mensagem);
                janelaGrupo.setVisible(true);
            }
            else {
                UserSessionWindow JanelaSessao = sessoes.get(remetente.getNome());

                //caso 1: janela existente
                if (JanelaSessao != null) {
                    JanelaSessao.addMessage(remetente.getNome() + ": " + mensagem);
                    JanelaSessao.setVisible(true);
                    JanelaSessao.toFront();
                    return;
                }

                //caso 2: janela não existente
                JanelaSessao = new UserSessionWindow(remetente, chatService);
                JanelaSessao.addMessage(remetente.getNome() + ": " + mensagem);
                sessoes.put(remetente.getNome(), JanelaSessao);
                JanelaSessao.setVisible(true);
            }
        });
    }

    //Quando o Usuário A clica no botão "ENCERRAR CHAT" envia uma mensagem ao Usuário B e depois de 3s fecha a janela
    //Mensagem tipo FIM_CHAT está implementada na classe ChatService
    public void fimChatRecebido(User usuario) {
        SwingUtilities.invokeLater(() -> {
            UserSessionWindow janelaSessao = sessoes.remove(usuario.getNome());
            if (janelaSessao != null) {
                janelaSessao.addMessage(usuario.getNome() + " encerrou o chat com você.");
                new Timer(3000, e -> janelaSessao.dispose()).start();
            }
        });
    }
}
