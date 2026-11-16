package pokeucsal;

/** Captura com risco: dá certo em 70% das tentativas, sorteadas pelo gerador recebido. */
public class RegraDeCapturaPadrao implements RegraDeCaptura {
    public static final double CHANCE_CAPTURA = 0.70;

    private final GeradorAleatorio gerador;

    /**
     * Cria a regra com o gerador de sorteio.
     *
     * @param gerador fonte do sorteio
     */
    public RegraDeCapturaPadrao(GeradorAleatorio gerador) {
        this.gerador = gerador;
    }

    @Override
    public boolean capturaTemSucesso(Pokesal alvo) {
        return gerador.sortearChance() < CHANCE_CAPTURA;
    }
}
