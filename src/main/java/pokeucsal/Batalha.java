package pokeucsal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Batalha {
    public static final int PAUSA_CURTA_MS = 1500;
    public static final int PAUSA_LONGA_MS = 2000;
    public static boolean modoTeste = false;
    /** Desliga só as esperas (Thread.sleep), mantendo a saída de console: usado por testes que exercitam os menus sem demorar. */
    public static boolean semPausas = false;

    private static final Logger LOG = Logger.getLogger(Batalha.class.getName());
    private static final double CURA_CANTEIRO = 0.05;    // Canteiro Central cura 5% dos Pokésals de Planta

    private final ServicoTerreno servicoTerreno;
    private final Clock clock;
    private final RepositorioResultados repositorio;
    private final RegistroBatalha registro;
    private final GeradorAleatorio gerador;
    
    private String climaAtual;
    private int turnosAtuais;

    public Batalha(ServicoTerreno st, Clock ck, RepositorioResultados rep, RegistroBatalha reg, GeradorAleatorio ger) {
        this.servicoTerreno = st; this.clock = ck; this.repositorio = rep; this.registro = reg;
        this.gerador = ger;
        this.climaAtual = consultarTerreno();
    }
    public String getClimaAtual() { return climaAtual; }

    /**
     * O terreno vem de um serviço externo, então pode falhar ou devolver null.
     * Antes isso causava NullPointerException em aplicarCorClima/processarFimDeTurno
     * (.equals em null). Agora cai num terreno neutro e a batalha segue.
     */
    private String consultarTerreno() {
        try {
            String terreno = servicoTerreno.obterTerrenoAtual(clock);
            return (terreno == null || terreno.isEmpty()) ? Terrenos.NEUTRO : terreno;
        } catch (RuntimeException e) {
            LOG.warning("Falha ao consultar o serviço de terreno: " + e.getMessage());
            return Terrenos.NEUTRO;
        }
    }

    public static void pausar(int ms) {
        if (modoTeste || semPausas) return;
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void aplicarCorClima() {
        if (modoTeste) return;
        switch (this.climaAtual) {
            case Terrenos.ASFALTO_QUENTE: System.out.print("\u001B[31m"); break; 
            case Terrenos.PISO_ESCORREGADIO: System.out.print("\u001B[34m"); break; 
            case Terrenos.CANTEIRO_CENTRAL: System.out.print("\u001B[32m"); break; 
            default: break;
        }
    }

    /** Sorteia um índice válido (0..tamanho-1) usando o gerador injetado, mesmo que ele devolva 1.0. */
    private static int sortearIndice(GeradorAleatorio ger, int tamanho) {
        int indice = (int) (ger.sortearChance() * tamanho);
        return Math.max(0, Math.min(indice, tamanho - 1));
    }

    public void mudarTerrenoSalShard(RegistroBatalha registro, GeradorAleatorio gerador) {
        // Regras do Sal Shard: (1) tem que MUDAR o terreno (antes podia sortear o mesmo e o item era gasto à toa);
        // (2) de noite não pode criar Asfalto Quente (regra 2.2: só existe de dia).
        List<String> opcoes = new ArrayList<>(Terrenos.disponiveis(Terrenos.ehDia(clock)));
        opcoes.remove(this.climaAtual);
        this.climaAtual = opcoes.get(sortearIndice(gerador, opcoes.size()));
        aplicarCorClima();
        if(!modoTeste) System.out.println("\nA Sal Shard brilhou! O Campo de Batalha Agora é O " + this.climaAtual + "!");
        registro.registrarEfeitoTerreno("Campo", "Sal Shard ativou: " + climaAtual);
    }

    private void curarPeloTerreno(Pokemon p) {
        if (p.getTipo().getNomeTipo().equals("Planta") && p.getHp() > 0) {
            p.curar(CURA_CANTEIRO);
            if(!modoTeste) System.out.println("[TERRENO] O Canteiro Central Restaurou a Vida de " + p.getNome() + "!");
            registro.registrarEfeitoTerreno(p.getNome(), "Cura Terreno");
        }
    }

    public void processarFimDeTurno(Pokemon p1, Pokemon p2) {
        if(!modoTeste) System.out.println("\n--- Fim de Turno ---");
        // Ordem da regra 2.4: efeito de STATUS e depois efeito de TERRENO
        // (antes o terreno era processado e registrado antes dos status).
        if (p1.getHp() > 0) p1.processarStatusFimDeTurno(registro);
        if (p2.getHp() > 0) p2.processarStatusFimDeTurno(registro);
        if (Terrenos.CANTEIRO_CENTRAL.equals(climaAtual)) {
            curarPeloTerreno(p1);
            curarPeloTerreno(p2);
        }
    }

    private void turnoIndividual(Treinador atacante, Treinador alvo, Golpe golpe) {
        if(!modoTeste) System.out.println("\n--- Turno de " + atacante.getPokemon().getNome() + " ---");
        pausar(PAUSA_CURTA_MS);
        golpe.executar(atacante.getPokemon(), alvo.getPokemon(), climaAtual, gerador, registro);
    }

    /**
     * Resolve UM turno completo. g1/g2 podem ser null (o treinador usou um item em vez de atacar).
     *
     * Por quê aceitar null aqui: antes só o caminho "os dois atacam" contava o turno. Os caminhos "um atacou e o outro
     * usou item" (PvP) e "usei item" (PvE) duplicavam parte da lógica e esqueciam o resto: turnos eram
     * contados a menos no resultado salvo.
     */
    public void resolverRodadaDeAtaques(Treinador t1, Golpe g1, Treinador t2, Golpe g2) {
        Pokemon p1 = t1.getPokemon(); Pokemon p2 = t2.getPokemon();
        turnosAtuais++;

        if (g1 != null && g2 != null) {
            Treinador primeiro = (p1.getSpd() >= p2.getSpd()) ? t1 : t2;
            Treinador segundo = (primeiro == t1) ? t2 : t1;
            Golpe gP = (primeiro == t1) ? g1 : g2;
            Golpe gS = (primeiro == t1) ? g2 : g1;

            pausar(PAUSA_CURTA_MS);
            if(!modoTeste) {
                System.out.println("\n--- Ordem de Ataque ---");
                System.out.println(primeiro.getPokemon().getNome() + " é Mais Rápido e Toma a Frente!");
            }
            pausar(PAUSA_CURTA_MS);

            gP.executar(primeiro.getPokemon(), segundo.getPokemon(), climaAtual, gerador, registro);
            
            if (segundo.getPokemon().getHp() > 0) {
                if(!modoTeste) System.out.println("\nContra-ataque de " + segundo.getPokemon().getNome() + "!");
                pausar(PAUSA_CURTA_MS);
                gS.executar(segundo.getPokemon(), primeiro.getPokemon(), climaAtual, gerador, registro);
            } else {
                if(!modoTeste) System.out.println("\n" + segundo.getPokemon().getNome() + " Desmaiou Antes de Conseguir Atacar!");
                pausar(PAUSA_CURTA_MS);
            }
        } else if (g1 != null) {
            turnoIndividual(t1, t2, g1);
        } else if (g2 != null) {
            turnoIndividual(t2, t1, g2);
        }
        processarFimDeTurno(p1, p2);

    }

    /** PvE: o inimigo é sempre controlado pelo computador. Retorna true se o jogador venceu. */
    public boolean iniciarCombate(Treinador jogador, Treinador inimigo, Scanner sc) {
        prepararBatalha(jogador, inimigo);
        Pokemon pI = inimigo.getPokemon();
        
        if(!modoTeste) System.out.println("\nUm " + pI.getNome() + " Adversário se Aproxima!");
        pausar(PAUSA_CURTA_MS);
        pI.curar(1.0); pI.exibirSts();
        pausar(PAUSA_CURTA_MS);

        anunciarTerreno();
        pausar(PAUSA_LONGA_MS);

        Treinador vencedor = executarCombate(jogador, inimigo, sc, true);
        if(!modoTeste) {
            if(vencedor == jogador) System.out.println("Você Venceu a Batalha!");
            else System.out.println("Seu PokeSal Desmaiou...");
            System.out.print("\u001B[0m");
        }
        return vencedor == jogador;
    }

    /** PvP: cada treinador escolhe pelo teclado, a não ser que ele seja marcado como CPU (torneio). */
    public Treinador iniciarCombatePvP(Treinador t1, Treinador t2, Scanner sc) {
        prepararBatalha(t1, t2);
        Pokemon p1 = t1.getPokemon(); Pokemon p2 = t2.getPokemon();
        
        if(!modoTeste) System.out.println("\nBatalha PvP iniciada entre " + p1.getNome() + " (" + t1.getNome() + ") e " + p2.getNome() + " (" + t2.getNome() + ")!");
        pausar(PAUSA_LONGA_MS);
        anunciarTerreno();

        Treinador vencedor = executarCombate(t1, t2, sc, false);
        if(!modoTeste) {
            System.out.println(vencedor.getNome() + " Venceu!");
            System.out.print("\u001B[0m"); 
        }
        return vencedor;
    }

    private void prepararBatalha(Treinador t1, Treinador t2) {
        t1.getBag().resetarUsoBatalha(); t2.getBag().resetarUsoBatalha();
        turnosAtuais = 0;
        this.climaAtual = consultarTerreno();
    }

    private void anunciarTerreno() {
        aplicarCorClima();
        if(!modoTeste) System.out.println("Condição do Ambiente de Combate: " + climaAtual + "!");
    }

    /**
     * Laço de turnos ÚNICO para PvE e PvP (antes eram dois laços duplicados com comportamentos diferentes).
     * Quando termina, salva o resultado UMA vez e devolve o vencedor.
     */
    private Treinador executarCombate(Treinador t1, Treinador t2, Scanner sc, boolean t2Automatico) {
        Pokemon p1 = t1.getPokemon(); Pokemon p2 = t2.getPokemon();

        while (p1.getHp() > 0 && p2.getHp() > 0) {
            Golpe g1 = escolherAcao(t1, sc, false);
            Golpe g2 = escolherAcao(t2, sc, t2Automatico);
            resolverRodadaDeAtaques(t1, g1, t2, g2);
        }

        // Empate duplo (os dois caem no mesmo fim de turno por queimadura/veneno): antes o t2 sempre "vencia".
        // Agora vale o mesmo critério de iniciativa do turno: maior velocidade (empate de velocidade favorece o t1).
        boolean t1Venceu;
        if (p1.getHp() > 0) t1Venceu = true;
        else if (p2.getHp() > 0) t1Venceu = false;
        else t1Venceu = p1.getSpd() >= p2.getSpd();

        Treinador vencedor = t1Venceu ? t1 : t2;
        Treinador perdedor = t1Venceu ? t2 : t1;
        salvarResultado(vencedor, perdedor);
        return vencedor;
    }

    private void salvarResultado(Treinador vencedor, Treinador perdedor) {
        ResultadoBatalha resultado = new ResultadoBatalha(vencedor.getNome(), perdedor.getNome(), turnosAtuais,
                vencedor.getPokemon().getNome(), perdedor.getPokemon().getNome());
        try { repositorio.salvarResultado(resultado); }
        // Regra 2.3: se o repositório falhar, a batalha continua válida; o erro é REGISTRADO (antes o catch era vazio).
        catch (Exception e) { LOG.warning("Falha ao salvar o resultado da batalha (" + resultado + "): " + e.getMessage()); }
    }

    private Golpe golpeAleatorio(Pokemon p) {
        return p.getGolpes()[sortearIndice(gerador, p.getGolpes().length)];
    }

    private Golpe escolherAcao(Treinador t, Scanner sc, boolean automatico) {
        Pokemon p = t.getPokemon();
        // Computador (ou CPU do torneio) sorteia o golpe pelo gerador injetado e nunca lê o teclado.
        if (automatico || t.isCpu()) return golpeAleatorio(p);
        if (modoTeste) return p.getGolpes()[0]; 

        while (true) {
            System.out.println("\n--- Turno de " + t.getNome() + " (" + p.getNome() + ") ---");
            System.out.println("HP: " + p.getHp() + "/" + p.getMaxHp());
            System.out.println("1 - Atacar\n2 - Mochila");
            if (!sc.hasNextInt()) { sc.next(); continue; }
            int acao = sc.nextInt();
            if (acao == 1) {
                System.out.println("\nEscolha um Golpe:");
                for (int i=0; i<p.getGolpes().length; i++) { System.out.println((i+1) + " - " + p.getGolpes()[i].getNome()); }
                if (!sc.hasNextInt()) { sc.next(); continue; }
                int esc = sc.nextInt() - 1;
                if (esc >= 0 && esc < p.getGolpes().length) return p.getGolpes()[esc];
                else System.out.println("Opção Inválida!");
            } else if (acao == 2) {
                if (t.getBag().abrirMochila(p, this, registro, t, sc, gerador)) return null;
            }
        }
    }
}
