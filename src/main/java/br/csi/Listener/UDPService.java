package br.csi.Listener;

import br.csi.Model.User;

public interface UDPService {

    /**
     * Envia uma mensagem para um destinatário
     * @param mensagem
     * @param destinatario
     * @param chatGeral
     */
    void enviarMensagem(String mensagem, User destinatario, boolean chatGeral);

    /**
     * Notifica que o próprio usuário foi alterado
     * @param usuario
     */
    void usuarioAlterado(User usuario);

    /**
     * Adiciona um listener para indicar o recebimento de mensagens
     * @param listener
     */
    void addListenerMensagem(MessageListener listener);

    /**
     * Adiciona um listener para indicar recebimento e/ou alterações em usuários
     * @param listener
     */
    void addListenerUsuario(UserListener listener);



}
