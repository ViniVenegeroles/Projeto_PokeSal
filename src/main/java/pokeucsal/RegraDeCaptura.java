package pokeucsal;

/** Decide se uma tentativa de captura dá certo. Fica atrás de uma interface para os testes escolherem o resultado. */
public interface RegraDeCaptura {
    boolean capturaTemSucesso(Pokemon alvo);
}
