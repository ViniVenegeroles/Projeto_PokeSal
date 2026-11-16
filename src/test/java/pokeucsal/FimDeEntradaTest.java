package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teclado que acaba (Ctrl+D / entrada redirecionada) e mensagens de feedback da mochila.
 * Antes, Batalha.escolherAcao e Bag.abrirMochila chamavam sc.next() sem checar hasNext() e o jogo caía
 * com NoSuchElementException; e escolher um item esgotado voltava ao menu sem dizer nada.
 */
class FimDeEntradaTest {
    private static final Clock DIA = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneOffset.UTC);

    private RegistroEmMemoria registro;
    private GeradorFixo gerador;
    private List<ResultadoBatalha> salvos;

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
        registro = new RegistroEmMemoria();
        gerador = new GeradorFixo(0, 0.99);   // nunca erra, nunca dá crítico, nunca aplica status
        salvos = new ArrayList<>();
    }

    @AfterEach
    void tearDown() { Batalha.modoTeste = false; }

    private static Pokemon lutador(String nome, int hp, int spd) {
        return new Pokemon(nome, new TipoFogo(), 0, 0, hp, spd, new Golpe[]{ new Golpe.Normal("Pancada", 50) });
    }

    @Test
    void testBatalhaComTecladoAcabandoJogaOPrimeiroGolpeEnaoQuebra() {
        Batalha arena = new Batalha(c -> Terrenos.NEUTRO, DIA, salvos::add, registro, gerador);
        Treinador t1 = new Treinador("J1", lutador("CharSal", 100, 50));   // mais rápido: vence
        Treinador t2 = new Treinador("J2", lutador("CyndaSal", 100, 40));
        Treinador[] vencedor = new Treinador[1];

        // Modo teste desligado de propósito: é o único jeito de o menu (e a leitura do teclado) ser exercitado.
        ConsoleTestes.comConsole(() -> vencedor[0] = arena.iniciarCombatePvP(t1, t2, new Scanner("")));

        assertSame(t1, vencedor[0]);
        assertEquals(1, salvos.size());
        assertEquals("J1", salvos.get(0).getVencedor());
    }

    @Test
    void testBatalhaComTecladoAcabandoNoMeioDaEscolhaDeGolpe() {
        Batalha arena = new Batalha(c -> Terrenos.NEUTRO, DIA, salvos::add, registro, gerador);
        Treinador t1 = new Treinador("J1", lutador("CharSal", 100, 50));
        Treinador t2 = new Treinador("J2", lutador("CyndaSal", 100, 40));

        // "1" escolhe Atacar e o teclado acaba antes do número do golpe: não pode lançar exceção.
        ConsoleTestes.comConsole(() -> arena.iniciarCombatePvP(t1, t2, new Scanner("1\n")));

        assertEquals(1, salvos.size());
        assertTrue(t1.getPokemon().getHp() == 0 || t2.getPokemon().getHp() == 0);
    }

    @Test
    void testMochilaSemEntradaVoltaSemQuebrarENaoGastaItem() {
        Bag bag = new Bag();
        Pokemon aliado = lutador("CharSal", 100, 50);
        Treinador treinador = new Treinador("Jogador", aliado);
        Batalha arena = new Batalha(c -> Terrenos.NEUTRO, DIA, r -> { }, registro, gerador);

        assertFalse(bag.abrirMochila(aliado, arena, registro, treinador, new Scanner(""), gerador));
        assertEquals(0, registro.eventos.size());
    }

    @Test
    void testItemEsgotadoAvisaOJogadorENaoConsomeOLimiteDaBatalha() {
        Bag bag = new Bag();
        Pokemon aliado = lutador("CharSal", 100, 50);
        Treinador treinador = new Treinador("Jogador", aliado);
        Batalha arena = new Batalha(c -> Terrenos.NEUTRO, DIA, r -> { }, registro, gerador);
        Scanner teclado = new Scanner("1\n1\n");   // Potion (1 unidade), depois Potion de novo (já acabou)

        boolean[] usou = new boolean[2];
        String saida = ConsoleTestes.comConsole(() -> {
            usou[0] = bag.abrirMochila(aliado, arena, registro, treinador, teclado, gerador);
            usou[1] = bag.abrirMochila(aliado, arena, registro, treinador, teclado, gerador);
        });

        assertTrue(usou[0]);
        assertFalse(usou[1]);
        assertTrue(saida.contains("Você não tem mais esse item!"));
        assertEquals(1, registro.quantos("item:Jogador:Potion"));
    }

    @Test
    void testOpcaoInvalidaNaMochilaAvisaEVolta() {
        Bag bag = new Bag();
        Pokemon aliado = lutador("CharSal", 100, 50);
        Treinador treinador = new Treinador("Jogador", aliado);
        Batalha arena = new Batalha(c -> Terrenos.NEUTRO, DIA, r -> { }, registro, gerador);

        boolean[] usou = new boolean[1];
        String saida = ConsoleTestes.comConsole(() ->
                usou[0] = bag.abrirMochila(aliado, arena, registro, treinador, new Scanner("9"), gerador));

        assertFalse(usou[0]);
        assertTrue(saida.contains("Opção Inválida!"));
    }
}
