package br.csi;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.List;


import org.json.JSONObject;

public class ChatP2P extends JFrame {
    private String username;
    private String status = "disponivel";
    private DatagramSocket socket;
    private Map<String, UserSession> userSessions;
    private DefaultListModel<String> onlineUsersModel;
    private JList<String> onlineUsersList;
    private GroupChatWindow groupChat;

    // Timer para sondas
    private Timer probeTimer;
    private Map<String, Long> lastProbeTime;

    public ChatP2P(String username) {
        this.username = username;
        this.userSessions = new HashMap<>();
        this.onlineUsersModel = new DefaultListModel<>();
        this.lastProbeTime = new HashMap<>();

        initializeUI();
        initializeNetwork();
        startProbeTimer();
        startCleanupTimer();
    }

    private void initializeUI() {
        setTitle("Chat P2P - " + username);
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Lista de usuários online
        onlineUsersList = new JList<>(onlineUsersModel);
        onlineUsersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        onlineUsersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = onlineUsersList.getSelectedValue();
                if (selectedUser != null) {
                    openChatWindow(selectedUser);
                }
            }
        });

        // Botão para chat em grupo
        JButton groupChatButton = new JButton("Chat em Grupo");
        groupChatButton.addActionListener(e -> openGroupChat());

        // Layout
        setLayout(new BorderLayout());
        add(new JScrollPane(onlineUsersList), BorderLayout.CENTER);
        add(groupChatButton, BorderLayout.SOUTH);
    }

    private void initializeNetwork() {
        try {
            // SEMPRE usa porta 8080 conforme edital
            socket = new DatagramSocket(8080);
            new Thread(this::receiveMessages).start();
            System.out.println("Usuário " + username + " ouvindo na porta 8080");

        } catch (BindException e) {
            JOptionPane.showMessageDialog(this, "Erro: Porta 8080 já em uso. Execute em outro computador.");
            System.exit(1);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao criar socket: " + e.getMessage());
        }
    }

    private void startProbeTimer() {
        probeTimer = new Timer(5000, e -> sendProbe());
        probeTimer.start();
    }

    private void startCleanupTimer() {
        Timer cleanupTimer = new Timer(10000, e -> cleanupInactiveUsers());
        cleanupTimer.start();
    }

    private void cleanupInactiveUsers() {
        long currentTime = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Long> entry : lastProbeTime.entrySet()) {
            if (currentTime - entry.getValue() > 30000) { // 30 segundos
                toRemove.add(entry.getKey());
            }
        }

        for (String user : toRemove) {
            lastProbeTime.remove(user);
            onlineUsersModel.removeElement(user);

            UserSession session = userSessions.get(user);
            if (session != null) {
                session.dispose();
                userSessions.remove(user);
            }
        }
    }

    private void sendProbe() {
        try {
            JSONObject probe = new JSONObject();
            probe.put("tipoMensagem", "sonda");
            probe.put("usuario", username);
            probe.put("status", status);

            String message = probe.toString();
            byte[] buffer = message.getBytes();

            // Envia para broadcast na subrede
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    broadcastAddress, 8080);
            socket.send(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        byte[] buffer = new byte[1024];

        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                JSONObject json = new JSONObject(receivedMessage);

                String messageType = json.getString("tipoMensagem");
                String user = json.getString("usuario");

                // Ignora mensagens próprias
                if (user.equals(username)) {
                    continue;
                }

                // Atualiza o tempo da última sonda
                lastProbeTime.put(user, System.currentTimeMillis());

                switch (messageType) {
                    case "sonda":
                        handleProbe(json, packet.getAddress());
                        break;
                    case "msg_individual":
                        handleIndividualMessage(json, packet.getAddress());
                        break;
                    case "msg_grupo":
                        handleGroupMessage(json);
                        break;
                    case "fim_chat":
                        handleEndChat(user);
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleProbe(JSONObject json, InetAddress address) {
        String user = json.getString("usuario");
        String status = json.getString("status");

        SwingUtilities.invokeLater(() -> {
            // Adiciona ou atualiza usuário na lista
            if (!onlineUsersModel.contains(user)) {
                onlineUsersModel.addElement(user);
            }
        });
    }

    private void handleIndividualMessage(JSONObject json, InetAddress address) {
        String user = json.getString("usuario");
        String message = json.getString("msg");

        SwingUtilities.invokeLater(() -> {
            UserSession session = userSessions.get(user);
            if (session == null) {
                session = new UserSession(user, this);
                userSessions.put(user, session);
            }
            session.addMessage(user + ": " + message);
            session.setVisible(true);
        });
    }

    private void handleGroupMessage(JSONObject json) {
        String user = json.getString("usuario");
        String message = json.getString("msg");

        SwingUtilities.invokeLater(() -> {
            if (groupChat == null) {
                groupChat = new GroupChatWindow(this);
            }
            groupChat.addMessage(user + ": " + message);
            groupChat.setVisible(true);
        });
    }

    private void handleEndChat(String user) {
        SwingUtilities.invokeLater(() -> {
            onlineUsersModel.removeElement(user); // remove da lista imediatamente
            UserSession session = userSessions.get(user);
            if (session != null) {
                session.dispose();
                userSessions.remove(user);
            }
        });
    }

    public void openChatWindow(String user) {
        UserSession session = userSessions.get(user);
        if (session == null) {
            session = new UserSession(user, this);
            userSessions.put(user, session);
        }
        session.setVisible(true);
    }

    public void openGroupChat() {
        if (groupChat == null) {
            groupChat = new GroupChatWindow(this);
        }
        groupChat.setVisible(true);
    }

    public void sendIndividualMessage(String user, String message) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipoMensagem", "msg_individual");
            json.put("usuario", username);
            json.put("status", status);
            json.put("msg", message);

            sendMessageToUser(user, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendGroupMessage(String message) {
        try {
            JSONObject json = new JSONObject();
            json.put("tipoMensagem", "msg_grupo");
            json.put("usuario", username);
            json.put("status", status);
            json.put("msg", message);

            sendBroadcast(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendEndChat(String user) {
        try {
            System.out.println("🔄 ENVIANDO fim_chat para: " + user);
            JSONObject json = new JSONObject();
            json.put("tipoMensagem", "fim_chat");
            json.put("usuario", username);
            String message = json.toString();
            System.out.println("📦 Mensagem: " + message);
            sendMessageToUser(user, message);
        } catch (Exception e) {
            System.out.println("❌ Erro ao enviar fim_chat: " + e.getMessage());
        }
    }

    private void sendMessageToUser(String user, String message) {
        try {
            System.out.println("📤 Enviando mensagem para broadcast...");
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    address, 8080);
            socket.send(packet);
            System.out.println("✅ Mensagem enviada 1/3");

            Thread.sleep(100);
            socket.send(packet);
            System.out.println("✅ Mensagem enviada 2/3");

            Thread.sleep(100);
            socket.send(packet);
            System.out.println("✅ Mensagem enviada 3/3");

        } catch (Exception e) {
            System.out.println("❌ Erro no envio: " + e.getMessage());
        }
    }

    private void sendBroadcast(String message) {
        try {
            byte[] buffer = message.getBytes();
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

            // Envia apenas para a porta 8080
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                    broadcastAddress, 8080);
            socket.send(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}