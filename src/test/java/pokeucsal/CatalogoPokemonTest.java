package pokeucsal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Os 6 iniciais (regra 1 das Fases 01/02) e a garantia de que cada chamada devolve um objeto novo. */
class CatalogoPokemonTest {
    private static final String[] NOMES = { "BulbaSal", "CharSal", "SquirtSal", "ChikoSal", "CyndaSal", "TotoSal" };
    @BeforeEach
    void setUp() { Batalha.modoTeste = true; }

    @AfterEach
    void tearDown() { Batalha.modoTeste = false; }

    private static final String[] TIPOS = { "Planta", "Fogo", "Água", "Planta", "Fogo", "Água" };

    @Test
    void testOsSeisIniciaisTemNomeTipoEQuatroGolpes() {
        for (int numero = 1; numero <= CatalogoPokemon.TOTAL; numero++) {
            Pokemon p = CatalogoPokemon.criar(numero);
            assertEquals(NOMES[numero - 1], p.getNome());
            assertEquals(TIPOS[numero - 1], p.getTipo().getNomeTipo());
            assertEquals(4, p.getGolpes().length);
            assertEquals(150, p.getMaxHp());
        }
    }

    @Test
    void testCadaChamadaDevolveUmPokesalNovo() {
        Pokemon primeiro = CatalogoPokemon.criar(2);
        Pokemon segundo = CatalogoPokemon.criar(2);
        assertTrue(primeiro != segundo);
        primeiro.dano(1000);                                   // ferir um não pode afetar o outro
        assertEquals(150, segundo.getHp());
    }

    @Test
    void testNumeroInexistenteLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> CatalogoPokemon.criar(0));
        assertThrows(IllegalArgumentException.class, () -> CatalogoPokemon.criar(7));
    }
}
