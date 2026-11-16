package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BatalhaConsoleTest {
    private static final Clock DIA = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneOffset.UTC);
    private static final String DOIS_ACERTOS_DE_CADA = "1\n1\n1\n1\n1\n1\n1\n1\n";

    private RegistroEmMemoria registro;
    private GeradorFixo gerador;
    private List<ResultadoBatalha> salvos;

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
        registro = new RegistroEmMemoria();
        gerador = new GeradorFixo(0, 0.99);
        salvos = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        Batalha.modoTeste = false;
    }

    private Batalha batalha(String terreno) {
        return new Batalha(c -> terreno, DIA, salvos::add, registro, gerador);
    }

    private static Pokesal lutador(String nome, TipoPoke tipo, int hp, int spd) {
        return new Pokesal(nome, tipo, 0, 0, hp, spd, new Golpe[]{ new Golpe.Normal("Pancada", 50) });
    }

    @Test
    void testPvPComEntradasInvalidasMochilaVoltarEVitoriaDoMaisRapido() {
        Batalha arena = batalha(Terrenos.NEUTRO);
        Treinador t1 = new Treinador("J1", lutador("CharSal", new TipoFogo(), 100, 50));
        Treinador t2 = new Treinador("J2", lutador("BulbaSal", new TipoPlanta(), 100, 40));

        String teclado = "abc\n7\n1\n9\n1\nabc\n2\n0\n1\n1\n" + "1\n1\n" + "1\n1\n" + "1\n1\n";
        Treinador[] vencedor = new Treinador[1];

        String saida = ConsoleTestes.comConsole(() -> vencedor[0] = arena.iniciarCombatePvP(t1, t2, new Scanner(teclado)));

        assertSame(t1, vencedor[0]);
        assertEquals(50, t1.getPokesal().getHp());
        assertEquals(0, t2.getPokesal().getHp());
        assertEquals(1, salvos.size());
        assertEquals("J1", salvos.get(0).getVencedor());
        assertEquals(2, salvos.get(0).getTurnos());
        assertTrue(saida.contains("Batalha PvP iniciada entre CharSal (J1) e BulbaSal (J2)!"));
        assertTrue(saida.contains("Condição do Ambiente de Combate: Terreno Neutro!"));
        assertTrue(saida.contains("Opção Inválida!"));
        assertTrue(saida.contains("--- MOCHILA (Itens Usados: 0/2) ---"));
        assertTrue(saida.contains("CharSal é Mais Rápido e Toma a Frente!"));
        assertTrue(saida.contains("Contra-ataque de BulbaSal!"));
        assertTrue(saida.contains("BulbaSal Desmaiou Antes de Conseguir Atacar!"));
        assertTrue(saida.contains("J1 Venceu!"));
    }

    @Test
    void testPvPOndeUmUsaItemEOOutroAtacaGastaOTurnoMasContaNoResultado() {
        Batalha arena = batalha(Terrenos.NEUTRO);
        Treinador t1 = new Treinador("J1", lutador("CharSal", new TipoFogo(), 100, 50));
        Treinador t2 = new Treinador("J2", lutador("BulbaSal", new TipoPlanta(), 100, 40));

        String teclado = "2\n1\n" + "1\n1\n"
                + "1\n1\n" + "2\n1\n"
                + "1\n1\n" + "1\n1\n";

        Treinador vencedor = comConsole(() -> arena.iniciarCombatePvP(t1, t2, new Scanner(teclado)));

        assertSame(t1, vencedor);
        assertEquals(3, salvos.get(0).getTurnos());
        assertEquals(1, registro.quantos("item:J1:Potion"));
        assertEquals(1, registro.quantos("item:J2:Potion"));
        assertEquals(50, t1.getPokesal().getHp());
    }

    @Test
    void testTerrenoApareceNoAnuncioECadaTerrenoTemSuaCorNoConsole() {
        assertTrue(partidaCurta(Terrenos.ASFALTO_QUENTE).contains("\u001B[31m"));
        assertTrue(partidaCurta(Terrenos.PISO_ESCORREGADIO).contains("\u001B[34m"));
        assertTrue(partidaCurta(Terrenos.CANTEIRO_CENTRAL).contains("\u001B[32m"));

        String neutro = partidaCurta(Terrenos.NEUTRO);
        assertFalse(neutro.contains("\u001B[31m") || neutro.contains("\u001B[34m") || neutro.contains("\u001B[32m"));
        assertTrue(neutro.contains("\u001B[0m"));
    }

    private String partidaCurta(String terreno) {
        Batalha arena = batalha(terreno);
        Treinador t1 = new Treinador("J1", lutador("CharSal", new TipoFogo(), 100, 50));
        Treinador t2 = new Treinador("J2", lutador("BulbaSal", new TipoPlanta(), 100, 40));
        return ConsoleTestes.comConsole(() -> arena.iniciarCombatePvP(t1, t2, new Scanner(DOIS_ACERTOS_DE_CADA)));
    }

    @Test
    void testPvEVitoriaDoJogador() {
        Batalha arena = batalha(Terrenos.NEUTRO);
        Treinador jogador = new Treinador("J1", lutador("CharSal", new TipoFogo(), 100, 50));
        Treinador inimigo = new Treinador("Rival", lutador("BulbaSal", new TipoPlanta(), 100, 40));
        boolean[] venceu = new boolean[1];

        String saida = ConsoleTestes.comConsole(() -> venceu[0] = arena.iniciarCombate(jogador, inimigo, new Scanner("1\n1\n1\n1\n")));

        assertTrue(venceu[0]);
        assertTrue(saida.contains("Um BulbaSal Adversário se Aproxima!"));
        assertTrue(saida.contains("--- Status de BulbaSal ---"));
        assertTrue(saida.contains("Você Venceu a Batalha!"));
        assertEquals("J1", salvos.get(0).getVencedor());
    }

    @Test
    void testPvEDerrotaDoJogador() {
        Batalha arena = batalha(Terrenos.NEUTRO);
        Treinador jogador = new Treinador("J1", lutador("CharSal", new TipoFogo(), 100, 10));
        Treinador inimigo = new Treinador("Rival", lutador("BulbaSal", new TipoPlanta(), 100, 90));
        boolean[] venceu = new boolean[] { true };

        String saida = ConsoleTestes.comConsole(() -> venceu[0] = arena.iniciarCombate(jogador, inimigo, new Scanner("1\n1\n1\n1\n")));

        assertFalse(venceu[0]);
        assertTrue(saida.contains("Seu PokeSal Desmaiou..."));
        assertEquals("Rival", salvos.get(0).getVencedor());
    }

    @Test
    void testCanteiroCentralCuraSoPokesalDePlantaQueEstaDePe() {
        Batalha arena = batalha(Terrenos.CANTEIRO_CENTRAL);
        Pokesal planta = lutador("BulbaSal", new TipoPlanta(), 100, 40);
        Pokesal fogo = lutador("CharSal", new TipoFogo(), 100, 50);
        planta.dano(50);
        fogo.dano(50);

        arena.processarFimDeTurno(planta, fogo);

        assertEquals(50 + (int) (100 * 0.05), planta.getHp());
        assertEquals(50, fogo.getHp());
        assertEquals(1, registro.quantos("terreno:BulbaSal:Cura Terreno"));

        planta.dano(1000);
        planta.dano(1000);
        arena.processarFimDeTurno(planta, fogo);
        assertEquals(0, planta.getHp());
        assertEquals(1, registro.quantos("terreno:BulbaSal:Cura Terreno"));
    }

    @Test
    void testForaDoCanteiroCentralNinguemSeCura() {
        Batalha arena = batalha(Terrenos.NEUTRO);
        Pokesal planta = lutador("BulbaSal", new TipoPlanta(), 100, 40);
        planta.dano(50);

        arena.processarFimDeTurno(planta, lutador("CharSal", new TipoFogo(), 100, 50));

        assertEquals(50, planta.getHp());
        assertTrue(registro.eventos.isEmpty());
    }

    @Test
    void testCanteiroCentralAvisaNoConsole() {
        Batalha arena = batalha(Terrenos.CANTEIRO_CENTRAL);
        Pokesal planta = lutador("BulbaSal", new TipoPlanta(), 100, 40);
        planta.dano(50);

        String saida = ConsoleTestes.comConsole(() -> arena.processarFimDeTurno(planta, lutador("CharSal", new TipoFogo(), 100, 50)));

        assertTrue(saida.contains("--- Fim de Turno ---"));
        assertTrue(saida.contains("[TERRENO] O Canteiro Central Restaurou a Vida de BulbaSal!"));
    }

    @Test
    void testEmpateDuploPorQueimaduraFavoreceOMaisRapido() {
        assertEquals("J1", empateDuplo(50, 40));
        assertEquals("J2", empateDuplo(30, 40));
        assertEquals("J1", empateDuplo(40, 40));
    }

    private String empateDuplo(int spd1, int spd2) {
        Batalha arena = batalha(Terrenos.NEUTRO);
        Golpe[] foco = { new Golpe.Buff("Foco") };
        Pokesal p1 = new Pokesal("CharSal", new TipoFogo(), 0, 0, 16, spd1, foco);
        Pokesal p2 = new Pokesal("CyndaSal", new TipoFogo(), 0, 0, 16, spd2, foco);
        for (Pokesal p : new Pokesal[] { p1, p2 }) {
            p.dano(13);
            p.dano(2);
            p.setQueimado(true);
        }
        Treinador t1 = new Treinador("J1", p1);
        Treinador t2 = new Treinador("J2", p2);

        Treinador vencedor = arena.iniciarCombatePvP(t1, t2, new Scanner(""));

        assertEquals(0, p1.getHp());
        assertEquals(0, p2.getHp());
        return vencedor.getNome();
    }

    private Treinador comConsole(java.util.function.Supplier<Treinador> trecho) {
        Treinador[] resultado = new Treinador[1];
        ConsoleTestes.comConsole(() -> resultado[0] = trecho.get());
        return resultado[0];
    }

    @Test
    void testOrdemDeAtaquePorVelocidade() {
        assertEquals(Arrays.asList("CharSal", "BulbaSal"), atacantesNaOrdem(50, 40));
        assertEquals(Arrays.asList("BulbaSal", "CharSal"), atacantesNaOrdem(40, 50));
    }

    private List<String> atacantesNaOrdem(int spd1, int spd2) {
        RegistroEmMemoria registroDaLuta = new RegistroEmMemoria();
        Batalha arena = new Batalha(c -> Terrenos.NEUTRO, DIA, salvos::add, registroDaLuta, gerador);
        Treinador t1 = new Treinador("J1", lutador("CharSal", new TipoFogo(), 100, spd1));
        Treinador t2 = new Treinador("J2", lutador("BulbaSal", new TipoPlanta(), 100, spd2));

        arena.iniciarCombatePvP(t1, t2, new Scanner(""));

        return registroDaLuta.eventos.stream().filter(e -> e.startsWith("ataque:")).limit(2)
                .map(e -> e.split(":")[1]).collect(Collectors.toList());
    }

    @Test
    void testTerceiroItemNoMenuAvisaDoLimiteEOTurnoContinua() {
        Batalha arena = batalha(Terrenos.NEUTRO);
        Treinador t1 = new Treinador("J1", lutador("CharSal", new TipoFogo(), 100, 50));
        Treinador t2 = new Treinador("J2", lutador("BulbaSal", new TipoPlanta(), 100, 40));
        String teclado = "2\n1\n" + "1\n1\n"
                + "2\n2\n" + "1\n1\n"
                + "2\n" + "1\n1\n" + "1\n1\n";
        Treinador[] vencedor = new Treinador[1];

        String saida = ConsoleTestes.comConsole(() -> vencedor[0] = arena.iniciarCombatePvP(t1, t2, new Scanner(teclado)));

        assertSame(t2, vencedor[0]);
        assertEquals(3, salvos.get(0).getTurnos());
        assertEquals(1, registro.quantos("item:J1:Potion"));
        assertEquals(1, registro.quantos("item:J1:Super Potion"));
        assertTrue(saida.contains("Limite Máximo De 2 itens Atingido Nesta Batalha."));
    }
}
