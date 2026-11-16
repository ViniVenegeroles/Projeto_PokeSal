package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Requisito autoral: captura do Pokésal derrotado no torneio.
 * Colaboradores MOCKADOS (todos recebidos pelo construtor do Torneio):
 *   RegraDeCaptura (decide se deu certo), RepositorioCapturas (guarda), NotificadorCaptura (avisa) e a Batalha.
 * NÃO mockados: Treinador e Pokemon (a captura e a troca são justamente o comportamento deles) e o Scanner,
 * que faz o papel do jogador com texto fixo.
 *
 * Matchers x valores exatos: valores exatos (bulba, "Jogador", "BulbaSal") quando o dado É o que se testa;
 * any()/anyString() quando o argumento não importa naquela verificação. Regra do Mockito: se um argumento
 * usa matcher, todos os da chamada usam (por isso eq(...) aparece misturado com any()).
 */
@ExtendWith(MockitoExtension.class)
class CapturaTorneioTest {
    @Mock Batalha arena;
    @Mock RegraDeCaptura regra;
    @Mock RepositorioCapturas repositorio;
    @Mock NotificadorCaptura notificador;

    private Pokemon charJogador, bulba, squirt, toto;
    private Treinador jogador, adv1, adv2, adv3;
    private Scanner teclado;
    // "quemEstavaEmCampo x oponente" no momento de cada luta (o estado do Treinador muda depois, por isso gravamos na hora).
    private final List<String> lutas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
        Golpe[] g = { new Golpe.Normal("Tackle", 20) };
        charJogador = new Pokemon("CharSal", new TipoFogo(), 50, 40, 150, 90, g);
        bulba = new Pokemon("BulbaSal", new TipoPlanta(), 50, 40, 150, 80, g);
        squirt = new Pokemon("SquirtSal", new TipoAgua(), 50, 40, 150, 70, g);
        toto = new Pokemon("TotoSal", new TipoAgua(), 50, 40, 150, 60, g);
        jogador = new Treinador("Jogador", charJogador);
        adv1 = new Treinador("Adversário 1", bulba, true);
        adv2 = new Treinador("Adversário 2", squirt, true);
        adv3 = new Treinador("Adversário 3", toto, true);

        // lenient: nem todo teste abaixo joga o torneio, e o modo estrito reclamaria do stub sem uso.
        // O humano sempre vence; entre CPUs vence o primeiro da dupla.
        lenient().doAnswer(inv -> {
            Treinador x = inv.getArgument(0);
            Treinador y = inv.getArgument(1);
            lutas.add(x.getPokemon().getNome() + " x " + y.getPokemon().getNome());
            return !x.isCpu() ? x : (!y.isCpu() ? y : x);
        }).when(arena).iniciarCombatePvP(any(), any(), any());
    }

    @AfterEach
    void tearDown() { Batalha.modoTeste = false; }

    private Torneio torneio(List<Treinador> participantes, String digitadoPeloJogador) {
        teclado = new Scanner(digitadoPeloJogador);
        return new Torneio(participantes, arena, teclado, regra, repositorio, notificador);
    }

    private Treinador jogarTorneioDeQuatro(String digitadoPeloJogador) {
        return torneio(Arrays.asList(jogador, adv1, adv2, adv3), digitadoPeloJogador).iniciarTorneio();
    }

    // ------------------------------------------------------------ Treinador

    @Test
    void testTreinadorCapturaRestauraHpENaoDuplica() {
        bulba.dano(500); // derrotado
        assertTrue(jogador.capturar(bulba));
        assertEquals(bulba.getMaxHp(), bulba.getHp());
        assertFalse(jogador.capturar(bulba));
        assertFalse(jogador.capturar(null));
        assertEquals(Arrays.asList(charJogador, bulba), jogador.getEquipe());
    }

    @Test
    void testTrocarPokemonSoAceitaQuemEstaNoTime() {
        jogador.capturar(bulba);
        jogador.trocarPokemon(bulba);
        assertSame(bulba, jogador.getPokemon());
        assertThrows(IllegalArgumentException.class, () -> jogador.trocarPokemon(squirt));
    }

    // -------------------------------------------------- captura no torneio

    @Test
    void testCapturaBemSucedidaEntraNoTimeESalvaENotificaNessaOrdem() throws Exception {
        when(regra.capturaTemSucesso(bulba)).thenReturn(true);

        // "1" = tenta capturar o BulbaSal; "2" = na final escolhe o 2º do time (o BulbaSal capturado).
        Treinador campeao = jogarTorneioDeQuatro("1\n2\n");

        assertSame(jogador, campeao);
        assertEquals(Arrays.asList(charJogador, bulba), jogador.getEquipe());
        assertEquals(Arrays.asList("CharSal x BulbaSal", "SquirtSal x TotoSal", "BulbaSal x SquirtSal"), lutas);

        // A ordem importa: regra decide -> repositório guarda -> notificador avisa.
        InOrder ordem = inOrder(regra, repositorio, notificador);
        ordem.verify(regra).capturaTemSucesso(bulba);
        ordem.verify(repositorio).salvarCaptura("Jogador", "BulbaSal");
        ordem.verify(notificador).notificarCaptura("Jogador", "BulbaSal");
        verifyNoMoreInteractions(regra, repositorio, notificador);
    }

    @Test
    void testRepositorioEIntermediarioNotificadorRecebemTreinadorEPokemonCorretos() throws Exception {
        when(regra.capturaTemSucesso(any())).thenReturn(true);
        jogarTorneioDeQuatro("1\n1\n");

        ArgumentCaptor<String> treinador = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> pokemon = ArgumentCaptor.forClass(String.class);
        verify(repositorio).salvarCaptura(treinador.capture(), pokemon.capture());
        assertEquals("Jogador", treinador.getValue());
        assertEquals("BulbaSal", pokemon.getValue());

        ArgumentCaptor<String> quemFoiAvisado = ArgumentCaptor.forClass(String.class);
        verify(notificador, atLeastOnce()).notificarCaptura(quemFoiAvisado.capture(), eq("BulbaSal"));
        assertEquals("Jogador", quemFoiAvisado.getValue());
    }

    @Test
    void testCapturaQueFalhaNaoMudaOTimeNemSalvaNemNotifica() {
        when(regra.capturaTemSucesso(bulba)).thenReturn(false);

        jogarTorneioDeQuatro("1\n");

        assertEquals(1, jogador.getEquipe().size());
        verify(regra, times(1)).capturaTemSucesso(bulba);
        verifyNoInteractions(repositorio, notificador);
    }

    @Test
    void testRecusarCapturaNaoConsultaNenhumColaborador() {
        jogarTorneioDeQuatro("0\n");

        assertEquals(1, jogador.getEquipe().size());
        verifyNoInteractions(regra, repositorio, notificador);
        verify(arena, times(3)).iniciarCombatePvP(any(), any(), any());
        verify(arena, never()).iniciarCombatePvP(eq(adv1), any(), any());   // adv1 caiu na 1ª fase
    }

    @Test
    void testFalhaNoRepositorioNaoDesfazACapturaENotificaMesmoAssim() throws Exception {
        when(regra.capturaTemSucesso(bulba)).thenReturn(true);
        doThrow(new IOException("banco fora do ar")).when(repositorio).salvarCaptura(anyString(), anyString());

        jogarTorneioDeQuatro("1\n1\n");

        assertTrue(jogador.getEquipe().contains(bulba));            // a captura continua valendo
        verify(notificador).notificarCaptura("Jogador", "BulbaSal"); // e o jogador ainda é avisado
    }

    @Test
    void testFalhaNaNotificacaoNaoDesfazACaptura() throws Exception {
        when(regra.capturaTemSucesso(bulba)).thenReturn(true);
        doThrow(new IOException("sem sinal")).when(notificador).notificarCaptura(anyString(), anyString());

        jogarTorneioDeQuatro("1\n1\n");

        assertTrue(jogador.getEquipe().contains(bulba));
        verify(repositorio).salvarCaptura("Jogador", "BulbaSal");
    }

    @Test
    void testNaFinalNaoOfereceCaptura() {
        // "0" recusa a captura da semifinal. O "1" que sobra capturaria o SquirtSal SE a final oferecesse captura.
        jogarTorneioDeQuatro("0\n1\n");

        assertFalse(jogador.getEquipe().contains(squirt));
        verify(regra, never()).capturaTemSucesso(squirt);
    }

    @Test
    void testNoTorneioDeOitoARegraEConsultadaEmCadaFaseComRetornosEmSequencia() throws Exception {
        List<Treinador> oito = new ArrayList<>(Arrays.asList(jogador, adv1, adv2, adv3));
        Golpe[] g = { new Golpe.Normal("Tackle", 20) };
        for (int i = 4; i <= 7; i++) {
            oito.add(new Treinador("Adversário " + i, new Pokemon("Pokesal" + i, new TipoAgua(), 50, 40, 150, 50 - i, g), true));
        }
        // 1ª tentativa (quartas) falha; 2ª (semifinal) dá certo.
        when(regra.capturaTemSucesso(any())).thenReturn(false, true);

        // "1" = tenta capturar nas quartas, "1" = tenta capturar na semifinal, "1" = mantém o CharSal na final.
        torneio(oito, "1\n1\n1\n").iniciarTorneio();

        verify(regra, times(2)).capturaTemSucesso(any());
        assertEquals(2, jogador.getEquipe().size());
        assertSame(squirt, jogador.getEquipe().get(1));              // o derrotado da semifinal (Adversário 2)
        verify(repositorio, times(1)).salvarCaptura(eq("Jogador"), eq("SquirtSal"));
        verify(arena, times(7)).iniciarCombatePvP(any(), any(), any());   // 8 treinadores = 7 lutas
    }

    // -------------------------------------------------- troca entre capturados

    @Test
    void testEntradaInvalidaNoMenuRepeteAPerguntaEMantemOPokemonEscolhido() {
        when(regra.capturaTemSucesso(bulba)).thenReturn(true);

        // "1" captura; no menu da final vem lixo ("abc") e número fora da faixa ("9") antes do "1" (manter o CharSal).
        jogarTorneioDeQuatro("1\nabc\n9\n1\n");

        assertEquals(2, jogador.getEquipe().size());
        assertSame(charJogador, jogador.getPokemon());
        assertEquals("CharSal x SquirtSal", lutas.get(2));
    }

    @Test
    void testSemEntradaNoTecladoNaoCapturaENaoTrava() {
        jogarTorneioDeQuatro("");

        assertEquals(1, jogador.getEquipe().size());
        verify(arena, times(3)).iniciarCombatePvP(any(), any(), any());
        verifyNoInteractions(regra, repositorio, notificador);
    }

    @Test
    void testAdversarioCpuNuncaCapturaNemPerguntaNada() {
        Treinador c1 = new Treinador("C1", bulba, true), c2 = new Treinador("C2", squirt, true);
        Treinador c3 = new Treinador("C3", toto, true), c4 = new Treinador("C4", charJogador, true);
        Scanner sc = new Scanner("1\n1\n1\n");

        new Torneio(Arrays.asList(c1, c2, c3, c4), arena, sc, regra, repositorio, notificador).iniciarTorneio();

        assertEquals(1, c1.getEquipe().size());
        assertTrue(sc.hasNextInt());   // nada foi consumido do teclado
        verifyNoInteractions(regra, repositorio, notificador);
    }
}
