package pokeucsal;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Jogo no console: menu com os modos contra o computador, entre jogadores e torneio eliminatório.
 */
public class Main {
    private static final int TOTAL_POKESALS = 6;

    private static void exibirMenuPokesals() {
        System.out.println("\n--- ESCOLHA DOS POKÉSALS ---");
        System.out.println("1 - BulbaSal (Planta)   | 4 - ChikoSal (Planta)");
        System.out.println("2 - CharSal (Fogo)      | 5 - CyndaSal (Fogo)");
        System.out.println("3 - SquirtSal (Água)    | 6 - TotoSal (Água)");
        System.out.println("----------------------------");
    }

    private static final class EntradaEncerradaException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static int lerInteiro(Scanner sc, String mensagem) {
        while (true) {
            System.out.print(mensagem);
            if (!sc.hasNext()) {
                throw new EntradaEncerradaException();
            }
            if (sc.hasNextInt()) {
                return sc.nextInt();
            }
            sc.next();
            System.out.println("Entrada inválida! Digite um número.");
        }
    }

    private static int lerEscolha(Scanner sc, String mensagem, int min, int max) {
        while (true) {
            int escolha = lerInteiro(sc, mensagem);
            if (escolha >= min && escolha <= max) {
                return escolha;
            }
            System.out.println("Opção inválida! Escolha entre " + min + " e " + max + ".");
        }
    }

    private static int lerEscolhaPokesal(Scanner sc, String mensagem) {
        return lerEscolha(sc, mensagem, 1, TOTAL_POKESALS);
    }

    private static List<Treinador> montarParticipantes(int numeroDoJogador, int tamanho) {
        List<Integer> outros = new ArrayList<>();
        for (int n = 1; n <= TOTAL_POKESALS; n++) if (n != numeroDoJogador) outros.add(n);
        Collections.shuffle(outros);

        List<Integer> sorteados = new ArrayList<>(outros);
        for (int i = 0; sorteados.size() < tamanho - 1; i++) sorteados.add(outros.get(i % outros.size()));
        Collections.shuffle(sorteados);

        List<Treinador> participantes = new ArrayList<>();
        participantes.add(new Treinador("Jogador", CatalogoPokesal.criar(numeroDoJogador)));
        for (int i = 0; i < tamanho - 1; i++) {
            participantes.add(new Treinador("Adversário " + (i + 1), CatalogoPokesal.criar(sorteados.get(i)), true));
        }
        return participantes;
    }

    private static List<Pokesal> restantesEmbaralhados(Pokesal[] todos, Pokesal escolhido) {
        List<Pokesal> restantes = new ArrayList<>(Arrays.asList(todos));
        restantes.remove(escolhido);
        Collections.shuffle(restantes);
        return restantes;
    }

    /**
     * Inicia o jogo.
     *
     * @param args argumentos de linha de comando (não usados)
     */
    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            executar(sc);
        } catch (EntradaEncerradaException e) {
            System.out.println("\nEntrada encerrada. Até a próxima!");
        }
    }

    private static void executar(Scanner sc) {
        Clock relogio = Clock.system(ZoneId.of("America/Bahia"));
        GeradorAleatorio ga = new GeradorAleatorioPadrao();

        ServicoTerreno st = new ServicoTerrenoClima(new ClienteClimaOpenMeteo());
        RepositorioResultados rp = res -> System.out.println("[DB-Log] Resultado salvo: " + res);
        RegistroBatalha rb = new RegistroBatalhaEmMemoria();

        Batalha arena = new Batalha(st, relogio, rp, rb, ga);

        RegraDeCaptura regraDeCaptura = new RegraDeCapturaPadrao(ga);
        RepositorioCapturas repositorioCapturas = (treinador, pokesal) ->
                System.out.println("[DB-Log] Captura salva: " + treinador + " capturou " + pokesal);
        NotificadorCaptura notificadorCaptura = new NotificadorCapturaConsole();

        Pokesal[] todosIniciais = new Pokesal[TOTAL_POKESALS];
        for (int i = 0; i < TOTAL_POKESALS; i++) {
            todosIniciais[i] = CatalogoPokesal.criar(i + 1);
        }

        boolean rodandoJogo = true;

        while (rodandoJogo) {
            for (Pokesal p : todosIniciais) p.restaurar();

            System.out.println(
                "╔══════════════════════════════════════════════════════════╗\n"
                +"║    *** O TORNEIO DO ESTACIONAMENTO DA UCSAL ***          ║\n"
                +"║                 ESTÁ PARA COMEÇAR!                       ║\n"
                +"╚══════════════════════════════════════════════════════════╝\n"
                +"\n"
                +"  1 - Jogador vs Computador (PvE)\n"
                +"  2 - Jogador vs Jogador (PvP)\n"
                +"  3 - Modo Torneio Eliminatório (PVE) \n"
                +"  0 - Sair\n"
                +"───────────────────────────────────────────────────────────");

            int modoJogo = lerInteiro(sc, "> Escolha o modo de jogo: ");

            if (modoJogo == 1) {
                exibirMenuPokesals();
                Pokesal pEsc = todosIniciais[lerEscolhaPokesal(sc, "> Escolha seu Pokésal (1-6): ") - 1];
                Treinador t1 = new Treinador("Jogador 1", pEsc);

                Pokesal inimigo = restantesEmbaralhados(todosIniciais, pEsc).get(0);
                Treinador tCpu = new Treinador("Computador", inimigo, true);

                arena.iniciarCombate(t1, tCpu, sc);
            } else if (modoJogo == 2) {
                exibirMenuPokesals();
                Pokesal pokeJ1 = todosIniciais[lerEscolhaPokesal(sc, "> Jogador 1, escolha (1-6): ") - 1];
                Pokesal pokeJ2;
                do {
                    pokeJ2 = todosIniciais[lerEscolhaPokesal(sc, "> Jogador 2, escolha (1-6): ") - 1];
                    if (pokeJ2 == pokeJ1) {
                        System.out.println("O Jogador 1 já escolheu o " + pokeJ1.getNome() + "! Escolha outro.");
                    }
                } while (pokeJ2 == pokeJ1);

                arena.iniciarCombatePvP(new Treinador("Jogador 1", pokeJ1), new Treinador("Jogador 2", pokeJ2), sc);
            } else if (modoJogo == 3) {
                exibirMenuPokesals();
                int numeroDoJogador = lerEscolhaPokesal(sc, "> Escolha seu Pokésal para o torneio (1-6): ");

                System.out.println("\n  1 - Torneio de 4 Treinadores (semifinal e final)");
                System.out.println("  2 - Torneio de 8 Treinadores (quartas, semifinal e final)");
                int tamanho = lerEscolha(sc, "> Escolha o tamanho do torneio (1-2): ", 1, 2) == 1 ? 4 : 8;

                List<Treinador> participantes = montarParticipantes(numeroDoJogador, tamanho);
                System.out.println("\nTorneio de " + tamanho + " Treinadores gerado! Você escolheu "
                        + participantes.get(0).getPokesal().getNome() + ".");
                new Torneio(participantes, arena, sc, regraDeCaptura, repositorioCapturas, notificadorCaptura).iniciarTorneio();
            } else if (modoJogo == 0) {
                rodandoJogo = false;
            } else {
                System.out.println("Modo inválido! Escolha 0, 1, 2 ou 3.");
            }
        }
    }
}
