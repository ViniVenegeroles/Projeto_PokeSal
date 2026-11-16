package pokeucsal;

/** Avisa as capturas no console. */
public class NotificadorCapturaConsole implements NotificadorCaptura {
    @Override
    public void notificarCaptura(String treinador, String pokesalCapturado) {
        System.out.println(">>> " + pokesalCapturado + " foi capturado e entrou para o time de " + treinador + "! (HP restaurado)");
    }
}
