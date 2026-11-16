package pokeucsal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoTerrenoClimaTest {
    private static final Clock DIA = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneOffset.UTC);
    private static final Clock NOITE = Clock.fixed(Instant.parse("2026-11-13T23:00:00Z"), ZoneOffset.UTC);

    @Mock ClienteClima cliente;
    @Mock RepositorioResultados repositorio;
    @InjectMocks ServicoTerrenoClima servico;

    @Test
    void testSolDeDiaViraAsfaltoQuente() throws Exception {
        when(cliente.obterObservacaoAtual()).thenReturn(new ObservacaoClima(0, 5, 0.0));
        assertEquals(Terrenos.ASFALTO_QUENTE, servico.obterTerrenoAtual(DIA));
    }

    @Test
    void testCeuLimpoDeNoiteViraNeutro() throws Exception {
        when(cliente.obterObservacaoAtual()).thenReturn(new ObservacaoClima(0, 5, 0.0));
        assertEquals(Terrenos.NEUTRO, servico.obterTerrenoAtual(NOITE));
    }

    @Test
    void testNubladoViraCanteiroCentral() throws Exception {
        when(cliente.obterObservacaoAtual()).thenReturn(
                new ObservacaoClima(3, 100, 0.0),
                new ObservacaoClima(2, ServicoTerrenoClima.LIMITE_NUVENS_NUBLADO, 0.0));

        assertEquals(Terrenos.CANTEIRO_CENTRAL, servico.obterTerrenoAtual(DIA));
        Clock depoDoCache = Clock.fixed(DIA.instant().plusSeconds(601), ZoneOffset.UTC);
        assertEquals(Terrenos.CANTEIRO_CENTRAL, servico.obterTerrenoAtual(depoDoCache));
    }

    @Test
    void testChuvaViraPisoEscorregadioEGanhaDoNublado() throws Exception {
        when(cliente.obterObservacaoAtual()).thenReturn(
                new ObservacaoClima(95, 90, 0.0),
                new ObservacaoClima(3, 90, 0.3));

        assertEquals(Terrenos.PISO_ESCORREGADIO, servico.obterTerrenoAtual(NOITE));
        Clock depoDoCache = Clock.fixed(NOITE.instant().plusSeconds(601), ZoneOffset.UTC);
        assertEquals(Terrenos.PISO_ESCORREGADIO, servico.obterTerrenoAtual(depoDoCache));
    }

    @Test
    void testConsultasSeguidasUsamCacheEExpiramEmDezMinutos() throws Exception {
        when(cliente.obterObservacaoAtual()).thenReturn(new ObservacaoClima(0, 0, 0.0));

        servico.obterTerrenoAtual(DIA);
        servico.obterTerrenoAtual(DIA);
        verify(cliente, times(1)).obterObservacaoAtual();

        servico.obterTerrenoAtual(Clock.fixed(DIA.instant().plusSeconds(601), ZoneOffset.UTC));
        verify(cliente, times(2)).obterObservacaoAtual();
    }

    @Test
    void testFalhaDoClienteLancaExcecaoEFicaGuardadaPorUmMinuto() throws Exception {
        when(cliente.obterObservacaoAtual()).thenThrow(new IOException("sem internet"));

        assertThrows(IllegalStateException.class, () -> servico.obterTerrenoAtual(DIA));
        assertThrows(IllegalStateException.class, () -> servico.obterTerrenoAtual(DIA));
        verify(cliente, times(1)).obterObservacaoAtual();

        Clock depoDaFalha = Clock.fixed(DIA.instant().plusSeconds(61), ZoneOffset.UTC);
        assertThrows(IllegalStateException.class, () -> servico.obterTerrenoAtual(depoDaFalha));
        verify(cliente, times(2)).obterObservacaoAtual();
    }

    @Test
    void testFalhaNaoDerrubaABatalhaQueCaiNoTerrenoNeutro() throws Exception {
        Batalha.modoTeste = true;
        try {
            when(cliente.obterObservacaoAtual()).thenThrow(new IOException("sem internet"));
            Batalha batalha = new Batalha(servico, DIA, repositorio, null, null);
            assertEquals(Terrenos.NEUTRO, batalha.getClimaAtual());

            verify(cliente, atLeastOnce()).obterObservacaoAtual();
        } finally {
            Batalha.modoTeste = false;
        }
    }

    @Test
    void testInterrupcaoDaConsultaRestauraOFlagDaThread() throws Exception {
        when(cliente.obterObservacaoAtual()).thenThrow(new InterruptedException("cancelado"));
        try {
            assertThrows(IllegalStateException.class, () -> servico.obterTerrenoAtual(DIA));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
