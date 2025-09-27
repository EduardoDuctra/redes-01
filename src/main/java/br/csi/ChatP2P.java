package br.csi;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.net.*;
import java.util.*;
import org.json.JSONObject;

public class ChatP2P extends JFrame {

    private String nomeUsuario;
    private String status = "disponivel";
    private DatagramSocket socket;
    private Map<String, UserSession> sessoesUsuarios;
    private DefaultListModel<String> modeloUsuariosOnline;
    private JList<String> listaUsuariosOnline;
    private GroupChatWindow janelaGrupo;
    private Timer timerSonda;
    private Timer timerAtividade;
    private Timer timerLimpeza;
    private Map<String, UsuarioOnline> usuariosConectados;

    public ChatP2P(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
        this.sessoesUsuarios = new HashMap<>();
        this.modeloUsuariosOnline = new DefaultListModel<>();
        this.usuariosConectados = new HashMap<>();

        inicializarInterface();
        inicializarRede();
        iniciarTimers();
    }

    private void inicializarInterface() {
        setTitle("Chat P2P - " + nomeUsuario);
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        listaUsuariosOnline = new JList<>(modeloUsuariosOnline);
        listaUsuariosOnline.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaUsuariosOnline.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String usuarioSelecionado = listaUsuariosOnline.getSelectedValue();
                if (usuarioSelecionado != null) abrirJanelaChat(usuarioSelecionado);
            }
        });

        JButton botaoGrupo = new JButton("Chat em Grupo");
        botaoGrupo.addActionListener(e -> abrirChatGrupo());

        setLayout(new BorderLayout());
        add(new JScrollPane(listaUsuariosOnline), BorderLayout.CENTER);
        add(botaoGrupo, BorderLayout.SOUTH);
    }

    private void inicializarRede() {
        try {
            socket = new DatagramSocket(8080);
            new Thread(this::receberMensagens).start();
            System.out.println("👤 Usuário " + nomeUsuario + " ouvindo na porta 8080");
        } catch (BindException e) {
            JOptionPane.showMessageDialog(this, "Erro: Porta 8080 já em uso.");
            System.exit(1);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao criar socket: " + e.getMessage());
        }
    }

    private void iniciarTimers() {
        // Timer sonda (5s)
        timerSonda = new Timer(5000, e -> enviarSonda());
        timerSonda.start();

        // Timer verificação de atividade (10s)
        timerAtividade = new Timer(10000, e -> verificarUsuariosAtivos());
        timerAtividade.start();

        // Timer limpeza de inativos (10s)
        timerLimpeza = new Timer(10000, e -> removerUsuariosInativos());
        timerLimpeza.start();
    }

    private void enviarSonda() {
        try {
            JSONObject msg = new JSONObject();
            msg.put("tipoMensagem", "sonda");
            msg.put("usuario", nomeUsuario);
            msg.put("status", status);

            enviarMensagemBroadcast(msg.toString());
        } catch (Exception e) {
            System.out.println("❌ Erro ao enviar sonda: " + e.getMessage());
        }
    }

    private void receberMensagens() {
        byte[] buffer = new byte[1024];

        while (true) {
            try {
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                socket.receive(pacote);

                String mensagemRecebida = new String(pacote.getData(), 0, pacote.getLength());
                JSONObject json = new JSONObject(mensagemRecebida);

                String tipo = json.getString("tipoMensagem");
                String remetente = json.getString("usuario");

                if (!remetente.equals(nomeUsuario)) {
                    long agora = System.currentTimeMillis();

                    UsuarioOnline usuario = usuariosConectados.get(remetente);
                    if (usuario == null) {
                        usuario = new UsuarioOnline(remetente, json.optString("status", "disponivel"), agora);
                        usuariosConectados.put(remetente, usuario);

                        SwingUtilities.invokeLater(() -> {
                            if (!modeloUsuariosOnline.contains(remetente)) {
                                modeloUsuariosOnline.addElement(remetente);
                                System.out.println("👤 Novo usuário online: " + remetente);
                            }
                        });
                    } else {
                        usuario.setUltimoSinal(agora);
                        usuario.setStatus(json.optString("status", usuario.getStatus()));
                    }

                    switch (tipo) {
                        case "sonda": break;
                        case "msg_individual": processarMensagemIndividual(json); break;
                        case "msg_grupo": processarMensagemGrupo(json); break;
                        case "fim_chat": processarFimChat(remetente); break;
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Erro ao receber mensagem: " + e.getMessage());
            }
        }
    }

    private void verificarUsuariosAtivos() {
        long agora = System.currentTimeMillis();
        System.out.println("=== USUÁRIOS ATIVOS (últimos 10s) ===");

        if (usuariosConectados.isEmpty()) {
            System.out.println("Nenhum usuário ativo");
        } else {
            for (UsuarioOnline u : usuariosConectados.values()) {
                long inativo = agora - u.getUltimoSinal();
                String status;
                if (inativo <= 10000) {
                    status = "✅";
                } else {
                    status = "⚠️ Inativo";
                }
                System.out.println(status + " " + u.getUsuario());
            }
        }

        System.out.println("===================================");
    }

    private void removerUsuariosInativos() {
        long agora = System.currentTimeMillis();
        ArrayList<String> remover = new ArrayList<>();

        for (UsuarioOnline u : usuariosConectados.values()) {
            if (agora - u.getUltimoSinal() > 30000) remover.add(u.getUsuario());
        }

        for (String usuario : remover) {
            usuariosConectados.remove(usuario);
            modeloUsuariosOnline.removeElement(usuario);

            UserSession sessao = sessoesUsuarios.get(usuario);
            if (sessao != null) sessao.dispose();
            sessoesUsuarios.remove(usuario);

            System.out.println("❌ Usuário " + usuario + " removido por inatividade (30s+)");
        }
    }

    private void processarMensagemIndividual(JSONObject msg) {
        String remetente = msg.getString("usuario");
        String conteudo = msg.getString("msg");

        SwingUtilities.invokeLater(() -> {
            UserSession sessao = sessoesUsuarios.get(remetente);
            if (sessao == null) {
                sessao = new UserSession(remetente, this);
                sessoesUsuarios.put(remetente, sessao);
            }
            sessao.addMessage(remetente + ": " + conteudo);
            sessao.setVisible(true);
            System.out.println("💬 Mensagem de " + remetente + ": " + conteudo);
        });
    }

    private void processarMensagemGrupo(JSONObject msg) {
        String remetente = msg.getString("usuario");
        String conteudo = msg.getString("msg");

        SwingUtilities.invokeLater(() -> {
            if (janelaGrupo == null) janelaGrupo = new GroupChatWindow(this);
            janelaGrupo.addMessage(remetente + ": " + conteudo);
            janelaGrupo.setVisible(true);
            System.out.println("👥 Mensagem de grupo de " + remetente + ": " + conteudo);
        });
    }

    private void processarFimChat(String usuario) {
        SwingUtilities.invokeLater(() -> {
            usuariosConectados.remove(usuario);
            modeloUsuariosOnline.removeElement(usuario);

            UserSession sessao = sessoesUsuarios.get(usuario);
            if (sessao != null) {
                // Mostra mensagem no chat antes de fechar
                sessao.addMessage("⚠️ O usuário " + usuario + " encerrou o chat.");

                // Fecha a janela após 3 segundos
                new javax.swing.Timer(3000, e -> {
                    sessao.dispose();
                    sessoesUsuarios.remove(usuario);
                }).start();
            }

            System.out.println("👋 Usuário " + usuario + " desconectou");
        });
    }

    private void enviarMensagemBroadcast(String mensagem) {
        try {
            byte[] dados = mensagem.getBytes();
            InetAddress broadcast = InetAddress.getByName("255.255.255.255");
            DatagramPacket pacote = new DatagramPacket(dados, dados.length, broadcast, 8080);

            for (int i = 1; i <= 3; i++) {
                socket.send(pacote);
                Thread.sleep(100);
            }
        } catch (Exception e) {
            System.out.println("❌ Erro no envio broadcast: " + e.getMessage());
        }
    }

    public void sendIndividualMessage(String usuario, String mensagem) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipoMensagem", "msg_individual");
            json.put("usuario", nomeUsuario);
            json.put("status", status);
            json.put("msg", mensagem);

            enviarMensagemBroadcast(json.toString());
        } catch (Exception e) {
            System.out.println("❌ Erro ao enviar mensagem individual: " + e.getMessage());
        }
    }

    public void sendGroupMessage(String mensagem) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipoMensagem", "msg_grupo");
            json.put("usuario", nomeUsuario);
            json.put("status", status);
            json.put("msg", mensagem);

            enviarMensagemBroadcast(json.toString());
        } catch (Exception e) {
            System.out.println("❌ Erro ao enviar mensagem de grupo: " + e.getMessage());
        }
    }

    public void sendEndChat(String usuarioDestino) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipoMensagem", "fim_chat");
            json.put("usuario", nomeUsuario);

            enviarMensagemBroadcast(json.toString());
        } catch (Exception e) {
            System.out.println("❌ Erro ao enviar fim de chat: " + e.getMessage());
        }
    }

    public void abrirJanelaChat(String usuario) {
        UserSession sessao = sessoesUsuarios.get(usuario);
        if (sessao == null) {
            sessao = new UserSession(usuario, this);
            sessoesUsuarios.put(usuario, sessao);
        }
        sessao.setVisible(true);
    }

    public void abrirChatGrupo() {
        if (janelaGrupo == null) janelaGrupo = new GroupChatWindow(this);
        janelaGrupo.setVisible(true);
    }



    // Getters
    public Map<String, UsuarioOnline> getUsuariosConectados() {
        return usuariosConectados;
    }

    public Map<String, UserSession> getSessoesUsuarios() {
        return sessoesUsuarios;
    }

    public DefaultListModel<String> getModeloUsuariosOnline() {
        return modeloUsuariosOnline;
    }


}
