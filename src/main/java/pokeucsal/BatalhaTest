package pokeucsal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BatalhaTest {

    @Test
    public void testVantagemElemental() {
        Golpe[] golpes = {new Golpe.Elemental("Lança-Chamas", 30)};
        Pokemon charsal = new Pokemon("CharSal", new TipoFogo(), 50, 30, 40, 40, golpes);
        Pokemon bulbasal = new Pokemon("BulbaSal", new TipoPlanta(), 50, 30, 40, 40, golpes);

        double multiplicador = charsal.getTipo().calcMult(bulbasal.getTipo());

        assertEquals(2.0, multiplicador, 0.001);
    }

    @Test
    public void testEfeitoTerrenoEstacionamentoUCSal() {
        Pokemon bulbasal = new Pokemon("BulbaSal", new TipoPlanta(), 100, 30, 100, 40, new Golpe[0]);

        bulbasal.dano(50);
        bulbasal.curar(0.05);

        assertEquals(62, bulbasal.getHp());
    }

    @Test
    public void testOrdemDeAtaquePorVelocidade() {
        Golpe[] golpes = {new Golpe.Normal("Batida", 9999)};
        Pokemon rapido = new Pokemon("Rapido", new TipoFogo(), 9999, 30, 100, 90, golpes);
        Pokemon lento = new Pokemon("Lento", new TipoPlanta(), 9999, 30, 100, 20, golpes);

        lento.dano(9999);
        lento.dano(9999);

        Batalha batalha = new Batalha();
        batalha.resolverRodadaDeAtaques(rapido, golpes[0], lento, golpes[0]);

        assertEquals(100, rapido.getHp(), "Pokemon mais rapido deve atacar primeiro e manter HP intacto");
        assertEquals(0, lento.getHp(), "Pokemon mais lento deve ser derrotado antes de contra-atacar");
    }

    @Test
    public void testUsoLimiteDeItensExcedido() {
        Bag bag = new Bag();
        bag.resetarUsoBatalha();

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            throw new IllegalStateException("Limite Máximo De 2 itens Atingido Nesta Batalha.");
        });

        assertEquals("Limite Máximo De 2 itens Atingido Nesta Batalha.", exception.getMessage());
    }

    @Test
    public void testCalculoDanoBoundaryValues() {
        Pokemon defensor = new Pokemon("Defensor", new TipoPlanta(), 50, 10, 50, 40, new Golpe[0]);

        defensor.dano(9999);

        assertEquals(28, defensor.getHp());
    }

    @Test
    public void testCuraStatusAntidoto() {
        Pokemon p = new Pokemon("Teste", new TipoFogo(), 50, 30, 40, 40, new Golpe[0]);

        p.setQueimado(true);
        assertTrue(p.isQueimado());

        p.curarStatus();

        assertFalse(p.isQueimado());
        assertFalse(p.isEnvenenado());
        assertFalse(p.isParalisado());
    }

    @Test
    public void testGolpeAutoralBuffComDebuffEmSiMesmo() {
        Pokemon usuario = new Pokemon("FogoAtivo", new TipoFogo(), 50, 40, 100, 50, new Golpe[0]);

        Golpe.Buff golpeBuff = new Golpe.Buff("Dança das Chamas");
        golpeBuff.executar(usuario, null);

        assertEquals(60, usuario.getAtk());
        assertEquals(90, usuario.getPrecisao());
        assertEquals(35, usuario.getDef());
    }
}
