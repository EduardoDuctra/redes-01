package br.csi.Service;

import br.csi.Listener.MessageListener;
import br.csi.Listener.UserListener;
import br.csi.Model.Mensagem;
import br.csi.Model.TipoMensagem;
import br.csi.Model.User;
import br.csi.Swing.ChatP2PUI;

import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatService {

    private String nomeUsuario;
    private String status = "disponivel";
    private DatagramSocket socket;
    private Map<String, User> usuariosConectados;

    private List<MessageListener> messageListeners;
    private List<UserListener> userListeners;

    public ChatService(String nomeUsuario) throws SocketException {
        this.nomeUsuario = nomeUsuario;
        this.usuariosConectados = new HashMap<>();
        this.messageListeners = new CopyOnWriteArrayList<>();
        this.userListeners = new CopyOnWriteArrayList<>();
        this.socket = new DatagramSocket(8080);
    }

    // ===== Getters =====
    public String getNomeUsuario() { return nomeUsuario; }
    public Map<String, User> getUsuariosConectados() { return usuariosConectados; }

    // ===== Adicionar listeners =====
    public void addMessageListener(MessageListener listener) { messageListeners.add(listener); }
    public void addUserListener(UserListener listener) { userListeners.add(listener); }

    // ===== Rede =====
    public void iniciarRede() {
        iniciarSondas();
        monitorarUsuariosAtivos();
        removerUsuariosInativos();
        receberMensagensAsync();
    }

    // ===== Sondas =====
    private void iniciarSondas() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() { enviarSonda(); }
        }, 0, 5000);
    }

    private void enviarSonda() {
        try {
            Mensagem sonda = Mensagem.criarSonda(nomeUsuario, status);
            enviarMensagemBroadcast(sonda.toJson().toString());
        } catch (Exception e) {
            System.out.println("Erro ao enviar sonda: " + e.getMessage());
        }
    }

    // ===== Monitoramento =====
    private void monitorarUsuariosAtivos() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("=== USUÁRIOS ATIVOS (últimos 10s) ===");
                if (usuariosConectados.isEmpty()) {
                    System.out.println("Nenhum usuário ativo");
                } else {
                    long agora = System.currentTimeMillis();
                    for (User u : usuariosConectados.values()) {
                        System.out.println(u.getNome() + " - status: " + u.getStatus() +
                                " - últimos sinal: " + (agora - u.getUltimoSinal()) + "ms atrás");
                    }
                }
            }
        }, 0, 10000);
    }

    private void removerUsuariosInativos() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                long agora = System.currentTimeMillis();
                for (User u : new ArrayList<>(usuariosConectados.values())) {
                    if (agora - u.getUltimoSinal() > 30000) {
                        usuariosConectados.remove(u.getNome());
                        System.out.println("⚠️ Usuário removido por inatividade: " + u.getNome());
                        for (UserListener l : userListeners) l.usuarioRemovido(u);
                    }
                }
            }
        }, 0, 5000);
    }

    // ===== Recebimento de mensagens =====
    private void receberMensagensAsync() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                    socket.receive(pacote);
                    String dados = new String(pacote.getData(), 0, pacote.getLength());
                    Mensagem msg = Mensagem.fromJson(new org.json.JSONObject(dados));

                    if (msg.getRemetente().equals(nomeUsuario)) continue;

                    long agora = System.currentTimeMillis();
                    User u = usuariosConectados.get(msg.getRemetente());
                    if (u == null) {
                        u = new User(msg.getRemetente(),
                                msg.getTipo() == TipoMensagem.SONDA ? msg.getConteudo() : "disponivel",
                                agora);
                        usuariosConectados.put(u.getNome(), u);
                        for (UserListener l : userListeners) l.usuarioAdicionado(u);
                    } else {
                        u.setUltimoSinal(agora);
                        if (msg.getTipo() == TipoMensagem.SONDA) {
                            u.setStatus(msg.getConteudo());
                            for (UserListener l : userListeners) l.usuarioAlterado(u);
                        }
                    }

                    boolean chatGeral = msg.getTipo() == TipoMensagem.MSG_GRUPO;

                    switch (msg.getTipo()) {
                        case MSG_INDIVIDUAL:
                            for (MessageListener l : messageListeners)
                                l.mensagemRecebida(msg.getConteudo(), u, false); // false = mensagem privada
                            break;
                        case MSG_GRUPO:
                            for (MessageListener l : messageListeners)
                                l.mensagemRecebida(msg.getConteudo(), u, true); // true = mensagem de grupo
                            break;
                        case FIM_CHAT:
                            for (UserListener l : userListeners) {
                                if (l instanceof ChatP2PUI) {
                                    ((ChatP2PUI) l).fimChatRecebido(u); // chama a função da UI
                                }
                            }
                            break;
                        case SONDA:
                            // já tratado no status
                            break;
                    }

                } catch (Exception e) {
                    System.out.println("Erro ao receber mensagem: " + e.getMessage());
                }
            }
        }).start();
    }

    // ===== Envio de mensagens =====
    private void enviarMensagemBroadcast(String mensagem) {
        try {
            byte[] dados = mensagem.getBytes();
            InetAddress broadcast = InetAddress.getByName("255.255.255.255");
            DatagramPacket pacote = new DatagramPacket(dados, dados.length, broadcast, 8080);
            socket.send(pacote);
        } catch (Exception e) {
            System.out.println("Erro no broadcast: " + e.getMessage());
        }
    }

    public void enviarMensagemIndividual(String destinatario, String conteudo) {
        try {
            Mensagem msg = Mensagem.criarMsgIndividual(nomeUsuario, destinatario, conteudo);
            enviarMensagemBroadcast(msg.toJson().toString());
        } catch (Exception e) {
            System.out.println("Erro ao enviar msg individual: " + e.getMessage());
        }
    }

    public void enviarMensagemGrupo(String conteudo) {
        try {
            Mensagem msg = Mensagem.criarMsgGrupo(nomeUsuario, conteudo);
            enviarMensagemBroadcast(msg.toJson().toString());
        } catch (Exception e) {
            System.out.println("Erro ao enviar msg grupo: " + e.getMessage());
        }
    }

    public void enviarFimChat(String destinatario) {
        try {
            Mensagem msg = Mensagem.criarFimChat(nomeUsuario, destinatario);
            enviarMensagemBroadcast(msg.toJson().toString());
        } catch (Exception e) {
            System.out.println("Erro ao enviar fim chat: " + e.getMessage());
        }
    }
}
