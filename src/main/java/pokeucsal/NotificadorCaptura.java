package pokeucsal;

/** Avisa o jogador que um Pokésal foi capturado (colaborador externo; uma falha aqui não desfaz a captura). */
public interface NotificadorCaptura {
    void notificarCaptura(String treinador, String pokemonCapturado) throws Exception;
}
