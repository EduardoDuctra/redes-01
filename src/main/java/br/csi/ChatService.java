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
        new Thread(this::receberMensagens).start();
        iniciarSondas();
        iniciarLimpezaInativos();
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void addListener(ChatListener listener) {
        listeners.add(listener);
    }

    private void iniciarSondas() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                enviarSonda();
            }
        }, 0, 5000);
    }

    private void iniciarLimpezaInativos() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                long agora = System.currentTimeMillis();
                List<String> remover = new ArrayList<>();
                for (UsuarioOnline u : usuariosConectados.values()) {
                    if (agora - u.getUltimoSinal() > 30000) remover.add(u.getUsuario());
                }
                for (String u : remover) {
                    usuariosConectados.remove(u);
                    for (ChatListener l : listeners) l.usuarioRemovido(u);
                }
            }
        }, 0, 10000);
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

    private void receberMensagens() {
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
                    u = new UsuarioOnline(remetente, json.optString("status", "disponivel"), agora);
                    usuariosConectados.put(remetente, u);
                    for (ChatListener l : listeners) l.usuarioAdicionado(u);
                } else {
                    u.setUltimoSinal(agora);
                    u.setStatus(json.optString("status", u.getStatus()));
                }

                switch(tipo) {
                    case "msg_individual":
                        for (ChatListener l : listeners) l.mensagemIndividualRecebida(remetente, json.getString("msg"));
                        break;
                    case "msg_grupo":
                        for (ChatListener l : listeners) l.mensagemGrupoRecebida(remetente, json.getString("msg"));
                        break;
                    case "fim_chat":
                        for (ChatListener l : listeners) l.fimChatRecebido(remetente);
                        break;
                }

            } catch (Exception e) {
                System.out.println("Erro ao receber mensagem: " + e.getMessage());
            }
        }
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
