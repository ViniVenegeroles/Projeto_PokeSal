package pokeucsal;

/** Implementação usada no jogo: anuncia a captura no console. */
public class NotificadorCapturaConsole implements NotificadorCaptura {
    @Override
    public void notificarCaptura(String treinador, String pokemonCapturado) {
        System.out.println(">>> " + pokemonCapturado + " foi capturado e entrou para o time de " + treinador + "! (HP restaurado)");
    }
}
