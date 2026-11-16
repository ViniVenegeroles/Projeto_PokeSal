package pokeucsal;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final int TOTAL_POKEMONS = 6;
    
    private static void exibirMenuPokemons() {
        System.out.println("\n--- ESCOLHA DOS POKÉSALS ---");
        System.out.println("1 - BulbaSal (Planta)   | 4 - ChikoSal (Planta)");
        System.out.println("2 - CharSal (Fogo)      | 5 - CyndaSal (Fogo)");
        System.out.println("3 - SquirtSal (Água)    | 6 - TotoSal (Água)");
        System.out.println("----------------------------");
    }

    /**
     * Lê um inteiro do teclado sem quebrar se o usuário digitar letras.
     * Por quê: sc.nextInt() puro lança InputMismatchException e derrubava o jogo inteiro.
     */
    private static int lerInteiro(Scanner sc, String mensagem) {
        while (true) {
            System.out.print(mensagem);
            if (sc.hasNextInt()) return sc.nextInt();
            sc.next(); // descarta o token inválido, senão o loop ficaria preso nele
            System.out.println("Entrada inválida! Digite um número.");
        }
    }

    /** Repete a pergunta até o usuário digitar um número entre min e max (uma entrada inválida nunca escolhe "no escuro"). */
    private static int lerEscolha(Scanner sc, String mensagem, int min, int max) {
        while (true) {
            int escolha = lerInteiro(sc, mensagem);
            if (escolha >= min && escolha <= max) return escolha;
            System.out.println("Opção inválida! Escolha entre " + min + " e " + max + ".");
        }
    }

    private static int lerEscolhaPokemon(Scanner sc, String mensagem) {
        return lerEscolha(sc, mensagem, 1, TOTAL_POKEMONS);
    }

    /**
     * 1 treinador humano + (tamanho - 1) adversários CPU. Só existem 6 espécies: no torneio de 8, duas espécies
     * aparecem repetidas, mas cada treinador recebe um Pokésal NOVO (criar() nunca devolve o mesmo objeto).
     */
    private static List<Treinador> montarParticipantes(int numeroDoJogador, int tamanho) {
        List<Integer> outros = new ArrayList<>();
        for (int n = 1; n <= TOTAL_POKEMONS; n++) if (n != numeroDoJogador) outros.add(n);
        Collections.shuffle(outros);

        List<Integer> sorteados = new ArrayList<>(outros);
        for (int i = 0; sorteados.size() < tamanho - 1; i++) sorteados.add(outros.get(i % outros.size()));
        Collections.shuffle(sorteados);

        List<Treinador> participantes = new ArrayList<>();
        participantes.add(new Treinador("Jogador", CatalogoPokemon.criar(numeroDoJogador)));
        for (int i = 0; i < tamanho - 1; i++) {
            // O 3º argumento (true) marca como CPU: eles jogam sozinhos, sem pedir golpes no teclado.
            participantes.add(new Treinador("Adversário " + (i + 1), CatalogoPokemon.criar(sorteados.get(i)), true));
        }
        return participantes;
    }

    /** Todos os Pokésals menos o escolhido, embaralhados (quem sobra vira adversário). */
    private static List<Pokemon> restantesEmbaralhados(Pokemon[] todos, Pokemon escolhido) {
        // Por quê excluir o escolhido: o HP é do objeto Pokemon. Se o mesmo objeto lutasse dos dois lados,
        // ele bateria nele mesmo (o PvP tinha esse bug quando os dois jogadores digitavam o mesmo número).
        List<Pokemon> restantes = new ArrayList<>(Arrays.asList(todos));
        restantes.remove(escolhido);
        Collections.shuffle(restantes);
        return restantes;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // --- Colaboradores (injetados no construtor da Batalha: Princípio da Inversão de Dependência) ---
        // Fuso de Salvador: o "é dia?" da regra do Asfalto Quente precisa bater com o clima consultado.
        Clock relogio = Clock.system(ZoneId.of("America/Bahia"));
        GeradorAleatorio ga = new GeradorAleatorioPadrao();
        // O terreno agora vem do CLIMA REAL de Salvador (Open-Meteo): chuva = Piso Escorregadio,
        // nublado = Canteiro Central, sol = Asfalto Quente. Sem sorteio; se a internet cair, vale o Terreno Neutro.
        ServicoTerreno st = new ServicoTerrenoClima(new ClienteClimaOpenMeteo());
        RepositorioResultados rp = res -> System.out.println("[DB-Log] Resultado salvo: " + res);
        RegistroBatalha rb = new RegistroBatalha() {
            public void registrarAtaque(String a, String g) {}
            public void registrarDano(String d, int dan) {}
            public void registrarEfeitoStatus(String a, String s) {}
            public void registrarEfeitoTerreno(String p, String e) {}
            public void registrarUsoItem(String t, String i) {}
        };

        Batalha arena = new Batalha(st, relogio, rp, rb, ga);

        // Captura do torneio: quem decide (70% de chance), quem guarda e quem avisa.
        RegraDeCaptura regraDeCaptura = new RegraDeCapturaPadrao(ga);
        RepositorioCapturas repositorioCapturas = (treinador, pokemon) ->
                System.out.println("[DB-Log] Captura salva: " + treinador + " capturou " + pokemon);
        NotificadorCaptura notificadorCaptura = new NotificadorCapturaConsole();

        Pokemon[] todosIniciais = new Pokemon[TOTAL_POKEMONS];
        for (int i = 0; i < TOTAL_POKEMONS; i++) todosIniciais[i] = CatalogoPokemon.criar(i + 1);

        boolean rodandoJogo = true;

        while (rodandoJogo) {
            // restaurar() em vez de curar(1.0): além do HP, desfaz buffs/debuffs, precisão reduzida e status
            // que ficavam grudados no Pokésal depois da batalha anterior.
            for (Pokemon p : todosIniciais) p.restaurar();

            System.out.println(
                "╔══════════════════════════════════════════════════════════╗\n"
                +"║    *** O TORNEIO DO ESTACIONAMENTO DA UCSAL ***          ║\n"
                +"║                 ESTÁ PARA COMEÇAR!                       ║\n"
                +"╚══════════════════════════════════════════════════════════╝\n"
                +"\n"
                +"  1 - Jogador vs Computador (PvE)\n"
                +"  2 - Jogador vs Jogador (PvP)\n"
                +"  3 - Modo Torneio Eliminatório (PVE)\n"
                +"  0 - Sair\n"
                +"───────────────────────────────────────────────────────────");
            
            int modoJogo = lerInteiro(sc, "> Escolha o modo de jogo: ");

            if (modoJogo == 1) { 
                exibirMenuPokemons();
                Pokemon pEsc = todosIniciais[lerEscolhaPokemon(sc, "> Escolha seu Pokésal (1-6): ") - 1];
                Treinador t1 = new Treinador("Jogador 1", pEsc);
                
                Pokemon inimigo = restantesEmbaralhados(todosIniciais, pEsc).get(0);
                Treinador tCpu = new Treinador("Computador", inimigo, true);
                
                arena.iniciarCombate(t1, tCpu, sc);
            } 
            else if (modoJogo == 2) { 
                exibirMenuPokemons();
                Pokemon pokeJ1 = todosIniciais[lerEscolhaPokemon(sc, "> Jogador 1, escolha (1-6): ") - 1];
                Pokemon pokeJ2;
                do {
                    pokeJ2 = todosIniciais[lerEscolhaPokemon(sc, "> Jogador 2, escolha (1-6): ") - 1];
                    if (pokeJ2 == pokeJ1) System.out.println("O Jogador 1 já escolheu o " + pokeJ1.getNome() + "! Escolha outro.");
                } while (pokeJ2 == pokeJ1);
                
                arena.iniciarCombatePvP(new Treinador("Jogador 1", pokeJ1), new Treinador("Jogador 2", pokeJ2), sc);
            }
            else if (modoJogo == 3) { 
                // O jogador escolhe o próprio Pokésal e o tamanho do torneio; os adversários são controlados pelo computador.
                exibirMenuPokemons();
                int numeroDoJogador = lerEscolhaPokemon(sc, "> Escolha seu Pokésal para o torneio (1-6): ");

                System.out.println("\n  1 - Torneio de 4 Treinadores (semifinal e final)");
                System.out.println("  2 - Torneio de 8 Treinadores (quartas, semifinal e final)");
                int tamanho = lerEscolha(sc, "> Escolha o tamanho do torneio (1-2): ", 1, 2) == 1 ? 4 : 8;

                List<Treinador> participantes = montarParticipantes(numeroDoJogador, tamanho);
                System.out.println("\nTorneio de " + tamanho + " Treinadores gerado! Você escolheu "
                        + participantes.get(0).getPokemon().getNome() + ".");
                new Torneio(participantes, arena, sc, regraDeCaptura, repositorioCapturas, notificadorCaptura).iniciarTorneio();
            }
            else if (modoJogo == 0) {
                rodandoJogo = false;
            }
            else {
                System.out.println("Modo inválido! Escolha 0, 1, 2 ou 3.");
            }
        }
    }
}
