package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TiposElementaisTest {
    private final TipoPoke fogo = new TipoFogo();
    private final TipoPoke agua = new TipoAgua();
    private final TipoPoke planta = new TipoPlanta();

    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
    }

    @AfterEach
    void tearDown() {
        Batalha.modoTeste = false;
    }

    private static Pokesal pokesal(TipoPoke tipo) {
        return new Pokesal("Alvo", tipo, 50, 40, 100, 30, new Golpe[]{});
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
        assertEquals(1.0, fogo.calcMult(fogo));
        assertEquals(1.0, agua.calcMult(agua));
        assertEquals(1.0, planta.calcMult(planta));
    }

    @Test
    void testBuffDeCadaTipo() {
        Pokesal f = pokesal(fogo);
        fogo.apBf(f);
        assertEquals(60, f.getAtk());
        assertEquals(35, f.getDef());

        Pokesal a = pokesal(agua);
        agua.apBf(a);
        assertEquals(40, a.getSpd());
        assertEquals(45, a.getAtk());

        Pokesal p = pokesal(planta);
        planta.apBf(p);
        assertEquals(50, p.getDef());
        assertEquals(25, p.getSpd());
    }

    @Test
    void testDebuffDeCadaTipo() {
        Pokesal f = pokesal(planta);
        fogo.apDb(f);
        assertEquals(35, f.getDef());

        Pokesal a = pokesal(planta);
        agua.apDb(a);
        assertEquals(45, a.getAtk());

        Pokesal p = pokesal(fogo);
        planta.apDb(p);
        assertEquals(25, p.getSpd());
    }

    @Test
    void testBuffEDebuffAvisamNoConsole() {
        Pokesal alvo = pokesal(fogo);
        String saida = ConsoleTestes.comConsole(() -> {
            fogo.apBf(alvo);
            fogo.apDb(alvo);
            agua.apBf(alvo);
            agua.apDb(alvo);
            planta.apBf(alvo);
            planta.apDb(alvo);
        });
        assertTrue(saida.contains("Efeito Único (Fogo): O Ataque de Alvo subiu"));
        assertTrue(saida.contains("Efeito Único (Fogo): A Defesa de Alvo foi reduzida"));
        assertTrue(saida.contains("Efeito Único (Água): A Velocidade de Alvo subiu"));
        assertTrue(saida.contains("Efeito Único (Água): O Ataque de Alvo foi reduzido"));
        assertTrue(saida.contains("Efeito Único (Planta): A Defesa de Alvo subiu"));
        assertTrue(saida.contains("Efeito Único (Planta): A Velocidade de Alvo foi reduzida"));
    }
}
