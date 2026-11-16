package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Matriz de vantagens (regra 2 das Fases 01/02) e efeitos únicos de buff/debuff de cada tipo. Sem mocks: tipos são regra pura. */
class TiposElementaisTest {
    private final TipoPoke fogo = new TipoFogo();
    private final TipoPoke agua = new TipoAgua();
    private final TipoPoke planta = new TipoPlanta();

    @BeforeEach
    void setUp() { Batalha.modoTeste = true; }

    @AfterEach
    void tearDown() { Batalha.modoTeste = false; }

    private static Pokemon pokemon(TipoPoke tipo) {
        return new Pokemon("Alvo", tipo, 50, 40, 100, 30, new Golpe[]{});
    }

    @Test
    void testNomesDosTipos() {
        assertEquals("Fogo", fogo.getNomeTipo());
        assertEquals("Água", agua.getNomeTipo());
        assertEquals("Planta", planta.getNomeTipo());
    }

    @Test
    void testVantagemElemental() {
        assertEquals(2.0, fogo.calcMult(planta));
        assertEquals(0.5, fogo.calcMult(agua));
        assertEquals(2.0, agua.calcMult(fogo));
        assertEquals(0.5, agua.calcMult(planta));
        assertEquals(2.0, planta.calcMult(agua));
        assertEquals(0.5, planta.calcMult(fogo));
    }

    @Test
    void testTipoContraOProprioTipoEhNeutroPoisOsDocumentosNaoDefinemOutraRegra() {
        // Se a equipe decidir que tipo igual é "pouco efetivo" (x0.5), este é o teste a mudar junto com os 3 calcMult.
        assertEquals(1.0, fogo.calcMult(fogo));
        assertEquals(1.0, agua.calcMult(agua));
        assertEquals(1.0, planta.calcMult(planta));
    }

    @Test
    void testBuffDeCadaTipo() {
        Pokemon f = pokemon(fogo);
        fogo.apBf(f);
        assertEquals(60, f.getAtk());   // Fogo: +10 de ataque...
        assertEquals(35, f.getDef());   // ...e -5 de defesa

        Pokemon a = pokemon(agua);
        agua.apBf(a);
        assertEquals(40, a.getSpd());   // Água: +10 de velocidade...
        assertEquals(45, a.getAtk());   // ...e -5 de ataque

        Pokemon p = pokemon(planta);
        planta.apBf(p);
        assertEquals(50, p.getDef());   // Planta: +10 de defesa...
        assertEquals(25, p.getSpd());   // ...e -5 de velocidade
    }

    @Test
    void testDebuffDeCadaTipo() {
        Pokemon f = pokemon(planta);
        fogo.apDb(f);
        assertEquals(35, f.getDef());

        Pokemon a = pokemon(planta);
        agua.apDb(a);
        assertEquals(45, a.getAtk());

        Pokemon p = pokemon(fogo);
        planta.apDb(p);
        assertEquals(25, p.getSpd());
    }

    @Test
    void testBuffEDebuffAvisamNoConsole() {
        Pokemon alvo = pokemon(fogo);
        String saida = ConsoleTestes.comConsole(() -> {
            fogo.apBf(alvo); fogo.apDb(alvo);
            agua.apBf(alvo); agua.apDb(alvo);
            planta.apBf(alvo); planta.apDb(alvo);
        });
        assertTrue(saida.contains("Efeito Único (Fogo): O Ataque de Alvo subiu"));
        assertTrue(saida.contains("Efeito Único (Fogo): A Defesa de Alvo foi reduzida"));
        assertTrue(saida.contains("Efeito Único (Água): A Velocidade de Alvo subiu"));
        assertTrue(saida.contains("Efeito Único (Água): O Ataque de Alvo foi reduzido"));
        assertTrue(saida.contains("Efeito Único (Planta): A Defesa de Alvo subiu"));
        assertTrue(saida.contains("Efeito Único (Planta): A Velocidade de Alvo foi reduzida"));
    }
}
