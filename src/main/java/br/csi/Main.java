package br.csi;

import javax.swing.*;
import java.net.*;
import java.io.*;

import br.csi.ChatP2P;
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java Main <nome_usuario>");
            return;
        }

        String username = args[0];

        SwingUtilities.invokeLater(() -> {
            ChatP2P chat = new ChatP2P(username);
            chat.setVisible(true);
        });
    }
}