package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class BagTest {
    private static final Clock DIA = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneOffset.UTC);
    private static final Clock NOITE = Clock.fixed(Instant.parse("2026-11-13T23:00:00Z"), ZoneOffset.UTC);

    private RegistroEmMemoria registro;
    private GeradorFixo gerador;
    private Bag bag;
    private Pokesal aliado;
    private Treinador treinador;
    private Batalha arena;

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
        registro = new RegistroEmMemoria();
        gerador = new GeradorFixo(0, 0.0);
        bag = new Bag();
        aliado = new Pokesal("CharSal", new TipoFogo(), 50, 40, 200, 80, new Golpe[]{});
        treinador = new Treinador("Jogador", aliado);
        arena = new Batalha(c -> Terrenos.NEUTRO, DIA, r -> { }, registro, gerador);
    }

    @AfterEach
    void tearDown() {
        Batalha.modoTeste = false;
    }

    private boolean abrir(Scanner teclado) {
        return bag.abrirMochila(aliado, arena, registro, treinador, teclado, gerador);
    }

    private boolean abrir(String teclado) {
        return abrir(new Scanner(teclado));
    }

    @Test
    void testPotionCuraQuinzePorCentoDoHpMaximo() {
        aliado.dano(150);
        int antes = aliado.getHp();

        assertTrue(abrir("1"));

        assertEquals(antes + (int) (aliado.getMaxHp() * 0.15), aliado.getHp());
        assertEquals(1, registro.quantos("item:Jogador:Potion"));
    }

    @Test
    void testSuperPotionCuraTrintaECincoPorCento() {
        aliado.dano(150);
        int antes = aliado.getHp();

        assertTrue(abrir("2"));

        assertEquals(antes + (int) (aliado.getMaxHp() * 0.35), aliado.getHp());
        assertEquals(1, registro.quantos("item:Jogador:Super Potion"));
    }

    @Test
    void testAntidotoLimpaTodosOsStatusEDevolveAVelocidade() {
        aliado.setQueimado(true);
        aliado.setEnvenenado(true);
        aliado.setParalisado(true);

        assertTrue(abrir("3"));

        assertFalse(aliado.isQueimado());
        assertFalse(aliado.isEnvenenado());
        assertFalse(aliado.isParalisado());
        assertEquals(80, aliado.getSpd());
        assertEquals(1, registro.quantos("item:Jogador:Antídoto"));
    }

    @Test
    void testSalShardDeDiaMudaOTerrenoParaOPrimeiroPossivel() {
        assertEquals(Terrenos.NEUTRO, arena.getClimaAtual());

        assertTrue(abrir("4"));

        assertEquals(Terrenos.ASFALTO_QUENTE, arena.getClimaAtual());
        assertEquals(1, registro.quantos("item:Jogador:Sal Shard"));
        assertEquals(1, registro.quantos("terreno:Campo:Sal Shard ativou: Asfalto Quente"));
    }

    @Test
    void testSalShardSempreMudaOTerrenoQualquerQueSejaOAtualOSorteioOuAHora() {
        String[] iniciais = { Terrenos.NEUTRO, Terrenos.ASFALTO_QUENTE, Terrenos.PISO_ESCORREGADIO, Terrenos.CANTEIRO_CENTRAL };
        double[] sorteios = { 0.0, 0.5, 0.99 };
        for (String inicial : iniciais) {
            for (double sorteio : sorteios) {
                for (Clock relogio : new Clock[] { DIA, NOITE }) {
                    GeradorFixo fixo = new GeradorFixo(0, sorteio);
                    Batalha batalha = new Batalha(c -> inicial, relogio, r -> { }, registro, fixo);
                    batalha.mudarTerrenoSalShard(registro, fixo);
                    assertFalse(inicial.equals(batalha.getClimaAtual()));
                    assertTrue(Terrenos.todos().contains(batalha.getClimaAtual()));
                }
            }
        }
    }

    @Test
    void testSalShardDeNoitePodeCriarAsfaltoQuente() {
        Batalha arenaNoite = new Batalha(c -> Terrenos.NEUTRO, NOITE, r -> { }, registro, gerador);

        assertTrue(bag.abrirMochila(aliado, arenaNoite, registro, treinador, new Scanner("4"), gerador));

        assertEquals(Terrenos.ASFALTO_QUENTE, arenaNoite.getClimaAtual());
    }

    @Test
    void testItemEsgotadoNaoGastaOLimiteDeItens() {
        Scanner teclado = new Scanner("1\n1\n2\n");
        assertTrue(abrir(teclado));
        assertFalse(abrir(teclado));
        assertTrue(abrir(teclado));
        assertEquals(1, registro.quantos("item:Jogador:Potion"));
        assertEquals(1, registro.quantos("item:Jogador:Super Potion"));
    }

    @Test
    void testItemEsgotadoDevolveFalsoParaCadaTipoDeItem() {
        Scanner teclado = new Scanner("1\n1\n"
                + "2\n2\n"
                + "3\n3\n"
                + "4\n4\n");

        for (int item = 1; item <= 4; item++) {
            bag.resetarUsoBatalha();
            assertTrue(abrir(teclado));
            assertFalse(abrir(teclado));
        }
    }

    @Test
    void testUsoLimiteDeItensExcedido() {
        Scanner teclado = new Scanner("1\n2\n3\n");
        assertTrue(abrir(teclado));
        assertTrue(abrir(teclado));
        LimiteDeItensExcedidoException erro = assertThrows(LimiteDeItensExcedidoException.class, () -> abrir(teclado));
        assertEquals("Limite Máximo De 2 itens Atingido Nesta Batalha.", erro.getMessage());
        assertEquals(0, registro.quantos("item:Jogador:Antídoto"));
    }

    @Test
    void testResetarUsoBatalhaDevolveItensELimite() {
        Scanner teclado = new Scanner("1\n2\n1\n");
        abrir(teclado);
        abrir(teclado);
        assertThrows(LimiteDeItensExcedidoException.class, () -> abrir(new Scanner("3")));

        bag.resetarUsoBatalha();

        assertTrue(abrir(new Scanner("1")));
    }

    @Test
    void testVoltarOpcaoInvalidaOuTextoNaoUsamItemNenhum() {
        assertFalse(abrir("0"));
        assertFalse(abrir("9"));
        assertFalse(abrir("abc"));
        assertTrue(registro.eventos.isEmpty());
    }

    @Test
    void testMochilaMostraMenuEMensagensNoConsole() {
        String saida = ConsoleTestes.comConsole(() -> {
            abrir(new Scanner("1"));
            abrir(new Scanner("2"));
            assertThrows(LimiteDeItensExcedidoException.class, () -> abrir(new Scanner("3")));
            bag.resetarUsoBatalha();
            abrir(new Scanner("3"));
            abrir(new Scanner("4"));
        });
        assertTrue(saida.contains("--- MOCHILA (Itens Usados: 0/2) ---"));
        assertTrue(saida.contains("Usou Potion e Curou 15%!"));
        assertTrue(saida.contains("Usou Super Potion e Curou 35%!"));
        assertTrue(saida.contains("Usou Antídoto e Limpou os Status!"));
        assertTrue(saida.contains("A Sal Shard brilhou!"));
    }
}
