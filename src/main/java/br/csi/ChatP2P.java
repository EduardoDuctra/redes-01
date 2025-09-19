package br.csi;

import java.awt.BorderLayout;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
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
    private Map<String, UsuarioOnline> usuariosConectados;

    public ChatP2P(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
        this.sessoesUsuarios = new HashMap<>();
        this.modeloUsuariosOnline = new DefaultListModel<>();
        this.usuariosConectados = new HashMap<>();
        this.inicializarInterface();
        this.inicializarRede();
        this.iniciarTimerSonda();
        this.iniciarTimerVerificacaoAtividade();
        this.iniciarTimerLimpezaInativos();
    }

    private void inicializarInterface() {
        this.setTitle("Chat P2P - " + this.nomeUsuario);
        this.setSize(600, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.listaUsuariosOnline = new JList(this.modeloUsuariosOnline);
        this.listaUsuariosOnline.setSelectionMode(0);
        this.listaUsuariosOnline.addListSelectionListener((evento) -> {
            if (!evento.getValueIsAdjusting()) {
                String usuarioSelecionado = this.listaUsuariosOnline.getSelectedValue();
                if (usuarioSelecionado != null) {
                    this.abrirJanelaChat(usuarioSelecionado);
                }
            }
        });

        JButton botaoGrupo = new JButton("Chat em Grupo");
        botaoGrupo.addActionListener((evento) -> {
            this.abrirChatGrupo();
        });

        this.setLayout(new BorderLayout());
        this.add(new JScrollPane(this.listaUsuariosOnline), BorderLayout.CENTER);
        this.add(botaoGrupo, BorderLayout.SOUTH);
    }

    private void inicializarRede() {
        try {
            this.socket = new DatagramSocket(8080);
            (new Thread(this::receberMensagens)).start();
            System.out.println("👤 Usuário " + this.nomeUsuario + " ouvindo na porta 8080");
        } catch (BindException erro) {
            JOptionPane.showMessageDialog(this, "Erro: Porta 8080 já em uso. Execute em outro computador.");
            System.exit(1);
        } catch (Exception erro) {
            JOptionPane.showMessageDialog(this, "Erro ao criar socket: " + erro.getMessage());
        }
    }

    private void iniciarTimerSonda() {
        this.timerSonda = new Timer(5000, (evento) -> {
            this.enviarSonda();
        });
        this.timerSonda.start();
    }

    private void iniciarTimerVerificacaoAtividade() {
        Timer timerAtividade = new Timer(10000, (evento) -> {
            this.verificarUsuariosAtivos();
        });
        timerAtividade.start();
    }

    private void iniciarTimerLimpezaInativos() {
        Timer timerLimpeza = new Timer(10000, (evento) -> {
            this.removerUsuariosInativos();
        });
        timerLimpeza.start();
    }

    private void verificarUsuariosAtivos() {
        long tempoAtual = System.currentTimeMillis();
        System.out.println("=== USUÁRIOS ATIVOS (últimos 10s) ===");

        if (usuariosConectados.isEmpty()) {
            System.out.println("Nenhum usuário ativo no momento");
        } else {
            for (UsuarioOnline usuario : usuariosConectados.values()) {
                long tempoInativo = tempoAtual - usuario.getUltimoSinal();

                if (tempoInativo <= 10000) {
                    System.out.println("✅ " + usuario.getUsuario() + " - " + usuario.getStatus() +
                            " (inativo há " + (tempoInativo/1000) + "s)");
                }
            }
        }
        System.out.println("===================================");
    }

    private void removerUsuariosInativos() {
        long tempoAtual = System.currentTimeMillis();
        ArrayList<String> usuariosParaRemover = new ArrayList<>();

        for (Map.Entry<String, UsuarioOnline> entrada : usuariosConectados.entrySet()) {
            UsuarioOnline usuario = entrada.getValue();
            long tempoInativo = tempoAtual - usuario.getUltimoSinal();

            if (tempoInativo > 30000) {
                usuariosParaRemover.add(entrada.getKey());
            }
        }

        for (String usuario : usuariosParaRemover) {
            usuariosConectados.remove(usuario);
            modeloUsuariosOnline.removeElement(usuario);

            UserSession sessao = sessoesUsuarios.get(usuario);
            if (sessao != null) {
                sessao.dispose();
                sessoesUsuarios.remove(usuario);
            }

            System.out.println("❌ Usuário " + usuario + " removido por inatividade (30s+)");
        }
    }

    private void enviarSonda() {
        try {
            JSONObject mensagemSonda = new JSONObject();
            mensagemSonda.put("tipoMensagem", "sonda");
            mensagemSonda.put("usuario", this.nomeUsuario);
            mensagemSonda.put("status", this.status);

            String mensagemJson = mensagemSonda.toString();
            byte[] dados = mensagemJson.getBytes();
            InetAddress enderecoBroadcast = InetAddress.getByName("255.255.255.255");
            DatagramPacket pacote = new DatagramPacket(dados, dados.length, enderecoBroadcast, 8080);

            this.socket.send(pacote);
            System.out.println("📡 Sonda enviada: " + this.nomeUsuario + " - " + this.status);
        } catch (Exception erro) {
            System.out.println("❌ Erro ao enviar sonda: " + erro.getMessage());
        }
    }

    private void receberMensagens() {
        byte[] buffer = new byte[1024];

        while (true) {
            try {
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                this.socket.receive(pacote);

                String mensagemRecebida = new String(pacote.getData(), 0, pacote.getLength());
                JSONObject mensagemJson = new JSONObject(mensagemRecebida);
                String tipoMensagem = mensagemJson.getString("tipoMensagem");
                String usuarioRemetente = mensagemJson.getString("usuario");

                if (!usuarioRemetente.equals(this.nomeUsuario)) {
                    long tempoAtual = System.currentTimeMillis();

                    // Atualiza ou cria usuário online
                    UsuarioOnline usuarioOnline = usuariosConectados.get(usuarioRemetente);
                    if (usuarioOnline == null) {
                        String statusUsuario = mensagemJson.optString("status", "disponivel");
                        usuarioOnline = new UsuarioOnline(usuarioRemetente, statusUsuario, tempoAtual);
                        usuariosConectados.put(usuarioRemetente, usuarioOnline);

                        SwingUtilities.invokeLater(() -> {
                            if (!modeloUsuariosOnline.contains(usuarioRemetente)) {
                                modeloUsuariosOnline.addElement(usuarioRemetente);
                                System.out.println("👤 Novo usuário online: " + usuarioRemetente);
                            }
                        });
                    } else {
                        usuarioOnline.setUltimoSinal(tempoAtual);
                        usuarioOnline.setStatus(mensagemJson.optString("status", usuarioOnline.getStatus()));
                    }

                    System.out.println("📥 Mensagem recebida de " + usuarioRemetente +
                            " - Tipo: " + tipoMensagem +
                            " - Status: " + usuarioOnline.getStatus());

                    switch (tipoMensagem) {
                        case "sonda":
                            // Já processado acima - apenas atualiza usuário
                            break;
                        case "msg_individual":
                            this.processarMensagemIndividual(mensagemJson, pacote.getAddress());
                            break;
                        case "msg_grupo":
                            this.processarMensagemGrupo(mensagemJson);
                            break;
                        case "fim_chat":
                            this.processarFimChat(usuarioRemetente);
                            break;
                    }
                }
            } catch (Exception erro) {
                System.out.println("❌ Erro ao receber mensagem: " + erro.getMessage());
            }
        }
    }

    private void processarMensagemIndividual(JSONObject mensagem, InetAddress endereco) {
        String usuarioRemetente = mensagem.getString("usuario");
        String conteudoMensagem = mensagem.getString("msg");

        SwingUtilities.invokeLater(() -> {
            UserSession sessao = sessoesUsuarios.get(usuarioRemetente);
            if (sessao == null) {
                sessao = new UserSession(usuarioRemetente, this);
                sessoesUsuarios.put(usuarioRemetente, sessao);
            }

            // Usando o método correto que deve existir em UserSession
            sessao.addMessage(usuarioRemetente + ": " + conteudoMensagem);
            sessao.setVisible(true);
            System.out.println("💬 Mensagem individual de " + usuarioRemetente + ": " + conteudoMensagem);
        });
    }

    private void processarMensagemGrupo(JSONObject mensagem) {
        String usuarioRemetente = mensagem.getString("usuario");
        String conteudoMensagem = mensagem.getString("msg");

        SwingUtilities.invokeLater(() -> {
            if (this.janelaGrupo == null) {
                this.janelaGrupo = new GroupChatWindow(this);
            }

            // Usando o método correto que deve existir em GroupChatWindow
            this.janelaGrupo.addMessage(usuarioRemetente + ": " + conteudoMensagem);
            this.janelaGrupo.setVisible(true);
            System.out.println("👥 Mensagem de grupo de " + usuarioRemetente + ": " + conteudoMensagem);
        });
    }

    private void processarFimChat(String usuario) {
        SwingUtilities.invokeLater(() -> {
            usuariosConectados.remove(usuario);
            modeloUsuariosOnline.removeElement(usuario);

            UserSession sessao = sessoesUsuarios.get(usuario);
            if (sessao != null) {
                sessao.dispose();
                sessoesUsuarios.remove(usuario);
            }

            System.out.println("👋 Usuário " + usuario + " desconectou");
        });
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
        if (this.janelaGrupo == null) {
            this.janelaGrupo = new GroupChatWindow(this);
        }
        this.janelaGrupo.setVisible(true);
    }

    // Método correto - usando o nome original da sua classe
    public void sendIndividualMessage(String usuarioDestino, String mensagem) {
        try {
            JSONObject mensagemJson = new JSONObject();
            mensagemJson.put("tipoMensagem", "msg_individual");
            mensagemJson.put("usuario", this.nomeUsuario);
            mensagemJson.put("status", this.status);
            mensagemJson.put("msg", mensagem);

            this.enviarMensagemBroadcast(mensagemJson.toString());
            System.out.println("✉️ Mensagem individual para " + usuarioDestino + ": " + mensagem);
        } catch (Exception erro) {
            System.out.println("❌ Erro ao enviar mensagem individual: " + erro.getMessage());
        }
    }

    // Método correto - usando o nome original da sua classe
    public void sendGroupMessage(String mensagem) {
        try {
            JSONObject mensagemJson = new JSONObject();
            mensagemJson.put("tipoMensagem", "msg_grupo");
            mensagemJson.put("usuario", this.nomeUsuario);
            mensagemJson.put("status", this.status);
            mensagemJson.put("msg", mensagem);

            this.enviarMensagemBroadcast(mensagemJson.toString());
            System.out.println("📢 Mensagem de grupo: " + mensagem);
        } catch (Exception erro) {
            System.out.println("❌ Erro ao enviar mensagem de grupo: " + erro.getMessage());
        }
    }

    // Método correto - usando o nome original da sua classe
    public void sendEndChat(String usuarioDestino) {
        try {
            JSONObject mensagemJson = new JSONObject();
            mensagemJson.put("tipoMensagem", "fim_chat");
            mensagemJson.put("usuario", this.nomeUsuario);

            this.enviarMensagemBroadcast(mensagemJson.toString());
            System.out.println("🚪 Enviando fim de chat para: " + usuarioDestino);
        } catch (Exception erro) {
            System.out.println("❌ Erro ao enviar fim de chat: " + erro.getMessage());
        }
    }

    private void enviarMensagemBroadcast(String mensagem) {
        try {
            byte[] dados = mensagem.getBytes();
            InetAddress enderecoBroadcast = InetAddress.getByName("255.255.255.255");
            DatagramPacket pacote = new DatagramPacket(dados, dados.length, enderecoBroadcast, 8080);

            // Envia 3 vezes para garantir entrega
            for (int i = 1; i <= 3; i++) {
                this.socket.send(pacote);
                System.out.println("✅ Mensagem enviada " + i + "/3");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception erro) {
            System.out.println("❌ Erro no envio broadcast: " + erro.getMessage());
        }
    }

    // Métodos auxiliares para compatibilidade
    public void enviarMensagemIndividual(String usuarioDestino, String mensagem) {
        this.sendIndividualMessage(usuarioDestino, mensagem);
    }

    public void enviarMensagemGrupo(String mensagem) {
        this.sendGroupMessage(mensagem);
    }

    public void enviarFimChat(String usuarioDestino) {
        this.sendEndChat(usuarioDestino);
    }

    public Map<String, UsuarioOnline> getUsuariosConectados() {
        return new HashMap<>(usuariosConectados);
    }
}