package pokeucsal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Terreno decidido pelo CLIMA REAL de Salvador (sem sorteio):
 *   chuva   -> Piso Escorregadio
 *   nublado -> Canteiro Central
 *   sol     -> Asfalto Quente   (regra 2.2: só de dia; de noite com céu limpo vira Terreno Neutro)
 *
 * Se o serviço externo falhar, lança IllegalStateException. A Batalha já trata isso usando o
 * Terreno Neutro, então uma queda da internet nunca derruba o jogo nem volta a sortear terreno.
 */
public class ServicoTerrenoClima implements ServicoTerreno {
    static final Duration VALIDADE_CACHE = Duration.ofMinutes(10);
    // Falha também fica guardada por um tempo curto: sem internet, cada luta esperaria o tempo limite da API de novo.
    static final Duration VALIDADE_FALHA = Duration.ofMinutes(1);
    static final int LIMITE_NUVENS_NUBLADO = 70; // % de cobertura a partir da qual consideramos "nublado"

    private final ClienteClima cliente;
    private ObservacaoClima ultima;
    private Instant ultimaConsulta;
    private IllegalStateException ultimaFalha;
    private Instant instanteDaFalha;

    public ServicoTerrenoClima(ClienteClima cliente) { this.cliente = cliente; }

    @Override
    public synchronized String obterTerrenoAtual(Clock relogio) {
        return terrenoPara(observar(relogio), Terrenos.ehDia(relogio));
    }

    /**
     * Cache de 10 minutos: a Batalha consulta o terreno duas vezes por luta (no construtor e ao preparar),
     * e o torneio tem várias lutas. O clima não muda nesse intervalo, então não vale chamar a API toda vez.
     */
    private ObservacaoClima observar(Clock relogio) {
        Instant agora = relogio.instant();
        if (ultima != null && Duration.between(ultimaConsulta, agora).compareTo(VALIDADE_CACHE) < 0) {
            return ultima;
        }
        if (ultimaFalha != null && Duration.between(instanteDaFalha, agora).compareTo(VALIDADE_FALHA) < 0) {
            throw new IllegalStateException(ultimaFalha.getMessage(), ultimaFalha); // não repete a chamada que acabou de falhar
        }
        try {
            ultima = cliente.obterObservacaoAtual();
            ultimaConsulta = agora;
            ultimaFalha = null;
            return ultima;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta de clima interrompida.", e);
        } catch (Exception e) {
            ultimaFalha = new IllegalStateException("Não foi possível consultar o clima de Salvador: " + e.getMessage(), e);
            instanteDaFalha = agora;
            throw ultimaFalha;
        }
    }

    /** Regra pura (sem internet, sem relógio): fácil de testar com qualquer combinação. Chuva tem prioridade. */
    static String terrenoPara(ObservacaoClima o, boolean dia) {
        if (chovendo(o)) return Terrenos.PISO_ESCORREGADIO;
        if (nublado(o)) return Terrenos.CANTEIRO_CENTRAL;
        return dia ? Terrenos.ASFALTO_QUENTE : Terrenos.NEUTRO;
    }

    private static boolean chovendo(ObservacaoClima o) {
        int c = o.codigoWmo();
        boolean codigoDeChuva = (c >= 51 && c <= 67)    // garoa e chuva (inclui chuva congelante)
                || (c >= 80 && c <= 82)                  // pancadas de chuva
                || (c >= 95 && c <= 99);                 // trovoada
        return codigoDeChuva || o.precipitacaoMm() > 0;
    }

    private static boolean nublado(ObservacaoClima o) {
        int c = o.codigoWmo();
        return c == 3 || c == 45 || c == 48 || o.coberturaNuvens() >= LIMITE_NUVENS_NUBLADO; // 3 = encoberto, 45/48 = neblina
    }
}
