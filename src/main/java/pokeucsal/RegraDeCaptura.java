package pokeucsal;

/** Decide se uma tentativa de captura dá certo. */
public interface RegraDeCaptura {
    /**
     * Decide o resultado da tentativa.
     *
     * @param alvo Pokésal que se tenta capturar
     * @return true se a captura deu certo
     */
    boolean capturaTemSucesso(Pokesal alvo);
}
