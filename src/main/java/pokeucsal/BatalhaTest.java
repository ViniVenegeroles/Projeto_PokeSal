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
    public void testEfeitoTerrenoEstacionamentoUCSal() throws NoSuchFieldException, IllegalAccessException {
        Pokemon bulbasal = new Pokemon("BulbaSal", new TipoPlanta(), 100, 30, 100, 40, new Golpe[0]);
        Pokemon dummy = new Pokemon("Inimigo", new TipoFogo(), 100, 30, 100, 40, new Golpe[0]);
        bulbasal.dano(50);
        int hpAposDano = bulbasal.getHp();
        Batalha arena = new Batalha();
        java.lang.reflect.Field campoClima = Batalha.class.getDeclaredField("climaAtual");
        campoClima.setAccessible(true);
        campoClima.set(arena, "Canteiro Central");
        arena.processarFimDeTurno(bulbasal, dummy);
        assertEquals(hpAposDano + 5, bulbasal.getHp());
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
        assertEquals(100, rapido.getHp());
        assertEquals(0, lento.getHp());
    }
    @Test
    public void testUsoLimiteDeItensExcedido() {
        Bag bag = new Bag();
        bag.resetarUsoBatalha();
        bag.usarItem();
        bag.usarItem();
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            bag.usarItem();
        });
        assertEquals("Limite Máximo De 2 itens Atingido Nesta Batalha.", exception.getMessage());
    }
    @Test
    public void testCalculoDanoBoundaryValues() {
        Pokemon defensor = new Pokemon("Defensor", new TipoPlanta(), 50, 1000, 50, 40, new Golpe[0]);
        defensor.dano(10);
        assertEquals(49, defensor.getHp());
        defensor.dano(9999);
        assertEquals(27, defensor.getHp());
        defensor.dano(9999);
        defensor.dano(9999);
        assertEquals(0, defensor.getHp());
        defensor.curar(0.99);
        defensor.curar(0.99);
        assertEquals(50, defensor.getHp());
    }
    @Test
    public void testEfeitosDosStatusNegativos() {
        Pokemon alvo = new Pokemon("Alvo", new TipoFogo(), 50, 30, 100, 40, new Golpe[0]);
        alvo.setParalisado(true);
        assertEquals(30, alvo.getSpd());
        alvo.setQueimado(true);
        alvo.processarStatusFimDeTurno();
        assertEquals(94, alvo.getHp());
        alvo.curarStatus();
        alvo.setEnvenenado(true);
        alvo.processarStatusFimDeTurno();
        assertEquals(88, alvo.getHp());
        alvo.processarStatusFimDeTurno();
        assertEquals(76, alvo.getHp());
    }
    @Test
    public void testCuraStatusAntidoto() {
        Pokemon p = new Pokemon("Teste", new TipoFogo(), 50, 30, 40, 40, new Golpe[0]);
        p.setQueimado(true);
        p.setEnvenenado(true);
        p.setParalisado(true);
        assertTrue(p.isQueimado());
        assertTrue(p.isEnvenenado());
        assertTrue(p.isParalisado());
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
    @Test
    @SuppressWarnings("java:S3011")
    public void testMudarTerrenoSalShard() throws NoSuchFieldException, IllegalAccessException {
        Batalha arena = new Batalha();
        java.lang.reflect.Field campoClima = Batalha.class.getDeclaredField("climaAtual");
        campoClima.setAccessible(true);
        for (int i = 0; i < 20; i++) {
            String climaAntes = (String) campoClima.get(arena);
            arena.mudarTerreno();
            String climaApos = (String) campoClima.get(arena);
            assertNotEquals(climaAntes, climaApos);
            assertTrue(climaApos.equals("Asfalto Quente") ||
                    climaApos.equals("Piso Escorregadio") ||
                    climaApos.equals("Canteiro Central"));
        }
    }
    @Test
    public void testLimitesDePrecisao() {
        Pokemon p = new Pokemon("Alvo", new TipoFogo(), 50, 50, 100, 50, new Golpe[0]);
        assertEquals(100, p.getPrecisao());
        p.diminuirPrecisao(40);
        assertEquals(60, p.getPrecisao());
        p.diminuirPrecisao(50);
        assertEquals(30, p.getPrecisao());
    }
    @Test
    public void testMitigacaoDeDanoPorDefesa() {
        Pokemon p = new Pokemon("Tanque", new TipoPlanta(), 50, 40, 100, 50, new Golpe[0]);
        p.dano(30);
        assertEquals(80, p.getHp());
    }
    @Test
    public void testRestauracaoTotalDeAtributosNaCuraMaxima() {
        Pokemon p = new Pokemon("Lutador", new TipoFogo(), 60, 60, 100, 60, new Golpe[0]);
        p.setAtk(20);
        p.setDef(20);
        p.setSpd(20);
        p.diminuirPrecisao(50);
        p.dano(40);
        assertEquals(20, p.getAtk());
        assertEquals(20, p.getDef());
        assertEquals(20, p.getSpd());
        assertEquals(50, p.getPrecisao());
        assertEquals(65, p.getHp());
        p.curar(1.0);
        assertEquals(60, p.getAtk());
        assertEquals(60, p.getDef());
        assertEquals(60, p.getSpd());
        assertEquals(100, p.getPrecisao());
        assertEquals(100, p.getHp());
    }
}