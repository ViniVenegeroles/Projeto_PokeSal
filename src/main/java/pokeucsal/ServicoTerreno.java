package pokeucsal;

import java.time.Clock;

/** Informa o terreno atual da batalha. */
public interface ServicoTerreno {
    /**
     * Consulta o terreno.
     *
     * @param relogio relógio usado para saber se é dia ou noite
     * @return o nome do terreno
     */
    String obterTerrenoAtual(Clock relogio);
}
