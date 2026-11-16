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
    // Relógios FIXOS (reais) em vez de mock de Clock: LocalTime.now(clock) precisa de instant() e getZone() de verdade.
    private static final Clock DIA = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneOffset.UTC);
    private static final Clock NOITE = Clock.fixed(Instant.parse("2026-11-10T23:00:00Z"), ZoneOffset.UTC);

    @Mock GeradorAleatorio gerador;
    @Mock ServicoTerreno servicoTerreno;
    @Mock ClienteClima clienteClima; // o clima real de Salvador: mockado para o teste não depender de internet
    @Mock RepositorioResultados repositorio;
    @Mock RegistroBatalha registro;

    @BeforeEach
    void setUp() { Batalha.modoTeste = true; }

    @AfterEach
    void tearDown() { Batalha.modoTeste = false; }

    @Test
    void testAsfaltoQuenteApenasDuranteODia() throws Exception {
        // Céu limpo o tempo todo: o que muda entre os dois instantes é só o horário (o Clock injetado).
        when(clienteClima.obterObservacaoAtual()).thenReturn(new ObservacaoClima(0, 5, 0.0));
        ServicoTerreno servico = new ServicoTerrenoClima(clienteClima);

        assertEquals(Terrenos.ASFALTO_QUENTE, servico.obterTerrenoAtual(DIA));
        // De noite o Asfalto Quente não existe (regra 2.2): o terreno cai no neutro, nunca em Asfalto.
        assertEquals(Terrenos.NEUTRO, servico.obterTerrenoAtual(NOITE));
        // DIA e NOITE ficam a 8h um do outro, então o cache de 10 min expirou e o cliente foi consultado 2 vezes.
        verify(clienteClima, times(2)).obterObservacaoAtual();
    }

    @Test
    void testBatalhaUsaTerrenoDoServicoEProtegeContraNull() {
        when(servicoTerreno.obterTerrenoAtual(DIA)).thenReturn(Terrenos.ASFALTO_QUENTE);
        Batalha comTerreno = new Batalha(servicoTerreno, DIA, repositorio, registro, gerador);
        assertEquals(Terrenos.ASFALTO_QUENTE, comTerreno.getClimaAtual());

        // Serviço devolvendo null (ou falhando) não pode causar NullPointerException depois.
        when(servicoTerreno.obterTerrenoAtual(NOITE)).thenReturn(null);
        Batalha semTerreno = new Batalha(servicoTerreno, NOITE, repositorio, registro, gerador);
        assertEquals(Terrenos.NEUTRO, semTerreno.getClimaAtual());
    }

    @Test
    void testSalShardMudaOTerrenoENuncaCriaAsfaltoDeNoite() {
        when(servicoTerreno.obterTerrenoAtual(NOITE)).thenReturn(Terrenos.PISO_ESCORREGADIO);
        when(gerador.sortearChance()).thenReturn(0.0);
        Batalha noite = new Batalha(servicoTerreno, NOITE, repositorio, registro, gerador);

        noite.mudarTerrenoSalShard(registro, gerador);

        // Tinha que MUDAR (era Piso) e, de noite, não pode virar Asfalto Quente.
        assertEquals(Terrenos.CANTEIRO_CENTRAL, noite.getClimaAtual());
        verify(registro).registrarEfeitoTerreno("Campo", "Sal Shard ativou: " + Terrenos.CANTEIRO_CENTRAL);
    }
}
