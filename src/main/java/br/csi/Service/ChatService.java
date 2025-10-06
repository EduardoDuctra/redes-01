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
    private String status = "disponivel"; //todos Usuários começam com o status Disponível
    private DatagramSocket socket; //envia e recebe pacotes UDP
    private Map<String, User> usuariosConectados; //Mapa de Usuários conectados. Chave é o nome

    //EventListeners
    private List<MessageListener> messageListeners;
    private List<UserListener> userListeners;

    //Construtor. Cada Usuário recebe seu nome
    // um MAP de outros Usuários conectados
    // Array de Usuários e Mensagens Listeners
    //DatagramSocket na porta 8080 - onde o  processo vai rodar
    public ChatService(String nomeUsuario) throws SocketException {
        this.nomeUsuario = nomeUsuario;
        this.usuariosConectados = new HashMap<>();
        this.messageListeners = new CopyOnWriteArrayList<>();
        this.userListeners = new CopyOnWriteArrayList<>();
        this.socket = new DatagramSocket(8080);

        // Se adiciona na lista de Usuários conectados para aparecer aos outros dispositivos conectados na rede
        User self = new User(nomeUsuario, status, System.currentTimeMillis());
        usuariosConectados.put(nomeUsuario, self);
    }

    //Getters e setters

    public String getStatus() {
        return status;
    }

    public void setStatus(String novoStatus) {
        // Atualiza o status do usuário
        this.status = novoStatus;

        //envia sonda para os Usuários informando que o status mudou
        enviarSonda();

        //Mostrar o status atual (novo) aos demais usuários
        User self = usuariosConectados.get(nomeUsuario);
        self.setStatus(status);

        //marca o sinal da última atualização
        self.setUltimoSinal(System.currentTimeMillis());

        //notifica aos demais Usuários, pelo Listener, que o status mudou
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


    // Listeners de Mensagem e Usuário
    @Override
    public void addListenerMensagem(MessageListener listener) {
        messageListeners.add(listener);
    }

    @Override
    public void addListenerUsuario(UserListener listener) {
        userListeners.add(listener);
    }


    public void iniciarRede() {

        //Envia sondas para descobrir outros usuários
        iniciarSondas();
        //Lista com monitoriamento de usuários ativos. Verifica a cada 10s
        monitorarUsuariosAtivos();
        //remove os usuários inativos. Mais de 30s inativos (aba fechada)
        removerUsuariosInativos();
        //recebe mensagem de usuários
        receberMensagensOutrosUsuarios();
    }

    private void iniciarSondas() {
        //timer de 5s criando para uma tarefa em ciclo (enviarSonda())
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                enviarSonda(); }
        }, 0, 5000);
    }

    private void enviarSonda() {
        try {
            //cria uma mensagem tipo SONDA. Envia o nome do usuário e o status para os dispositivos da rede
            Mensagem sonda = Mensagem.criarSonda(nomeUsuario, status);
            //broadcast = envia para todos os dispositivos da rede a mensagem de sonda
            enviarMensagemBroadcast(sonda.toJson().toString());
        } catch (Exception e) {
            System.out.println("Erro ao enviar sonda: " + e.getMessage());
        }
    }

    private void monitorarUsuariosAtivos() {
        //timer de 10s criando para uma tarefa em ciclo (imprimir os usuários ativos no terminal)
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("=== USUÁRIOS ATIVOS (últimos 10s) ===");
                if (usuariosConectados.isEmpty()) {
                    System.out.println("Nenhum usuário ativo");
                } else {
                    long agora = System.currentTimeMillis();
                    for (User usuario : usuariosConectados.values()) {
                        System.out.println(usuario.getNome() + " - status: " + usuario.getStatus());
                    }
                }
            }
        }, 0, 10000);
    }


    private void removerUsuariosInativos() {
        //timer de 5s criando para uma tarefa em ciclo (verifica se tem algum usuario inativo)
        //Usuários com mais de 30s inativos são removidos
        // inativo = fechar a janela do programa
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                long agora = System.currentTimeMillis();
                for (User usuario : new ArrayList<>(usuariosConectados.values())) {
                    //ignora o próprio usuário
                    if (!usuario.getNome().equals(nomeUsuario) && agora - usuario.getUltimoSinal() > 30000) {
                        usuariosConectados.remove(usuario.getNome());
                        System.out.println("Usuário removido por inatividade: " + usuario.getNome());

                        //avisa o Listener para remover o usuário
                        for (UserListener l : userListeners) {
                            l.usuarioRemovido(usuario);
                        }
                    }
                }
            }
        }, 0, 5000);
    }

    private void receberMensagensOutrosUsuarios() {

        //uso de Thread para receber mensagens
        new Thread(() -> {
            //tamanho do buffer de dados (armazena o pacote UDP recebido)
            byte[] buffer = new byte[1024];
            while (true) {
                try {

                    //Crio um pacote UDP vazio com 1024 bytes (tamanho do buffer)
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                    //Quando recebe uma mensagem de outro usuário da rede armazena os dados no buffer
                    //Aqui são armazenados o IP da máquina e a porta (processo)
                    socket.receive(pacote);

                    //Converte os dados do DatagramPacket e converte em String
                    String dados = new String(pacote.getData(), 0, pacote.getLength());
                    //Constroi o JSON recebido e depois converte em objeto
                    Mensagem msg = Mensagem.fromJson(new org.json.JSONObject(dados));

                    // ignora a própria sonda
                    if (msg.getRemetente().equals(nomeUsuario) && msg.getTipo() == TipoMensagem.SONDA) {
                        continue;
                    }

                    // ignora as próprias mensagens
                    if (msg.getRemetente().equals(nomeUsuario) && msg.getTipo() != TipoMensagem.SONDA) {
                        continue;
                    }

                    //guarda o horario atual da mensagem. Controla a atividade do usuário
                    long agora = System.currentTimeMillis();
                    //busca em usuariosConectados quem mandou a mensagem
                    User remetente = usuariosConectados.get(msg.getRemetente());

                    if (remetente == null) {
                        // Verifica se o usuário está na lista de usuários conectados. Se não estiver:

                        // Define o status do usuário
                        String status;
                        if (msg.getTipo() == TipoMensagem.SONDA) {
                            // se for mensagem sonda, pega o conteúdo da mensagem
                            status = msg.getConteudo();
                        }
                        else {
                            // caso contrário, define o status como "disponivel"
                            status = "disponivel";
                        }

                        // Cria o novo usuário
                        remetente = new User(
                                msg.getRemetente(), // nome usuário
                                status,             // status
                                agora               // horario para controlar a atividade do usuário
                        );

                        // Adiciona o novo usuário ao mapa de usuários conectados
                        usuariosConectados.put(remetente.getNome(), remetente);

                        // Avisa os listeners que um novo usuário foi adicionado
                        for (UserListener l : userListeners) {
                            l.usuarioAdicionado(remetente);
                        }
                    }
                    else {
                        // Atualiza o horário do último sinal recebido do usuário, marcando que ele ainda está ativo
                        remetente.setUltimoSinal(agora);

                        // Atualiza status se for SONDA
                        if (msg.getTipo() == TipoMensagem.SONDA) {
                            //atualiza o status do usuário
                            remetente.setStatus(msg.getConteudo());
                            //avisa os Listeners que aquele usuário foi alterado
                            for (UserListener l : userListeners) l.usuarioAlterado(remetente);
                        }
                    }

                    // Se usuário está indisponível, ignora mensagens individuais
                    if ("indisponivel".equalsIgnoreCase(remetente.getStatus()) && msg.getTipo() != TipoMensagem.SONDA) {
                        continue;
                    }

                    // Tipos de mensagem/Processamento
                    switch (msg.getTipo()) {

                        case MSG_INDIVIDUAL -> {
                            //envia ao Listener o conteudo da mensagem recebida (individual)
                            for (MessageListener l : messageListeners) {
                                l.mensagemRecebida(msg.getConteudo(), remetente, false);
                            }
                        }
                        case MSG_GRUPO -> {
                            //envia ao Listener o conteudo da mensagem recebida (grupo)
                            for (MessageListener l : messageListeners) {
                                l.mensagemRecebida(msg.getConteudo(), remetente, true);
                            }
                        }
                        case FIM_CHAT -> {
                            //envia ao Listener que é mensagem de tipo fim_chat
                            for (UserListener l : userListeners) {
                                //verificar se o Listener implementa a interface ChatP2PUI.
                                //Se for, envia a mensagem de fim de chat
                                if (l instanceof ChatP2PUI) {
                                    ((ChatP2PUI) l).fimChatRecebido(remetente);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Erro ao receber mensagem: " + e.getMessage());
                }
            }
        }).start();
    }






    //-----------comentei até aqui -----------------
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
