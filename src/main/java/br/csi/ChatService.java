package br.csi;

import org.json.JSONObject;

import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatService {

    private String nomeUsuario;
    private String status = "disponivel";
    private DatagramSocket socket;
    private Map<String, UsuarioOnline> usuariosConectados;
    private List<ChatListener> listeners;

    public interface ChatListener {
        void usuarioAdicionado(UsuarioOnline usuario);
        void usuarioRemovido(String usuario);
        void mensagemIndividualRecebida(String remetente, String mensagem);
        void mensagemGrupoRecebida(String remetente, String mensagem);
        void fimChatRecebido(String usuario);
    }

    public ChatService(String nomeUsuario) throws SocketException {
        this.nomeUsuario = nomeUsuario;
        this.usuariosConectados = new HashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.socket = new DatagramSocket(8080);
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void addListener(ChatListener listener) {
        listeners.add(listener);
    }

    public Map<String, UsuarioOnline> getUsuariosConectados() {
        return usuariosConectados;
    }

    // Inicia envio de sondas, monitoramento e recepção de mensagens
    public void iniciarRede() {
        iniciarSondas();
        monitorarUsuariosAtivos();
        removerUsuariosInativos();
        receberMensagensAsync();
    }

    // Envio de sondas a cada 5s
    private void iniciarSondas() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() { enviarSonda(); }
        }, 0, 5000);
    }

    private void enviarSonda() {
        try {
            JSONObject msg = new JSONObject();
            msg.put("tipoMensagem", "sonda");
            msg.put("usuario", nomeUsuario);
            msg.put("status", status);
            enviarMensagemBroadcast(msg.toString());
        } catch (Exception e) {
            System.out.println("Erro ao enviar sonda: " + e.getMessage());
        }
    }

    // Monitoramento da lista de usuários ativos a cada 10s
    private void monitorarUsuariosAtivos() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("=== USUÁRIOS ATIVOS (últimos 10s) ===");
                if (usuariosConectados.isEmpty()) {
                    System.out.println("Nenhum usuário ativo");
                } else {
                    long agora = System.currentTimeMillis();
                    for (UsuarioOnline u : usuariosConectados.values()) {
                        System.out.println(u.getUsuario() + " - status: " + u.getStatus() +
                                " - últimos sinal: " + (agora - u.getUltimoSinal()) + "ms atrás");
                    }
                }
            }
        }, 0, 10000);
    }

    // Remove usuários inativos (>30s)
    private void removerUsuariosInativos() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                long agora = System.currentTimeMillis();
                for (UsuarioOnline u : new ArrayList<>(usuariosConectados.values())) {
                    if (agora - u.getUltimoSinal() > 30000) {
                        usuariosConectados.remove(u.getUsuario());
                        System.out.println("⚠️ Usuário removido por inatividade: " + u.getUsuario());
                        for (ChatListener l : listeners) l.usuarioRemovido(u.getUsuario());
                    }
                }
            }
        }, 0, 5000);
    }

    // Thread para receber mensagens UDP
    private void receberMensagensAsync() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                    socket.receive(pacote);
                    String mensagem = new String(pacote.getData(), 0, pacote.getLength());
                    JSONObject json = new JSONObject(mensagem);

                    String tipo = json.getString("tipoMensagem");
                    String remetente = json.getString("usuario");
                    if (remetente.equals(nomeUsuario)) continue;

                    long agora = System.currentTimeMillis();
                    UsuarioOnline u = usuariosConectados.get(remetente);
                    if (u == null) {
                        u = new UsuarioOnline(remetente, json.optString("status","disponivel"), agora);
                        usuariosConectados.put(remetente, u);
                        for (ChatListener l : listeners) l.usuarioAdicionado(u);
                    } else {
                        u.setUltimoSinal(agora);
                        u.setStatus(json.optString("status", u.getStatus()));
                    }

                    switch(tipo) {
                        case "msg_individual":
                            for (ChatListener l : listeners)
                                l.mensagemIndividualRecebida(remetente, json.getString("msg"));
                            break;
                        case "msg_grupo":
                            for (ChatListener l : listeners)
                                l.mensagemGrupoRecebida(remetente, json.getString("msg"));
                            break;
                        case "fim_chat":
                            for (ChatListener l : listeners)
                                l.fimChatRecebido(remetente);
                            break;
                    }

                } catch (Exception e) {
                    System.out.println("Erro ao receber mensagem: " + e.getMessage());
                }
            }
        }).start();
    }

    // Envia mensagem UDP para broadcast
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

    // Envio de mensagens individuais
    public void enviarMensagemIndividual(String destinatario, String mensagem) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipoMensagem","msg_individual");
            json.put("usuario",nomeUsuario);
            json.put("status",status);
            json.put("msg",mensagem);
            enviarMensagemBroadcast(json.toString());
        } catch(Exception e) {
            System.out.println("Erro ao enviar msg individual: " + e.getMessage());
        }
    }

    // Envio de mensagens em grupo
    public void enviarMensagemGrupo(String mensagem) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipoMensagem","msg_grupo");
            json.put("usuario",nomeUsuario);
            json.put("status",status);
            json.put("msg",mensagem);
            enviarMensagemBroadcast(json.toString());
        } catch(Exception e) {
            System.out.println("Erro ao enviar msg grupo: " + e.getMessage());
        }
    }

    // Envio de fim de chat
    public void enviarFimChat(String usuario) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipoMensagem","fim_chat");
            json.put("usuario",nomeUsuario);
            enviarMensagemBroadcast(json.toString());
        } catch(Exception e) {
            System.out.println("Erro ao enviar fim chat: " + e.getMessage());
        }
    }
}
