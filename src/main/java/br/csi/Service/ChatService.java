package br.csi.Service;

import br.csi.Listener.MessageListener;
import br.csi.Listener.UDPService;
import br.csi.Listener.UserListener;
import br.csi.Model.Mensagem;
import br.csi.Model.TipoMensagem;
import br.csi.Model.User;
import br.csi.Swing.ChatP2PUI;

import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatService implements UDPService {

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

        // Cria o próprio usuário no mapa desde o início
        User self = new User(nomeUsuario, status, System.currentTimeMillis());
        usuariosConectados.put(nomeUsuario, self);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String novoStatus) {
        this.status = novoStatus;
        enviarSonda();

        // Atualiza o próprio usuário no mapa e notifica listeners
        User self = usuariosConectados.get(nomeUsuario);
        self.setStatus(status);
        self.setUltimoSinal(System.currentTimeMillis());

        for (UserListener l : userListeners) {
            l.usuarioAlterado(self);
        }
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public Map<String, User> getUsuariosConectados() {
        return usuariosConectados;
    }

    @Override
    public void addListenerMensagem(MessageListener listener) {
        messageListeners.add(listener);
    }

    @Override
    public void addListenerUsuario(UserListener listener) {
        userListeners.add(listener);
    }

    // ===== Rede =====
    public void iniciarRede() {
        iniciarSondas();
        monitorarUsuariosAtivos();
        removerUsuariosInativos();
        receberMensagensAsync();
    }

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
                                " - último sinal: " + (agora - u.getUltimoSinal()) + "ms atrás");
                    }
                }
            }
        }, 0, 10000); // Executa a cada 10s
    }

    private void removerUsuariosInativos() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                long agora = System.currentTimeMillis();
                for (User u : new ArrayList<>(usuariosConectados.values())) {
                    if (!u.getNome().equals(nomeUsuario) && agora - u.getUltimoSinal() > 30000) {
                        usuariosConectados.remove(u.getNome());
                        System.out.println("⚠️ Usuário removido por inatividade: " + u.getNome());

                        for (UserListener l : userListeners) {
                            l.usuarioRemovido(u);
                        }
                    }
                }
            }
        }, 0, 5000); // Verifica a cada 5s
    }

    private void receberMensagensAsync() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                    socket.receive(pacote);

                    String dados = new String(pacote.getData(), 0, pacote.getLength());
                    Mensagem msg = Mensagem.fromJson(new org.json.JSONObject(dados));

                    // 🔹 Ignora a própria sonda para não sobrescrever o status
                    if (msg.getRemetente().equals(nomeUsuario) && msg.getTipo() == TipoMensagem.SONDA) {
                        continue;
                    }

                    long agora = System.currentTimeMillis();
                    User remetente = usuariosConectados.get(msg.getRemetente());

                    if (remetente == null) {
                        // Novo usuário
                        remetente = new User(
                                msg.getRemetente(),
                                msg.getTipo() == TipoMensagem.SONDA ? msg.getConteudo() : "disponivel",
                                agora
                        );
                        usuariosConectados.put(remetente.getNome(), remetente);
                        for (UserListener l : userListeners) l.usuarioAdicionado(remetente);
                    } else {
                        // Atualiza último sinal
                        remetente.setUltimoSinal(agora);

                        // Atualiza status se for sonda
                        if (msg.getTipo() == TipoMensagem.SONDA) {
                            remetente.setStatus(msg.getConteudo());
                            for (UserListener l : userListeners) l.usuarioAlterado(remetente);
                        }
                    }

                    // Se usuário está indisponível, ignora mensagens individuais
                    if ("indisponivel".equalsIgnoreCase(remetente.getStatus()) && msg.getTipo() != TipoMensagem.SONDA) {
                        continue;
                    }

                    // Processa mensagens
                    switch (msg.getTipo()) {
                        case MSG_INDIVIDUAL -> {
                            for (MessageListener l : messageListeners) {
                                l.mensagemRecebida(msg.getConteudo(), remetente, false);
                            }
                        }
                        case MSG_GRUPO -> {
                            for (MessageListener l : messageListeners) {
                                l.mensagemRecebida(msg.getConteudo(), remetente, true);
                            }
                        }
                        case FIM_CHAT -> {
                            for (UserListener l : userListeners) {
                                if (l instanceof ChatP2PUI) {
                                    ((ChatP2PUI) l).fimChatRecebido(remetente);
                                }
                            }
                        }
                        case SONDA -> {
                            // Nada a fazer, já atualizamos o status
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Erro ao receber mensagem: " + e.getMessage());
                }
            }
        }).start();
    }
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

    @Override
    public void enviarMensagem(String mensagem, User destinatario, boolean chatGeral) {
        if (chatGeral) {
            enviarMensagemGrupo(mensagem);
        } else {
            enviarMensagemIndividual(destinatario.getNome(), mensagem);
        }
    }

    public void enviarMensagemIndividual(String destinatario, String conteudo) {
        User usuario = usuariosConectados.get(destinatario);
        if (usuario == null || "indisponivel".equalsIgnoreCase(usuario.getStatus())) return;

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

    @Override
    public void usuarioAlterado(User usuario) {
        for (UserListener l : userListeners) {
            l.usuarioAlterado(usuario);
        }
    }
}
