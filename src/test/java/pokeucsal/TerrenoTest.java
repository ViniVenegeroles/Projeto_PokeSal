package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerrenoTest {
    private static final Clock DIA = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneOffset.UTC);
    private static final Clock NOITE = Clock.fixed(Instant.parse("2026-11-10T23:00:00Z"), ZoneOffset.UTC);

    @Mock GeradorAleatorio gerador;
    @Mock ServicoTerreno servicoTerreno;
    @Mock ClienteClima clienteClima;
    @Mock RepositorioResultados repositorio;
    @Mock RegistroBatalha registro;

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
    }

    @AfterEach
    void tearDown() {
        Batalha.modoTeste = false;
    }

    @Test
    void testAsfaltoQuenteApenasDuranteODia() throws Exception {
        when(clienteClima.obterObservacaoAtual()).thenReturn(new ObservacaoClima(0, 5, 0.0));
        ServicoTerreno servico = new ServicoTerrenoClima(clienteClima);

        assertEquals(Terrenos.ASFALTO_QUENTE, servico.obterTerrenoAtual(DIA));

        assertEquals(Terrenos.NEUTRO, servico.obterTerrenoAtual(NOITE));

        verify(clienteClima, times(2)).obterObservacaoAtual();
    }

    @Test
    void testBatalhaUsaTerrenoDoServicoEProtegeContraNull() {
        when(servicoTerreno.obterTerrenoAtual(DIA)).thenReturn(Terrenos.ASFALTO_QUENTE);
        Batalha comTerreno = new Batalha(servicoTerreno, DIA, repositorio, registro, gerador);
        assertEquals(Terrenos.ASFALTO_QUENTE, comTerreno.getClimaAtual());

        when(servicoTerreno.obterTerrenoAtual(NOITE)).thenReturn(null);
        Batalha semTerreno = new Batalha(servicoTerreno, NOITE, repositorio, registro, gerador);
        assertEquals(Terrenos.NEUTRO, semTerreno.getClimaAtual());
    }

    @Test
    void testSalShardSempreMudaOTerrenoMesmoDeNoite() {
        when(servicoTerreno.obterTerrenoAtual(NOITE)).thenReturn(Terrenos.PISO_ESCORREGADIO);
        when(gerador.sortearChance()).thenReturn(0.0);
        Batalha noite = new Batalha(servicoTerreno, NOITE, repositorio, registro, gerador);

        noite.mudarTerrenoSalShard(registro, gerador);

        assertEquals(Terrenos.ASFALTO_QUENTE, noite.getClimaAtual());
        verify(registro).registrarEfeitoTerreno("Campo", "Sal Shard ativou: " + Terrenos.ASFALTO_QUENTE);
    }
}
