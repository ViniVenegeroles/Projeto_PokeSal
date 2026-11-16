package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TorneioTest {
    @Mock Batalha arena; // a Batalha inteira é mockada: o torneio só decide QUEM avança
    // Colaboradores da captura (regra 2.5). Aqui ninguém captura (teclado vazio), então nenhum deles deve ser tocado.
    @Mock RegraDeCaptura regra;
    @Mock RepositorioCapturas repositorio;
    @Mock NotificadorCaptura notificador;

    private Treinador a, b, c, d;
    private final Scanner sc = new Scanner("");

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true; // Torneio chama Batalha.pausar (estático): sem sleep nos testes
        Golpe[] g = { new Golpe.Normal("Tackle", 20) };
        a = new Treinador("A", new Pokemon("CharSal", new TipoFogo(), 50, 40, 150, 90, g));
        b = new Treinador("B", new Pokemon("BulbaSal", new TipoPlanta(), 50, 40, 150, 80, g));
        c = new Treinador("C", new Pokemon("SquirtSal", new TipoAgua(), 50, 40, 150, 70, g));
        d = new Treinador("D", new Pokemon("TotoSal", new TipoAgua(), 50, 40, 150, 60, g));
    }

    @AfterEach
    void tearDown() { Batalha.modoTeste = false; }

    private Torneio torneio(List<Treinador> participantes) {
        return new Torneio(participantes, arena, sc, regra, repositorio, notificador);
    }

    private static Treinador treinador(String nome, int spd) {
        return new Treinador(nome, new Pokemon(nome + "Sal", new TipoAgua(), 50, 40, 150, spd, new Golpe[] { new Golpe.Normal("Tackle", 20) }));
    }

    private static int vitorias(Torneio torneio, Treinador t) { return torneio.getVitorias().get(t); }

    @Test
    void testChaveamentoTorneioAvancaVencedores() {
        // Valores EXATOS (e não any()): o teste precisa provar QUEM enfrenta QUEM em cada fase.
        // doReturn().when() em vez de when().thenReturn(): o when() chama o método de verdade no mock com
        // argumentos diferentes dos stubs anteriores, e o modo estrito do Mockito acusa PotentialStubbingProblem.
        doReturn(b).when(arena).iniciarCombatePvP(a, b, sc); // semifinal 1: B vence
        doReturn(c).when(arena).iniciarCombatePvP(c, d, sc); // semifinal 2: C vence
        doReturn(c).when(arena).iniciarCombatePvP(b, c, sc); // final: C vence

        Treinador campeao = torneio(Arrays.asList(a, b, c, d)).iniciarTorneio();

        assertSame(c, campeao);
        InOrder ordem = inOrder(arena);
        ordem.verify(arena).iniciarCombatePvP(a, b, sc);
        ordem.verify(arena).iniciarCombatePvP(c, d, sc);
        ordem.verify(arena).iniciarCombatePvP(b, c, sc);
        verifyNoMoreInteractions(arena); // eliminados (A e D) não lutam de novo
        verifyNoInteractions(regra, repositorio, notificador);
    }

    @Test
    void testTorneioExigeQuatroOuOitoTreinadores() {
        assertThrows(IllegalArgumentException.class, () -> torneio(Arrays.asList(a, b, c)));
        assertThrows(IllegalArgumentException.class, () -> torneio(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> torneio(null));
    }

    @Test
    void testTorneioDeOitoTreinadoresTemTresFasesEUmCampeao() {
        Treinador e = treinador("E", 50), f = treinador("F", 40), g8 = treinador("G", 30), h = treinador("H", 20);
        doReturn(b).when(arena).iniciarCombatePvP(a, b, sc);    // quartas
        doReturn(c).when(arena).iniciarCombatePvP(c, d, sc);
        doReturn(e).when(arena).iniciarCombatePvP(e, f, sc);
        doReturn(h).when(arena).iniciarCombatePvP(g8, h, sc);
        doReturn(c).when(arena).iniciarCombatePvP(b, c, sc);    // semifinais
        doReturn(h).when(arena).iniciarCombatePvP(e, h, sc);
        doReturn(h).when(arena).iniciarCombatePvP(c, h, sc);    // final

        Torneio torneio = torneio(Arrays.asList(a, b, c, d, e, f, g8, h));
        Treinador campeao = torneio.iniciarTorneio();

        assertSame(h, campeao);
        InOrder ordem = inOrder(arena);
        ordem.verify(arena).iniciarCombatePvP(a, b, sc);
        ordem.verify(arena).iniciarCombatePvP(c, d, sc);
        ordem.verify(arena).iniciarCombatePvP(e, f, sc);
        ordem.verify(arena).iniciarCombatePvP(g8, h, sc);
        ordem.verify(arena).iniciarCombatePvP(b, c, sc);
        ordem.verify(arena).iniciarCombatePvP(e, h, sc);
        ordem.verify(arena).iniciarCombatePvP(c, h, sc);
        verifyNoMoreInteractions(arena);   // 8 treinadores = 7 lutas, nem uma a mais
        assertEquals(3, vitorias(torneio, h));
        assertEquals(2, vitorias(torneio, c));
        assertEquals(1, vitorias(torneio, b));
        assertEquals(1, vitorias(torneio, e));
        assertEquals(0, vitorias(torneio, a));
        assertEquals(0, vitorias(torneio, d));
    }

    @Test
    void testClassificacaoFinalListaPrimeiroQuemVenceuMaisPartidas() {
        doReturn(b).when(arena).iniciarCombatePvP(a, b, sc);
        doReturn(c).when(arena).iniciarCombatePvP(c, d, sc);
        doReturn(c).when(arena).iniciarCombatePvP(b, c, sc);
        Torneio torneio = torneio(Arrays.asList(a, b, c, d));

        String saida = ConsoleTestes.comConsole(torneio::iniciarTorneio);

        String[] linhas = saida.substring(saida.indexOf("CLASSIFICAÇÃO FINAL")).split("\\R");
        // linhas[0] = título, linhas[1] = cabeçalho das colunas, depois uma linha por treinador.
        assertTrue(linhas[2].startsWith("1   C ") && linhas[2].contains("Campeão"));
        assertTrue(linhas[3].startsWith("2   B ") && linhas[3].contains("Vice-campeão"));
        assertTrue(linhas[4].startsWith("3   A ") && linhas[4].contains("Eliminado: Semifinal"));   // empate em 0 vitórias: ordem de inscrição
        assertTrue(linhas[5].startsWith("4   D ") && linhas[5].contains("Eliminado: Semifinal"));
    }

    @Test
    void testCadaBatalhaComecaComOsPokesalsRevitalizados() {
        List<String> estados = new ArrayList<>();
        doAnswer(inv -> {
            Treinador x = inv.getArgument(0);
            Treinador y = inv.getArgument(1);
            // Estado de cada um NO MOMENTO em que a luta começa: "hpDoX/hpDoY/xQueimado/yQueimado".
            estados.add(x.getPokemon().getHp() + "/" + y.getPokemon().getHp() + "/" + x.getPokemon().isQueimado() + "/" + y.getPokemon().isQueimado());
            x.getPokemon().dano(60);              // o primeiro sempre vence, mas sai ferido e queimado
            x.getPokemon().setQueimado(true);
            return x;
        }).when(arena).iniciarCombatePvP(any(), any(), any());
        a.getPokemon().dano(100);                 // já chegam machucados ao torneio
        b.getPokemon().setQueimado(true);

        torneio(Arrays.asList(a, b, c, d)).iniciarTorneio();

        assertEquals(3, estados.size());
        for (String estado : estados) assertEquals("150/150/false/false", estado);   // inclusive a final do A, que lutou antes
    }
}
