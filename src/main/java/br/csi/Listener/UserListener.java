package br.csi.Listener;

import br.csi.Model.User;

public interface UserListener {

    void usuarioAdicionado(User usuario);

    void usuarioRemovido(User usuario);

    void usuarioAlterado(User usuario);

}
