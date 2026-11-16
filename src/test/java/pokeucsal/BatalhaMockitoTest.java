package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatalhaMockitoTest {
    @Mock ServicoTerreno servicoTerreno;
    @Mock Clock clock;
    @Mock RepositorioResultados repositorio;
    @Mock RegistroBatalha registro;
    @Mock GeradorAleatorio gerador;

    @InjectMocks Batalha batalha;

    private Treinador t1;
    private Treinador t2;
    private Golpe[] golpes;
    private Scanner fakeScanner;

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
        golpes = new Golpe[]{ new Golpe.Normal("Tackle", 20) };

        Pokesal p1 = new Pokesal("CharSal", new TipoFogo(), 50, 40, 150, 90, golpes);
        Pokesal p2 = new Pokesal("BulbaSal", new TipoPlanta(), 50, 40, 150, 80, golpes);
        t1 = new Treinador("Jogador 1", p1);
        t2 = new Treinador("Jogador 2", p2);
        fakeScanner = new Scanner("");
    }

    @AfterEach
    void tearDown() {
        Batalha.modoTeste = false;
    }

    private Batalha novaBatalha() {
        return new Batalha(servicoTerreno, clock, repositorio, registro, gerador);
    }

    @Test
    void testAcertoCriticoComGeradorMockado() {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.05);

        batalha.resolverRodadaDeAtaques(t1, golpes[0], t2, golpes[0]);

        assertEquals(96, t2.getPokesal().getHp());
        verify(registro).registrarEfeitoStatus(eq("CharSal"), eq("Acerto Crítico!"));
    }

    @Test
    void testGolpeErraQuandoPrecisaoFalha() {
        when(gerador.sortearPrecisao()).thenReturn(101);

        batalha.resolverRodadaDeAtaques(t1, golpes[0], t2, golpes[0]);

        assertEquals(150, t2.getPokesal().getHp());
        assertEquals(150, t1.getPokesal().getHp());
        verify(registro, never()).registrarDano(anyString(), anyInt());
        verify(gerador, never()).sortearChance();
    }

    @Test
    void testSequenciaDeRetornosDoGerador() {
        when(gerador.sortearPrecisao()).thenReturn(0, 101);
        when(gerador.sortearChance()).thenReturn(0.99);

        batalha.resolverRodadaDeAtaques(t1, golpes[0], t2, golpes[0]);

        assertEquals(118, t2.getPokesal().getHp());
        assertEquals(150, t1.getPokesal().getHp());
        verify(registro, times(2)).registrarAtaque(anyString(), eq("Tackle"));
        verify(registro, atLeastOnce()).registrarDano(eq("BulbaSal"), anyInt());
    }

    @Test
    void testSequenciaDeEventosNoTurno() {
        when(servicoTerreno.obterTerrenoAtual(any(Clock.class))).thenReturn(Terrenos.CANTEIRO_CENTRAL);
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.99);
        Batalha b = novaBatalha();
        t2.getPokesal().setQueimado(true);

        b.resolverRodadaDeAtaques(t1, golpes[0], t2, golpes[0]);

        InOrder ordem = inOrder(registro);
        ordem.verify(registro).registrarAtaque(eq("CharSal"), eq("Tackle"));
        ordem.verify(registro).registrarDano(eq("BulbaSal"), anyInt());
        ordem.verify(registro).registrarAtaque(eq("BulbaSal"), eq("Tackle"));
        ordem.verify(registro).registrarDano(eq("CharSal"), anyInt());
        ordem.verify(registro).registrarEfeitoStatus(eq("BulbaSal"), eq("Dano Queimadura"));
        ordem.verify(registro).registrarEfeitoTerreno(eq("BulbaSal"), eq("Cura Terreno"));
    }

    @Test
    void testTurnoComUsoDeItemAindaProcessaStatusEContaTurno() throws Exception {
        t1.getPokesal().setQueimado(true);

        batalha.resolverRodadaDeAtaques(t1, null, t2, null);

        verify(registro, never()).registrarAtaque(anyString(), anyString());
        verify(registro).registrarEfeitoStatus(eq("CharSal"), eq("Dano Queimadura"));
        verify(repositorio, never()).salvarResultado(any());
    }

    @Test
    void testResultadoSalvoAoFimDaBatalha() throws Exception {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.99);

        Treinador vencedor = batalha.iniciarCombatePvP(t1, t2, fakeScanner);

        ArgumentCaptor<ResultadoBatalha> captor = ArgumentCaptor.forClass(ResultadoBatalha.class);
        verify(repositorio).salvarResultado(captor.capture());
        ResultadoBatalha salvo = captor.getValue();
        assertSame(t1, vencedor);
        assertEquals("Jogador 1", salvo.getVencedor());
        assertEquals("Jogador 2", salvo.getPerdedor());
        assertEquals(5, salvo.getTurnos());
        assertEquals("CharSal", salvo.getPokesalVencedor());
        assertEquals("BulbaSal", salvo.getPokesalPerdedor());
    }

    @Test
    void testFalhaNoRepositorioNaoInvalidaBatalha() throws Exception {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.99);

        doThrow(new Exception("banco fora do ar")).when(repositorio).salvarResultado(any(ResultadoBatalha.class));

        Treinador vencedor = batalha.iniciarCombatePvP(t1, t2, fakeScanner);

        assertSame(t1, vencedor);
        verify(repositorio).salvarResultado(any(ResultadoBatalha.class));
    }

    @Test
    void testRepositorioNaoChamadoSeBatalhaNaoTerminou() throws Exception {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.99);

        batalha.resolverRodadaDeAtaques(t1, golpes[0], t2, golpes[0]);

        assertTrue(t1.getPokesal().getHp() > 0 && t2.getPokesal().getHp() > 0);
        verify(repositorio, never()).salvarResultado(any());
    }

    @Test
    void testCpuEscolheGolpePeloGeradorSemLerTeclado() {
        Golpe outro = new Golpe.Normal("Outro", 10);
        Pokesal pCpu = new Pokesal("TotoSal", new TipoAgua(), 50, 40, 150, 10, new Golpe[]{golpes[0], outro});
        Treinador cpu = new Treinador("Computador", pCpu, true);
        when(gerador.sortearChance()).thenReturn(0.99);
        when(gerador.sortearPrecisao()).thenReturn(0);

        batalha.iniciarCombatePvP(t1, cpu, fakeScanner);

        verify(registro, atLeastOnce()).registrarAtaque(eq("TotoSal"), eq("Outro"));
    }
}
