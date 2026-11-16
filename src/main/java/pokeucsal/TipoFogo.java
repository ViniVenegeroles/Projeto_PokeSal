package pokeucsal;
public class TipoFogo implements TipoPoke {
    @Override public String getNomeTipo() { return "Fogo"; }
    @Override public double calcMult(TipoPoke d) { return d.getNomeTipo().equals("Planta") ? 2.0 : d.getNomeTipo().equals("Água") ? 0.5 : 1.0; }
    @Override public void apBf(Pokemon u) { 
        u.setAtk(u.getAtk() + 10); u.setDef(u.getDef() - 5); 
        if(!Batalha.modoTeste) System.out.println("Efeito Único (Fogo): O Ataque de " + u.getNome() + " subiu, mas a Defesa caiu!");
    }
    @Override public void apDb(Pokemon a) { 
        a.setDef(a.getDef() - 5); 
        if(!Batalha.modoTeste) System.out.println("Efeito Único (Fogo): A Defesa de " + a.getNome() + " foi reduzida! (Def atual: " + a.getDef() + ")");
    }
}
