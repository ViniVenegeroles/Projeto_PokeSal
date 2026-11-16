package pokeucsal;

/** Tipo Água: vence Fogo (x2.0) e perde para Planta (x0.5). */
public class TipoAgua implements TipoPoke {
    @Override public String getNomeTipo() {
        return "Água";
    }
    @Override public double calcMult(TipoPoke d) {
        return d.getNomeTipo().equals("Fogo") ? 2.0 : d.getNomeTipo().equals("Planta") ? 0.5 : 1.0;
    }
    @Override public void apBf(Pokesal u) {
        u.setSpd(u.getSpd() + 10);
        u.setAtk(u.getAtk() - 5);
        if(!Batalha.modoTeste) {
            System.out.println("Efeito Único (Água): A Velocidade de " + u.getNome() + " subiu, mas o Ataque caiu!");
        }
    }
    @Override public void apDb(Pokesal a) {
        a.setAtk(a.getAtk() - 5);
        if(!Batalha.modoTeste) {
            System.out.println("Efeito Único (Água): O Ataque de " + a.getNome() + " foi reduzido! (Atk atual: " + a.getAtk() + ")");
        }
    }
}
