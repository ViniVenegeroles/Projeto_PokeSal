package pokeucsal;

/**
 * Catálogo dos seis Pokésals iniciais. Cada chamada cria um objeto novo, pois HP, status e capturas pertencem ao objeto.
 */
public final class CatalogoPokesal {
    public static final int TOTAL = 6;

    private CatalogoPokesal() { }

    /**
     * Cria um Pokésal inicial: 1 BulbaSal, 2 CharSal, 3 SquirtSal, 4 ChikoSal, 5 CyndaSal, 6 TotoSal.
     *
     * @param numero número do Pokésal, de 1 a 6
     * @return um Pokésal novo
     * @throws IllegalArgumentException se o número não existir
     */
    public static Pokesal criar(int numero) {
        switch (numero) {
            case 1:
                return new Pokesal("BulbaSal", new TipoPlanta(), 49, 32, 150, 45, golpesPlanta());
            case 2:
                return new Pokesal("CharSal", new TipoFogo(), 51, 32, 150, 42, golpesFogo());
            case 3:
                return new Pokesal("SquirtSal", new TipoAgua(), 48, 32, 150, 43, golpesAgua());
            case 4:
                return new Pokesal("ChikoSal", new TipoPlanta(), 49, 34, 150, 45, golpesPlanta());
            case 5:
                return new Pokesal("CyndaSal", new TipoFogo(), 52, 31, 150, 41, golpesFogo());
            case 6:
                return new Pokesal("TotoSal", new TipoAgua(), 49, 32, 150, 45, golpesAgua());
            default:
                throw new IllegalArgumentException("Pokésal inexistente: " + numero + " (use 1 a " + TOTAL + ").");
        }
    }

    private static Golpe[] golpesPlanta() {
        return new Golpe[] {
            new Golpe.Normal("Chicote", 25), new Golpe.Elemental("Cansanção", 35),
            new Golpe.Buff("Fotossíntese"), new Golpe.Debuff("Tempestade de Folhas", 40)
        };
    }

    private static Golpe[] golpesFogo() {
        return new Golpe[] {
            new Golpe.Normal("Arranhão", 25), new Golpe.Elemental("Lança-Chamas", 35),
            new Golpe.Buff("Dança das Chamas"), new Golpe.Debuff("Superaquecer", 40)
        };
    }

    private static Golpe[] golpesAgua() {
        return new Golpe[] {
            new Golpe.Normal("Impacto Repentino", 25), new Golpe.Elemental("Jato de Água", 35),
            new Golpe.Buff("Dança da Chuva"), new Golpe.Debuff("Gêiser Submerso", 40)
        };
    }
}
