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

/**
 * Testes da Batalha isolada de TODOS os colaboradores (Mockito).
 *
 * Quando usar matcher x valor exato:
 *  - valor EXATO (eq / literal) quando o valor faz parte do que está sendo testado (nome do atacante, nome do golpe);
 *  - MATCHER (anyInt, any) quando o valor é irrelevante para aquele teste (ex.: quanto de dano, que já é
 *    validado em outro teste). Regra do Mockito: se UM argumento usa matcher, TODOS da chamada precisam usar.
 */
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
        Batalha.modoTeste = true; // sem sleep e sem System.out
        golpes = new Golpe[]{ new Golpe.Normal("Tackle", 20) };
        // Atk 50, Def 40 => golpe Normal de poder 20 causa 20 + 25 = 45 de dano bruto, menos 13 de mitigação (40/3) = 32.
        Pokemon p1 = new Pokemon("CharSal", new TipoFogo(), 50, 40, 150, 90, golpes);
        Pokemon p2 = new Pokemon("BulbaSal", new TipoPlanta(), 50, 40, 150, 80, golpes);
        t1 = new Treinador("Jogador 1", p1);
        t2 = new Treinador("Jogador 2", p2);
        fakeScanner = new Scanner("");
    }

    @AfterEach
    void tearDown() { Batalha.modoTeste = false; }

    /** Batalha criada DEPOIS dos stubs (o @InjectMocks já foi montado antes de qualquer when()). */
    private Batalha novaBatalha() {
        return new Batalha(servicoTerreno, clock, repositorio, registro, gerador);
    }

    @Test
    void testAcertoCriticoComGeradorMockado() {
        when(gerador.sortearPrecisao()).thenReturn(0);   // 0 < 100 => acerta
        when(gerador.sortearChance()).thenReturn(0.05);  // 0.05 <= 0.0625 => dentro da faixa de crítico

        batalha.resolverRodadaDeAtaques(t1, golpes[0], t2, golpes[0]);

        // 45 * 1.5 = 67 (truncado) - 13 de mitigação = 54  =>  150 - 54 = 96 (sem crítico seria 118)
        assertEquals(96, t2.getPokemon().getHp());
        verify(registro).registrarEfeitoStatus(eq("CharSal"), eq("Acerto Crítico!"));
    }

    @Test
    void testGolpeErraQuandoPrecisaoFalha() {
        when(gerador.sortearPrecisao()).thenReturn(101); // 101 >= 100 => erra

        batalha.resolverRodadaDeAtaques(t1, golpes[0], t2, golpes[0]);

        assertEquals(150, t2.getPokemon().getHp());
        assertEquals(150, t1.getPokemon().getHp());
        verify(registro, never()).registrarDano(anyString(), anyInt());
        verify(gerador, never()).sortearChance(); // errou: nem chega a sortear crítico
    }

    @Test
    void testSequenciaDeRetornosDoGerador() {
        // thenReturn em SEQUÊNCIA: o 1º golpe (CharSal) acerta e o 2º (contra-ataque do BulbaSal) erra.
        when(gerador.sortearPrecisao()).thenReturn(0, 101);
        when(gerador.sortearChance()).thenReturn(0.99);

        batalha.resolverRodadaDeAtaques(t1, golpes[0], t2, golpes[0]);

        assertEquals(118, t2.getPokemon().getHp());
        assertEquals(150, t1.getPokemon().getHp());
        verify(registro, times(2)).registrarAtaque(anyString(), eq("Tackle"));
        verify(registro, atLeastOnce()).registrarDano(eq("BulbaSal"), anyInt());
    }

    @Test
    void testSequenciaDeEventosNoTurno() {
        when(servicoTerreno.obterTerrenoAtual(any(Clock.class))).thenReturn(Terrenos.CANTEIRO_CENTRAL);
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.99); // sem crítico
        Batalha b = novaBatalha();                      // lê o terreno já stubado
        t2.getPokemon().setQueimado(true);              // gera um evento de STATUS no fim do turno

        b.resolverRodadaDeAtaques(t1, golpes[0], t2, golpes[0]);

        // Ordem da regra 2.4: ataque -> dano -> status -> terreno
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
        t1.getPokemon().setQueimado(true);

        // Os dois usaram item (golpe null): ninguém ataca, mas o fim do turno acontece.
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
        assertEquals(5, salvo.getTurnos());           // 5 golpes de 32 para zerar 150 de HP
        assertEquals("CharSal", salvo.getPokemonVencedor());
        assertEquals("BulbaSal", salvo.getPokemonPerdedor());
    }

    @Test
    void testFalhaNoRepositorioNaoInvalidaBatalha() throws Exception {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.99);
        // doThrow (e não when/thenThrow) porque salvarResultado é um método void.
        doThrow(new Exception("banco fora do ar")).when(repositorio).salvarResultado(any(ResultadoBatalha.class));

        Treinador vencedor = batalha.iniciarCombatePvP(t1, t2, fakeScanner);

        assertSame(t1, vencedor);                      // o vencedor continua definido
        verify(repositorio).salvarResultado(any(ResultadoBatalha.class)); // e a tentativa de salvar aconteceu
    }

    @Test
    void testRepositorioNaoChamadoSeBatalhaNaoTerminou() throws Exception {
        when(gerador.sortearPrecisao()).thenReturn(0);
        when(gerador.sortearChance()).thenReturn(0.99);

        batalha.resolverRodadaDeAtaques(t1, golpes[0], t2, golpes[0]); // só 1 turno: ninguém desmaia

        assertTrue(t1.getPokemon().getHp() > 0 && t2.getPokemon().getHp() > 0);
        verify(repositorio, never()).salvarResultado(any());
    }

    @Test
    void testCpuEscolheGolpePeloGeradorSemLerTeclado() {
        Golpe outro = new Golpe.Normal("Outro", 10);
        Pokemon pCpu = new Pokemon("TotoSal", new TipoAgua(), 50, 40, 150, 10, new Golpe[]{golpes[0], outro});
        Treinador cpu = new Treinador("Computador", pCpu, true);
        when(gerador.sortearChance()).thenReturn(0.99);   // 0.99 * 2 golpes => índice 1 ("Outro"), valor também usado no crítico
        when(gerador.sortearPrecisao()).thenReturn(0);

        // No modo teste o humano sempre usa o 1º golpe; o scanner vazio prova que a CPU não leu o teclado.
        batalha.iniciarCombatePvP(t1, cpu, fakeScanner);

        verify(registro, atLeastOnce()).registrarAtaque(eq("TotoSal"), eq("Outro"));
    }
}
