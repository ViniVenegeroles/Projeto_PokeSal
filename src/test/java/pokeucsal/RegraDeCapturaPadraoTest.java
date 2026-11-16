package pokeucsal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A regra de captura só decide pelo sorteio recebido: com gerador de valor fixo o resultado é previsível. */
class RegraDeCapturaPadraoTest {
    private static final Pokemon ALVO = new Pokemon("BulbaSal", new TipoPlanta(), 50, 40, 150, 80, new Golpe[]{});

    private static boolean tentar(double chanceSorteada) {
        return new RegraDeCapturaPadrao(new GeradorFixo(0, chanceSorteada)).capturaTemSucesso(ALVO);
    }

    @Test
    void testSorteioBaixoCapturaESorteioAltoNao() {
        assertTrue(tentar(0.0));
        assertTrue(tentar(0.5));
        assertFalse(tentar(0.99));
    }

    @Test
    void testFronteiraDaChanceDeSetentaPorCento() {
        assertTrue(tentar(RegraDeCapturaPadrao.CHANCE_CAPTURA - 0.01));   // logo abaixo: captura
        assertFalse(tentar(RegraDeCapturaPadrao.CHANCE_CAPTURA));         // exatamente no valor: não captura (< estrito)
    }

    @Test
    void testChanceDeCapturaEhSetentaPorCento() {
        assertEquals(0.70, RegraDeCapturaPadrao.CHANCE_CAPTURA);
    }
}
