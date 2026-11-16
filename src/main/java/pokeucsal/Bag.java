package pokeucsal;
import java.util.Scanner;
public class Bag {
    private int itensUsd = 0;
    private int potion = 1;
    private int superPotion = 1;
    private int antidoto = 1;
    private int salShard = 1;

    public void resetarUsoBatalha() { this.itensUsd = 0; potion = 1; superPotion = 1; antidoto = 1; salShard = 1; }

    public boolean abrirMochila(Pokemon aliado, Batalha arena, RegistroBatalha registro, Treinador treinador, Scanner sc, GeradorAleatorio gerador) {
        if (itensUsd >= 2) {
            if(!Batalha.modoTeste) System.out.println("\nLimite Máximo De 2 itens Atingido Nesta Batalha.");
            Batalha.pausar(Batalha.PAUSA_CURTA_MS);
            return false;
        }
        if(!Batalha.modoTeste) {
            System.out.println("\n--- MOCHILA (Itens Usados: " + itensUsd + "/2) ---");
            System.out.println("1 - Potion (Cura 15% de HP) [Restam: " + potion + "]");
            System.out.println("2 - Super Potion (Cura 35% de HP) [Restam: " + superPotion + "]");
            System.out.println("3 - Antídoto (Cura Status) [Restam: " + antidoto + "]");
            System.out.println("4 - Sal Shard (Muda Terreno) [Restam: " + salShard + "]");
            System.out.println("0 - Voltar");
        }
        
        if (!sc.hasNextInt()) { sc.next(); return false; }
        int escolha = sc.nextInt();
        
        switch(escolha) {
            case 1: if(potion > 0) { potion--; aliado.curar(0.15); if(!Batalha.modoTeste) System.out.println("\n>>> " + aliado.getNome() + " Usou Potion e Curou 15%!"); registro.registrarUsoItem(treinador.getNome(), "Potion"); } else return false; break;
            case 2: if(superPotion > 0) { superPotion--; aliado.curar(0.35); if(!Batalha.modoTeste) System.out.println("\n>>> " + aliado.getNome() + " Usou Super Potion e Curou 35%!"); registro.registrarUsoItem(treinador.getNome(), "Super Potion"); } else return false; break;
            case 3: if(antidoto > 0) { antidoto--; aliado.curarStatus(); if(!Batalha.modoTeste) System.out.println("\n>>> " + aliado.getNome() + " Usou Antídoto e Limpou os Status!"); registro.registrarUsoItem(treinador.getNome(), "Antídoto"); } else return false; break;
            case 4: if(salShard > 0) { salShard--; arena.mudarTerrenoSalShard(registro, gerador); registro.registrarUsoItem(treinador.getNome(), "Sal Shard"); } else return false; break;
            case 0: return false;
            default: return false;
        }
        itensUsd++; Batalha.pausar(Batalha.PAUSA_CURTA_MS); return true;
    }
}
