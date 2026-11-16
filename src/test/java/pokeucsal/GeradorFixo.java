package pokeucsal;

/**
 * GeradorAleatorio de mentira que devolve sempre os mesmos valores.
 * precisao: o golpe erra se precisao >= precisão do atacante (0 = sempre acerta, 100 = sempre erra).
 * chance: crítico se <= 0.0625; status se < 0.125; escolhe o índice (int)(chance * tamanho) em sorteios de lista.
 */
final class GeradorFixo implements GeradorAleatorio {
    private final int precisao;
    private final double chance;

    GeradorFixo(int precisao, double chance) { this.precisao = precisao; this.chance = chance; }

    @Override public int sortearPrecisao() { return precisao; }
    @Override public double sortearChance() { return chance; }
}
