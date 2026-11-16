package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogoPokesalTest {
    private static final String[] NOMES = { "BulbaSal", "CharSal", "SquirtSal", "ChikoSal", "CyndaSal", "TotoSal" };
    @BeforeEach
    void setUp() {
        Batalha.modoTeste = true;
    }

    @AfterEach
    void tearDown() {
        Batalha.modoTeste = false;
    }

    private static final String[] TIPOS = { "Planta", "Fogo", "Água", "Planta", "Fogo", "Água" };

    @Test
    void testOsSeisIniciaisTemNomeTipoEQuatroGolpes() {
        for (int numero = 1; numero <= CatalogoPokesal.TOTAL; numero++) {
            Pokesal p = CatalogoPokesal.criar(numero);
            assertEquals(NOMES[numero - 1], p.getNome());
            assertEquals(TIPOS[numero - 1], p.getTipo().getNomeTipo());
            assertEquals(4, p.getGolpes().length);
            assertEquals(150, p.getMaxHp());
        }
    }

    @Test
    void testCadaChamadaDevolveUmPokesalNovo() {
        Pokesal primeiro = CatalogoPokesal.criar(2);
        Pokesal segundo = CatalogoPokesal.criar(2);
        assertTrue(primeiro != segundo);
        primeiro.dano(1000);
        assertEquals(150, segundo.getHp());
    }

    @Test
    void testNumeroInexistenteLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> CatalogoPokesal.criar(0));
        assertThrows(IllegalArgumentException.class, () -> CatalogoPokesal.criar(7));
    }
}
