package pokeucsal;

final class GeradorFixo implements GeradorAleatorio {
    private final int precisao;
    private final double chance;

    GeradorFixo(int precisao, double chance) {
        this.precisao = precisao;
        this.chance = chance;
    }

    @Override public int sortearPrecisao() {
        return precisao;
    }
    @Override public double sortearChance() {
        return chance;
    }
}
