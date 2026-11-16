package pokeucsal;
public class TipoPlanta implements TipoPoke {
    @Override public String getNomeTipo() { return "Planta"; }
    @Override public double calcMult(TipoPoke d) { return d.getNomeTipo().equals("Água") ? 2.0 : d.getNomeTipo().equals("Fogo") ? 0.5 : 1.0; }
    @Override public void apBf(Pokemon u) { 
        u.setDef(u.getDef() + 10); u.setSpd(u.getSpd() - 5); 
        if(!Batalha.modoTeste) System.out.println("Efeito Único (Planta): A Defesa de " + u.getNome() + " subiu, mas a Velocidade caiu!");
    }
    @Override public void apDb(Pokemon a) { 
        a.setSpd(a.getSpd() - 5); 
        if(!Batalha.modoTeste) System.out.println("Efeito Único (Planta): A Velocidade de " + a.getNome() + " foi reduzida! (Spd atual: " + a.getSpd() + ")");
    }
}
