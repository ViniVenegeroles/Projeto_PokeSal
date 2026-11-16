package pokeucsal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Terreno decidido pelo clima de Salvador: chuva vira Piso Escorregadio, nublado vira Canteiro Central e sol vira Asfalto Quente (de noite, céu limpo vira Terreno Neutro). A consulta fica em cache por 10 minutos e a falha por 1 minuto.
 */
public class ServicoTerrenoClima implements ServicoTerreno {
    static final Duration VALIDADE_CACHE = Duration.ofMinutes(10);

    static final Duration VALIDADE_FALHA = Duration.ofMinutes(1);
    static final int LIMITE_NUVENS_NUBLADO = 70;

    private final ClienteClima cliente;
    private ObservacaoClima ultima;
    private Instant ultimaConsulta;
    private IllegalStateException ultimaFalha;
    private Instant instanteDaFalha;

    /**
     * Cria o serviço com a fonte de clima.
     *
     * @param cliente fonte externa de clima
     */
    public ServicoTerrenoClima(ClienteClima cliente) {
        this.cliente = cliente;
    }

    @Override
    public synchronized String obterTerrenoAtual(Clock relogio) {
        return terrenoPara(observar(relogio), Terrenos.ehDia(relogio));
    }

    private ObservacaoClima observar(Clock relogio) {
        Instant agora = relogio.instant();
        if (ultima != null && Duration.between(ultimaConsulta, agora).compareTo(VALIDADE_CACHE) < 0) {
            return ultima;
        }
        if (ultimaFalha != null && Duration.between(instanteDaFalha, agora).compareTo(VALIDADE_FALHA) < 0) {
            throw new IllegalStateException(ultimaFalha.getMessage(), ultimaFalha);
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

    static String terrenoPara(ObservacaoClima o, boolean dia) {
        if (chovendo(o)) {
            return Terrenos.PISO_ESCORREGADIO;
        }
        if (nublado(o)) {
            return Terrenos.CANTEIRO_CENTRAL;
        }
        return dia ? Terrenos.ASFALTO_QUENTE : Terrenos.NEUTRO;
    }

    private static boolean chovendo(ObservacaoClima o) {
        int c = o.codigoWmo();
        boolean codigoDeChuva = (c >= 51 && c <= 67)
                || (c >= 80 && c <= 82)
                || (c >= 95 && c <= 99);
        return codigoDeChuva || o.precipitacaoMm() > 0;
    }

    private static boolean nublado(ObservacaoClima o) {
        int c = o.codigoWmo();
        return c == 3 || c == 45 || c == 48 || o.coberturaNuvens() >= LIMITE_NUVENS_NUBLADO;
    }
}
