package pokeucsal;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Batalha entre dois treinadores (contra o computador ou entre jogadores), com iniciativa por velocidade, efeitos de terreno e status no fim do turno.
 */
public class Batalha {
    public static final int PAUSA_CURTA_MS = 1500;
    public static final int PAUSA_LONGA_MS = 2000;
    public static boolean modoTeste = false;

    public static boolean semPausas = false;

    private static final Logger LOG = Logger.getLogger(Batalha.class.getName());
    private static final double CURA_CANTEIRO = 0.05;

    private final ServicoTerreno servicoTerreno;
    private final Clock clock;
    private final RepositorioResultados repositorio;
    private final RegistroBatalha registro;
    private final GeradorAleatorio gerador;

    private String climaAtual;
    private int turnosAtuais;

    /**
     * Cria uma batalha. Todos os colaboradores chegam pelo construtor.
     *
     * @param st serviço que informa o terreno
     * @param ck relógio usado para decidir se é dia ou noite
     * @param rep repositório onde o resultado da batalha é salvo
     * @param reg registro dos eventos de cada turno
     * @param ger gerador de aleatoriedade (precisão, crítico e sorteios)
     */
    public Batalha(ServicoTerreno st, Clock ck, RepositorioResultados rep, RegistroBatalha reg, GeradorAleatorio ger) {
        this.servicoTerreno = st;
        this.clock = ck;
        this.repositorio = rep;
        this.registro = reg;
        this.gerador = ger;
        this.climaAtual = consultarTerreno();
    }
    /** Devolve o nome do terreno atual da batalha. */
    public String getClimaAtual() {
        return climaAtual;
    }

    private String consultarTerreno() {
        try {
            String terreno = servicoTerreno.obterTerrenoAtual(clock);
            return (terreno == null || terreno.isEmpty()) ? Terrenos.NEUTRO : terreno;
        } catch (RuntimeException e) {
            LOG.warning("Falha ao consultar o serviço de terreno: " + e.getMessage());
            return Terrenos.NEUTRO;
        }
    }

    /**
     * Espera um tempo para dar ritmo à leitura. Não espera em modo de teste nem com {@code semPausas} ligado.
     *
     * @param ms tempo de espera em milissegundos
     */
    public static void pausar(int ms) {
        if (modoTeste || semPausas) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void aplicarCorClima() {
        if (modoTeste) {
            return;
        }
        switch (this.climaAtual) {
            case Terrenos.ASFALTO_QUENTE:
                System.out.print("\u001B[31m");
                break;
            case Terrenos.PISO_ESCORREGADIO:
                System.out.print("\u001B[34m");
                break;
            case Terrenos.CANTEIRO_CENTRAL:
                System.out.print("\u001B[32m");
                break;
            default:
                break;
        }
    }

    private static int sortearIndice(GeradorAleatorio ger, int tamanho) {
        int indice = (int) (ger.sortearChance() * tamanho);
        return Math.max(0, Math.min(indice, tamanho - 1));
    }

    /**
     * Efeito da Sal Shard: troca o terreno por outro, sorteado entre os três terrenos, sempre diferente do atual e sem depender de clima nem de horário.
     *
     * @param registro destino do evento de terreno
     * @param gerador sorteio do novo terreno
     */
    public void mudarTerrenoSalShard(RegistroBatalha registro, GeradorAleatorio gerador) {
        List<String> opcoes = new ArrayList<>(Terrenos.todos());
        opcoes.remove(this.climaAtual);
        this.climaAtual = opcoes.get(sortearIndice(gerador, opcoes.size()));
        aplicarCorClima();
        if(!modoTeste) {
            System.out.println("\nA Sal Shard brilhou! O Campo de Batalha Agora é O " + this.climaAtual + "!");
        }
        registro.registrarEfeitoTerreno("Campo", "Sal Shard ativou: " + climaAtual);
    }

    private void curarPeloTerreno(Pokesal p) {
        if (p.getTipo().getNomeTipo().equals("Planta") && p.getHp() > 0) {
            p.curar(CURA_CANTEIRO);
            if(!modoTeste) {
                System.out.println("[TERRENO] O Canteiro Central Restaurou a Vida de " + p.getNome() + "!");
            }
            registro.registrarEfeitoTerreno(p.getNome(), "Cura Terreno");
        }
    }

    /**
     * Aplica os efeitos de fim de turno: dano de status (queimadura e veneno) e a cura do Canteiro Central para Pokésals de Planta que ainda estão de pé.
     *
     * @param p1 Pokésal do primeiro treinador
     * @param p2 Pokésal do segundo treinador
     */
    public void processarFimDeTurno(Pokesal p1, Pokesal p2) {
        if(!modoTeste) {
            System.out.println("\n--- Fim de Turno ---");
        }

        if (p1.getHp() > 0) {
            p1.processarStatusFimDeTurno(registro);
        }
        if (p2.getHp() > 0) {
            p2.processarStatusFimDeTurno(registro);
        }
        if (Terrenos.CANTEIRO_CENTRAL.equals(climaAtual)) {
            curarPeloTerreno(p1);
            curarPeloTerreno(p2);
        }
    }

    private void turnoIndividual(Treinador atacante, Treinador alvo, Golpe golpe) {
        if(!modoTeste) {
            System.out.println("\n--- Turno de " + atacante.getPokesal().getNome() + " ---");
        }
        pausar(PAUSA_CURTA_MS);
        golpe.executar(atacante.getPokesal(), alvo.getPokesal(), climaAtual, gerador, registro);
    }

    /**
     * Resolve uma rodada: o mais rápido age primeiro e o outro contra-ataca se ainda estiver de pé. Um golpe {@code null} significa que o treinador usou um item no turno.
     *
     * @param t1 primeiro treinador
     * @param g1 golpe do primeiro treinador, ou {@code null} se usou item
     * @param t2 segundo treinador
     * @param g2 golpe do segundo treinador, ou {@code null} se usou item
     */
    public void resolverRodadaDeAtaques(Treinador t1, Golpe g1, Treinador t2, Golpe g2) {
        Pokesal p1 = t1.getPokesal();
        Pokesal p2 = t2.getPokesal();
        turnosAtuais++;

        if (g1 != null && g2 != null) {
            Treinador primeiro = (p1.getSpd() >= p2.getSpd()) ? t1 : t2;
            Treinador segundo = (primeiro == t1) ? t2 : t1;
            Golpe gP = (primeiro == t1) ? g1 : g2;
            Golpe gS = (primeiro == t1) ? g2 : g1;

            pausar(PAUSA_CURTA_MS);
            if(!modoTeste) {
                System.out.println("\n--- Ordem de Ataque ---");
                System.out.println(primeiro.getPokesal().getNome() + " é Mais Rápido e Toma a Frente!");
            }
            pausar(PAUSA_CURTA_MS);

            gP.executar(primeiro.getPokesal(), segundo.getPokesal(), climaAtual, gerador, registro);

            if (segundo.getPokesal().getHp() > 0) {
                if(!modoTeste) {
                    System.out.println("\nContra-ataque de " + segundo.getPokesal().getNome() + "!");
                }
                pausar(PAUSA_CURTA_MS);
                gS.executar(segundo.getPokesal(), primeiro.getPokesal(), climaAtual, gerador, registro);
            } else {
                if(!modoTeste) {
                    System.out.println("\n" + segundo.getPokesal().getNome() + " Desmaiou Antes de Conseguir Atacar!");
                }
                pausar(PAUSA_CURTA_MS);
            }
        } else if (g1 != null) {
            turnoIndividual(t1, t2, g1);
        } else if (g2 != null) {
            turnoIndividual(t2, t1, g2);
        }
        processarFimDeTurno(p1, p2);
    }

    /**
     * Batalha do jogador contra um adversário controlado pelo computador.
     *
     * @param jogador treinador humano
     * @param inimigo treinador controlado pelo computador
     * @param sc teclado do jogador
     * @return true se o jogador venceu
     */
    public boolean iniciarCombate(Treinador jogador, Treinador inimigo, Scanner sc) {
        prepararBatalha(jogador, inimigo);
        Pokesal pI = inimigo.getPokesal();

        if(!modoTeste) {
            System.out.println("\nUm " + pI.getNome() + " Adversário se Aproxima!");
        }
        pausar(PAUSA_CURTA_MS);
        pI.curar(1.0);
        pI.exibirSts();
        pausar(PAUSA_CURTA_MS);

        anunciarTerreno();
        pausar(PAUSA_LONGA_MS);

        Treinador vencedor = executarCombate(jogador, inimigo, sc, true);
        if(!modoTeste) {
            if(vencedor == jogador) {
                System.out.println("Você Venceu a Batalha!");
            } else {
                System.out.println("Seu PokeSal Desmaiou...");
            }
            System.out.print("\u001B[0m");
        }
        return vencedor == jogador;
    }

    /**
     * Batalha entre dois treinadores, cada um humano ou controlado pelo computador. O resultado é salvo no repositório ao final.
     *
     * @param t1 primeiro treinador
     * @param t2 segundo treinador
     * @param sc teclado dos jogadores
     * @return o treinador vencedor
     */
    public Treinador iniciarCombatePvP(Treinador t1, Treinador t2, Scanner sc) {
        prepararBatalha(t1, t2);
        Pokesal p1 = t1.getPokesal();
        Pokesal p2 = t2.getPokesal();

        if(!modoTeste) {
            System.out.println("\nBatalha PvP iniciada entre " + p1.getNome() + " (" + t1.getNome() + ") e " + p2.getNome() + " (" + t2.getNome() + ")!");
        }
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
        t1.getBag().resetarUsoBatalha();
        t2.getBag().resetarUsoBatalha();
        turnosAtuais = 0;
        this.climaAtual = consultarTerreno();
    }

    private void anunciarTerreno() {
        aplicarCorClima();
        if(!modoTeste) {
            System.out.println("Condição do Ambiente de Combate: " + climaAtual + "!");
        }
    }

    private Treinador executarCombate(Treinador t1, Treinador t2, Scanner sc, boolean t2Automatico) {
        Pokesal p1 = t1.getPokesal();
        Pokesal p2 = t2.getPokesal();

        while (p1.getHp() > 0 && p2.getHp() > 0) {
            Golpe g1 = escolherAcao(t1, sc, false);
            Golpe g2 = escolherAcao(t2, sc, t2Automatico);
            resolverRodadaDeAtaques(t1, g1, t2, g2);
        }

        boolean t1Venceu;
        if (p1.getHp() > 0) {
            t1Venceu = true;
        } else if (p2.getHp() > 0) {
            t1Venceu = false;
        } else {
            t1Venceu = p1.getSpd() >= p2.getSpd();
        }

        Treinador vencedor = t1Venceu ? t1 : t2;
        Treinador perdedor = t1Venceu ? t2 : t1;
        salvarResultado(vencedor, perdedor);
        return vencedor;
    }

    private void salvarResultado(Treinador vencedor, Treinador perdedor) {
        ResultadoBatalha resultado = new ResultadoBatalha(vencedor.getNome(), perdedor.getNome(), turnosAtuais,
                vencedor.getPokesal().getNome(), perdedor.getPokesal().getNome());
        try {
            repositorio.salvarResultado(resultado);
        }

        catch (Exception e) {
            LOG.warning("Falha ao salvar o resultado da batalha (" + resultado + "): " + e.getMessage());
        }
    }

    private Golpe golpeAleatorio(Pokesal p) {
        return p.getGolpes()[sortearIndice(gerador, p.getGolpes().length)];
    }

    private Golpe escolherAcao(Treinador t, Scanner sc, boolean automatico) {
        Pokesal p = t.getPokesal();

        if (automatico || t.isCpu()) {
            return golpeAleatorio(p);
        }
        if (modoTeste) {
            return p.getGolpes()[0];
        }

        while (true) {
            System.out.println("\n--- Turno de " + t.getNome() + " (" + p.getNome() + ") ---");
            System.out.println("HP: " + p.getHp() + "/" + p.getMaxHp());
            System.out.println("1 - Atacar\n2 - Mochila");
            if (!sc.hasNextInt()) {
                sc.next();
                continue;
            }
            int acao = sc.nextInt();
            if (acao == 1) {
                System.out.println("\nEscolha um Golpe:");
                for (int i=0; i<p.getGolpes().length; i++) { System.out.println((i+1) + " - " + p.getGolpes()[i].getNome()); }
                if (!sc.hasNextInt()) {
                    sc.next();
                    continue;
                }
                int esc = sc.nextInt() - 1;
                if (esc >= 0 && esc < p.getGolpes().length) {
                    return p.getGolpes()[esc];
                } else {
                    System.out.println("Opção Inválida!");
                }
            } else if (acao == 2) {
                try {
                    if (t.getBag().abrirMochila(p, this, registro, t, sc, gerador)) {
                        return null;
                    }
                } catch (LimiteDeItensExcedidoException e) {
                    if (!modoTeste) {
                        System.out.println("\n" + e.getMessage());
                    }
                    pausar(PAUSA_CURTA_MS);
                }
            }
        }
    }
}
