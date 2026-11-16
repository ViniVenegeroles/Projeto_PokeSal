package pokeucsal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class Torneio {
    private static final Logger LOG = Logger.getLogger(Torneio.class.getName());

    private final List<Treinador> participantes;
    private final Batalha arena;
    private final Scanner sc;
    private final RegraDeCaptura regraDeCaptura;
    private final RepositorioCapturas repositorioCapturas;
    private final NotificadorCaptura notificadorCaptura;

    // Placar do torneio (na ordem de inscrição): vitórias e como cada treinador terminou.
    private final Map<Treinador, Integer> vitorias = new LinkedHashMap<>();
    private final Map<Treinador, String> situacao = new LinkedHashMap<>();

    /**
     * Todos os colaboradores entram pelo construtor (regra 2.5): quem decide a captura, quem a guarda e quem avisa.
     * Não existe construtor "só com a arena": ele precisaria criar esses colaboradores aqui dentro.
     */
    public Torneio(List<Treinador> partes, Batalha arena, Scanner sc, RegraDeCaptura regraDeCaptura,
                   RepositorioCapturas repositorioCapturas, NotificadorCaptura notificadorCaptura) {
        // Regra 2.3: o torneio é eliminatório com 4 ou 8 treinadores.
        // Sem esta validação, uma lista ímpar estourava IndexOutOfBounds no meio do torneio (rodada.get(i + 1)).
        if (partes == null || (partes.size() != 4 && partes.size() != 8)) {
            throw new IllegalArgumentException("O torneio exige 4 ou 8 treinadores.");
        }
        this.participantes = new ArrayList<>(partes); // cópia: mexer na lista de fora não altera o torneio
        this.arena = arena;
        this.sc = sc;
        this.regraDeCaptura = regraDeCaptura;
        this.repositorioCapturas = repositorioCapturas;
        this.notificadorCaptura = notificadorCaptura;
        for (Treinador t : participantes) {
            vitorias.put(t, 0);
            situacao.put(t, "");
        }
    }

    /** Vitórias de cada treinador, na ordem de inscrição (só leitura). */
    public Map<Treinador, Integer> getVitorias() { return Collections.unmodifiableMap(vitorias); }

    public Treinador iniciarTorneio() {
        List<Treinador> rodada = new ArrayList<>(participantes);
        System.out.println("\n=== INÍCIO DO TORNEIO (" + participantes.size() + " Treinadores) ===");
        Batalha.pausar(Batalha.PAUSA_LONGA_MS);

        while (rodada.size() > 1) {
            boolean finalDoTorneio = rodada.size() == 2;
            System.out.println("\n----- " + nomeDaFase(rodada.size()).toUpperCase() + " -----");
            List<Treinador> proxima = new ArrayList<>();
            for (int i = 0; i < rodada.size(); i += 2) {
                Treinador a = rodada.get(i);
                Treinador b = rodada.get(i + 1);

                // Cada batalha começa do zero: HP cheio, sem status, atributos e precisão originais (time inteiro).
                revitalizar(a);
                revitalizar(b);

                // Antes da luta: quem é humano vê o adversário e pode trocar de Pokésal entre os capturados.
                prepararTreinador(a, b);
                prepararTreinador(b, a);

                System.out.println("\n>>> CONFRONTO: " + a.getNome() + " (" + a.getPokemon().getNome() + ") x "
                        + b.getNome() + " (" + b.getPokemon().getNome() + ")");
                Treinador vencedor = arena.iniciarCombatePvP(a, b, sc);
                Treinador derrotado = (vencedor == a) ? b : a;
                proxima.add(vencedor);
                vitorias.merge(vencedor, 1, Integer::sum);
                situacao.put(derrotado, finalDoTorneio ? "Vice-campeão" : "Eliminado: " + nomeDaFase(rodada.size()));

                // Depois da luta: o vencedor humano pode tentar capturar o Pokésal derrotado (menos na final: acabou).
                if (!finalDoTorneio && !vencedor.isCpu()) {
                    oferecerCaptura(vencedor, derrotado.getPokemon());
                }
            }
            rodada = proxima;
            if (rodada.size() > 1) {
                System.out.println("\n>>> AVANÇANDO PARA A PRÓXIMA FASE! <<<");
                Batalha.pausar(Batalha.PAUSA_LONGA_MS);
            }
        }
        Treinador campeao = rodada.get(0);
        situacao.put(campeao, "Campeão");
        System.out.println("\n*** CAMPEÃO DO ESTACIONAMENTO: " + campeao.getNome() + " ***");
        exibirClassificacao();
        return campeao;
    }

    private static String nomeDaFase(int treinadoresNaRodada) {
        if (treinadoresNaRodada == 2) return "Final";
        if (treinadoresNaRodada == 4) return "Semifinal";
        return "Quartas de final";
    }

    private void revitalizar(Treinador t) {
        for (Pokemon p : t.getEquipe()) p.restaurar();
    }

    // -------------------------------------------------------- tabela final

    /** Quem venceu mais partidas fica em cima; empate de vitórias mantém a ordem de inscrição. */
    private void exibirClassificacao() {
        List<Treinador> ordem = new ArrayList<>(participantes);
        ordem.sort((x, y) -> Integer.compare(vitorias.get(y), vitorias.get(x))); // sort de List é estável

        System.out.println("\n============ CLASSIFICAÇÃO FINAL ============");
        System.out.println(String.format("%-3s %-14s %-11s %-5s %-8s %s", "#", "Treinador", "Inicial", "Vit.", "Capturas", "Situação"));
        int posicao = 1;
        for (Treinador t : ordem) {
            System.out.println(String.format("%-3d %-14s %-11s %-5d %-8d %s", posicao++, t.getNome(),
                    t.getEquipe().get(0).getNome(), vitorias.get(t), t.getEquipe().size() - 1, situacao.get(t)));
        }
    }

    // ---------------------------------------------------------------- captura

    private void oferecerCaptura(Treinador vencedor, Pokemon derrotado) {
        System.out.println("\nVocê derrotou " + derrotado.getNome() + " (" + derrotado.getTipo().getNomeTipo() + ")!");
        System.out.println("Deseja tentar capturá-lo? (1 - Sim | 0 - Não)");
        if (lerOpcao(0, 1) != 1) {
            System.out.println(derrotado.getNome() + " ficou para trás.");
            return;
        }
        if (!regraDeCaptura.capturaTemSucesso(derrotado)) {
            System.out.println(">>> " + derrotado.getNome() + " escapou! A captura falhou.");
            return;
        }
        if (vencedor.capturar(derrotado)) {
            registrarCaptura(vencedor, derrotado);
        }
    }

    /** Depois que o Pokésal já está no time: guardar e avisar. Se qualquer um falhar, a captura continua valendo. */
    private void registrarCaptura(Treinador vencedor, Pokemon capturado) {
        try {
            repositorioCapturas.salvarCaptura(vencedor.getNome(), capturado.getNome());
        } catch (Exception e) {
            LOG.warning("Falha ao salvar a captura de " + capturado.getNome() + " por " + vencedor.getNome() + ": " + e.getMessage());
        }
        try {
            notificadorCaptura.notificarCaptura(vencedor.getNome(), capturado.getNome());
        } catch (Exception e) {
            LOG.warning("Falha ao notificar a captura de " + capturado.getNome() + ": " + e.getMessage());
        }
    }

    // ------------------------------------------------- próxima luta e troca

    private void prepararTreinador(Treinador t, Treinador adversario) {
        if (t.isCpu()) return;
        Pokemon inimigo = adversario.getPokemon();
        System.out.println("\n--- PRÓXIMA LUTA de " + t.getNome() + " ---");
        System.out.println("Seus Pokésals foram revitalizados para a luta.");
        System.out.println("Você enfrentará " + adversario.getNome() + " com " + inimigo.getNome()
                + " (" + inimigo.getTipo().getNomeTipo() + ") | HP " + inimigo.getHp() + "/" + inimigo.getMaxHp()
                + " | Atk " + inimigo.getAtk() + " | Def " + inimigo.getDef() + " | Spd " + inimigo.getSpd());
        oferecerTroca(t);
    }

    private void oferecerTroca(Treinador t) {
        List<Pokemon> equipe = t.getEquipe();
        if (equipe.size() < 2) return; // só o inicial: não há o que escolher

        System.out.println("Escolha quem vai lutar:");
        for (int i = 0; i < equipe.size(); i++) {
            Pokemon p = equipe.get(i);
            String emCampo = (p == t.getPokemon()) ? " <- em campo" : "";
            System.out.println((i + 1) + " - " + p.getNome() + " (" + p.getTipo().getNomeTipo() + ") HP " + p.getHp() + "/"
                    + p.getMaxHp() + " | Atk " + p.getAtk() + " | Def " + p.getDef() + " | Spd " + p.getSpd() + emCampo);
        }
        int escolha = lerOpcao(1, equipe.size());
        // Sem entrada válida (ex.: fim do teclado) mantém quem já estava em campo.
        if (escolha >= 1) {
            Pokemon novo = equipe.get(escolha - 1);
            if (novo != t.getPokemon()) System.out.println(t.getNome() + " trocou para " + novo.getNome() + "!");
            t.trocarPokemon(novo);
        }
    }

    /**
     * Lê um número entre min e max, repetindo a pergunta se vier algo inválido.
     * Devolve min - 1 se o teclado acabar (Scanner sem mais entrada), para o torneio nunca travar nem quebrar.
     */
    private int lerOpcao(int min, int max) {
        while (true) {
            System.out.print("> ");
            if (!sc.hasNext()) return min - 1;
            if (sc.hasNextInt()) {
                int v = sc.nextInt();
                if (v >= min && v <= max) return v;
            } else {
                sc.next();
            }
            System.out.println("Opção inválida! Digite um número de " + min + " a " + max + ".");
        }
    }
}
