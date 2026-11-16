package pokeucsal;
public class TipoAgua implements TipoPoke {
    @Override public String getNomeTipo() { return "Água"; }
    @Override public double calcMult(TipoPoke d) { return d.getNomeTipo().equals("Fogo") ? 2.0 : d.getNomeTipo().equals("Planta") ? 0.5 : 1.0; }
    @Override public void apBf(Pokemon u) { 
        u.setSpd(u.getSpd() + 10); u.setAtk(u.getAtk() - 5); 
        if(!Batalha.modoTeste) System.out.println("Efeito Único (Água): A Velocidade de " + u.getNome() + " subiu, mas o Ataque caiu!");
    }
    @Override public void apDb(Pokemon a) { 
        a.setAtk(a.getAtk() - 5); 
        if(!Batalha.modoTeste) System.out.println("Efeito Único (Água): O Ataque de " + a.getNome() + " foi reduzido! (Atk atual: " + a.getAtk() + ")");
    }
}
