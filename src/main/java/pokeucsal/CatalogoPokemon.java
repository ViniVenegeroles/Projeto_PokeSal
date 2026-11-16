package pokeucsal;

/**
 * Os 6 Pokésals iniciais (1 BulbaSal, 2 CharSal, 3 SquirtSal, 4 ChikoSal, 5 CyndaSal, 6 TotoSal).
 * Cada chamada de criar() devolve um objeto NOVO: o torneio de 8 treinadores precisa de mais Pokésals do que
 * espécies existem, e o HP, os status e as capturas são do objeto (dois treinadores nunca podem dividir o mesmo).
 */
public final class CatalogoPokemon {
    public static final int TOTAL = 6;

    private CatalogoPokemon() { }

    public static Pokemon criar(int numero) {
        switch (numero) {
            case 1: return new Pokemon("BulbaSal", new TipoPlanta(), 49, 32, 150, 45, golpesPlanta());
            case 2: return new Pokemon("CharSal", new TipoFogo(), 51, 32, 150, 42, golpesFogo());
            case 3: return new Pokemon("SquirtSal", new TipoAgua(), 48, 32, 150, 43, golpesAgua());
            case 4: return new Pokemon("ChikoSal", new TipoPlanta(), 49, 34, 150, 45, golpesPlanta());
            case 5: return new Pokemon("CyndaSal", new TipoFogo(), 52, 31, 150, 41, golpesFogo());
            case 6: return new Pokemon("TotoSal", new TipoAgua(), 49, 32, 150, 45, golpesAgua());
            default: throw new IllegalArgumentException("Pokésal inexistente: " + numero + " (use 1 a " + TOTAL + ").");
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
