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

@ExtendWith(MockitoExtension.class)
class CapturaTorneioTest {
    @Mock Batalha arena;
    @Mock RegraDeCaptura regra;
    @Mock RepositorioCapturas repositorio;
    @Mock NotificadorCaptura notificador;

    private Pokesal charJogador, bulba, squirt, toto;
    private Treinador jogador, adv1, adv2, adv3;
    private Scanner teclado;

    private final List<String> lutas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
        Golpe[] g = { new Golpe.Normal("Tackle", 20) };
        charJogador = new Pokesal("CharSal", new TipoFogo(), 50, 40, 150, 90, g);
        bulba = new Pokesal("BulbaSal", new TipoPlanta(), 50, 40, 150, 80, g);
        squirt = new Pokesal("SquirtSal", new TipoAgua(), 50, 40, 150, 70, g);
        toto = new Pokesal("TotoSal", new TipoAgua(), 50, 40, 150, 60, g);
        jogador = new Treinador("Jogador", charJogador);
        adv1 = new Treinador("Adversário 1", bulba, true);
        adv2 = new Treinador("Adversário 2", squirt, true);
        adv3 = new Treinador("Adversário 3", toto, true);

        lenient().doAnswer(inv -> {
            Treinador x = inv.getArgument(0);
            Treinador y = inv.getArgument(1);
            lutas.add(x.getPokesal().getNome() + " x " + y.getPokesal().getNome());
            return !x.isCpu() ? x : (!y.isCpu() ? y : x);
        }).when(arena).iniciarCombatePvP(any(), any(), any());
    }

    @AfterEach
    void tearDown() {
        Batalha.modoTeste = false;
    }

    private Torneio torneio(List<Treinador> participantes, String digitadoPeloJogador) {
        teclado = new Scanner(digitadoPeloJogador);
        return new Torneio(participantes, arena, teclado, regra, repositorio, notificador);
    }

    private Treinador jogarTorneioDeQuatro(String digitadoPeloJogador) {
        return torneio(Arrays.asList(jogador, adv1, adv2, adv3), digitadoPeloJogador).iniciarTorneio();
    }

    @Test
    void testTreinadorCapturaRestauraHpENaoDuplica() {
        bulba.dano(500);
        assertTrue(jogador.capturar(bulba));
        assertEquals(bulba.getMaxHp(), bulba.getHp());
        assertFalse(jogador.capturar(bulba));
        assertFalse(jogador.capturar(null));
        assertEquals(Arrays.asList(charJogador, bulba), jogador.getEquipe());
    }

    @Test
    void testTrocarPokesalSoAceitaQuemEstaNoTime() {
        jogador.capturar(bulba);
        jogador.trocarPokesal(bulba);
        assertSame(bulba, jogador.getPokesal());
        assertThrows(IllegalArgumentException.class, () -> jogador.trocarPokesal(squirt));
    }

    @Test
    void testCapturaBemSucedidaEntraNoTimeESalvaENotificaNessaOrdem() throws Exception {
        when(regra.capturaTemSucesso(bulba)).thenReturn(true);

        Treinador campeao = jogarTorneioDeQuatro("1\n2\n");

        assertSame(jogador, campeao);
        assertEquals(Arrays.asList(charJogador, bulba), jogador.getEquipe());
        assertEquals(Arrays.asList("CharSal x BulbaSal", "SquirtSal x TotoSal", "BulbaSal x SquirtSal"), lutas);

        InOrder ordem = inOrder(regra, repositorio, notificador);
        ordem.verify(regra).capturaTemSucesso(bulba);
        ordem.verify(repositorio).salvarCaptura("Jogador", "BulbaSal");
        ordem.verify(notificador).notificarCaptura("Jogador", "BulbaSal");
        verifyNoMoreInteractions(regra, repositorio, notificador);
    }

    @Test
    void testRepositorioEIntermediarioNotificadorRecebemTreinadorEPokesalCorretos() throws Exception {
        when(regra.capturaTemSucesso(any())).thenReturn(true);
        jogarTorneioDeQuatro("1\n1\n");

        ArgumentCaptor<String> treinador = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> pokesal = ArgumentCaptor.forClass(String.class);
        verify(repositorio).salvarCaptura(treinador.capture(), pokesal.capture());
        assertEquals("Jogador", treinador.getValue());
        assertEquals("BulbaSal", pokesal.getValue());

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
        verify(arena, never()).iniciarCombatePvP(eq(adv1), any(), any());
    }

    @Test
    void testFalhaNoRepositorioNaoDesfazACapturaENotificaMesmoAssim() throws Exception {
        when(regra.capturaTemSucesso(bulba)).thenReturn(true);
        doThrow(new IOException("banco fora do ar")).when(repositorio).salvarCaptura(anyString(), anyString());

        jogarTorneioDeQuatro("1\n1\n");

        assertTrue(jogador.getEquipe().contains(bulba));
        verify(notificador).notificarCaptura("Jogador", "BulbaSal");
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
        jogarTorneioDeQuatro("0\n1\n");

        assertFalse(jogador.getEquipe().contains(squirt));
        verify(regra, never()).capturaTemSucesso(squirt);
    }

    @Test
    void testNoTorneioDeOitoARegraEConsultadaEmCadaFaseComRetornosEmSequencia() throws Exception {
        List<Treinador> oito = new ArrayList<>(Arrays.asList(jogador, adv1, adv2, adv3));
        Golpe[] g = { new Golpe.Normal("Tackle", 20) };
        for (int i = 4; i <= 7; i++) {
            oito.add(new Treinador("Adversário " + i, new Pokesal("Pokesal" + i, new TipoAgua(), 50, 40, 150, 50 - i, g), true));
        }

        when(regra.capturaTemSucesso(any())).thenReturn(false, true);

        torneio(oito, "1\n1\n1\n").iniciarTorneio();

        verify(regra, times(2)).capturaTemSucesso(any());
        assertEquals(2, jogador.getEquipe().size());
        assertSame(squirt, jogador.getEquipe().get(1));
        verify(repositorio, times(1)).salvarCaptura(eq("Jogador"), eq("SquirtSal"));
        verify(arena, times(7)).iniciarCombatePvP(any(), any(), any());
    }

    @Test
    void testEntradaInvalidaNoMenuRepeteAPerguntaEMantemOPokesalEscolhido() {
        when(regra.capturaTemSucesso(bulba)).thenReturn(true);

        jogarTorneioDeQuatro("1\nabc\n9\n1\n");

        assertEquals(2, jogador.getEquipe().size());
        assertSame(charJogador, jogador.getPokesal());
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
        assertTrue(sc.hasNextInt());
        verifyNoInteractions(regra, repositorio, notificador);
    }
}
