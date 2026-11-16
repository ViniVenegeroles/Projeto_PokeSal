package pokeucsal;

/**
 * Captura com risco: dá certo em 70% das tentativas. O sorteio vem do GeradorAleatorio injetado
 * (proibido usar Random aqui, regra 2.1), então nos testes o resultado é escolhido pelo mock.
 */
public class RegraDeCapturaPadrao implements RegraDeCaptura {
    /** Troque aqui para deixar a captura mais fácil (valor maior) ou mais difícil (valor menor). */
    public static final double CHANCE_CAPTURA = 0.70;

    private final GeradorAleatorio gerador;

    public RegraDeCapturaPadrao(GeradorAleatorio gerador) { this.gerador = gerador; }

    @Override
    public boolean capturaTemSucesso(Pokemon alvo) {
        return gerador.sortearChance() < CHANCE_CAPTURA;
    }
}
