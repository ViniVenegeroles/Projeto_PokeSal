package pokeucsal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Torneio eliminatório de 4 ou 8 treinadores, com captura do Pokésal derrotado, escolha do Pokésal antes de cada luta, revitalização a cada batalha e classificação final por vitórias.
 */
public class Torneio {
    private static final Logger LOG = Logger.getLogger(Torneio.class.getName());

    private final List<Treinador> participantes;
    private final Batalha arena;
    private final Scanner sc;
    private final RegraDeCaptura regraDeCaptura;
    private final RepositorioCapturas repositorioCapturas;
    private final NotificadorCaptura notificadorCaptura;

    private final Map<Treinador, Integer> vitorias = new LinkedHashMap<>();
    private final Map<Treinador, String> situacao = new LinkedHashMap<>();

    /**
     * Cria o torneio. Todos os colaboradores chegam pelo construtor.
     *
     * @param partes treinadores inscritos (4 ou 8)
     * @param arena batalha usada em cada confronto
     * @param sc teclado do jogador
     * @param regraDeCaptura decide se a captura dá certo
     * @param repositorioCapturas guarda as capturas
     * @param notificadorCaptura avisa as capturas
     * @throws IllegalArgumentException se o número de treinadores não for 4 nem 8
     */
    public Torneio(List<Treinador> partes, Batalha arena, Scanner sc, RegraDeCaptura regraDeCaptura,
                   RepositorioCapturas repositorioCapturas, NotificadorCaptura notificadorCaptura) {
        if (partes == null || (partes.size() != 4 && partes.size() != 8)) {
            throw new IllegalArgumentException("O torneio exige 4 ou 8 treinadores.");
        }
        this.participantes = new ArrayList<>(partes);
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

    /** Devolve as vitórias de cada treinador, na ordem de inscrição (somente leitura). */
    public Map<Treinador, Integer> getVitorias() {
        return Collections.unmodifiableMap(vitorias);
    }

    /**
     * Joga o torneio inteiro e mostra a classificação final.
     *
     * @return o campeão
     */
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

                revitalizar(a);
                revitalizar(b);

                prepararTreinador(a, b);
                prepararTreinador(b, a);

                System.out.println("\n>>> CONFRONTO: " + a.getNome() + " (" + a.getPokesal().getNome() + ") x "
                        + b.getNome() + " (" + b.getPokesal().getNome() + ")");
                Treinador vencedor = arena.iniciarCombatePvP(a, b, sc);
                Treinador derrotado = (vencedor == a) ? b : a;
                proxima.add(vencedor);
                vitorias.merge(vencedor, 1, Integer::sum);
                situacao.put(derrotado, finalDoTorneio ? "Vice-campeão" : "Eliminado: " + nomeDaFase(rodada.size()));

                if (!finalDoTorneio && !vencedor.isCpu()) {
                    oferecerCaptura(vencedor, derrotado.getPokesal());
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
        if (treinadoresNaRodada == 2) {
            return "Final";
        }
        if (treinadoresNaRodada == 4) {
            return "Semifinal";
        }
        return "Quartas de final";
    }

    private void revitalizar(Treinador t) {
        for (Pokesal p : t.getEquipe()) p.restaurar();
    }

    private void exibirClassificacao() {
        List<Treinador> ordem = new ArrayList<>(participantes);
        ordem.sort((x, y) -> Integer.compare(vitorias.get(y), vitorias.get(x)));

        System.out.println("\n============ CLASSIFICAÇÃO FINAL ============");
        System.out.println(String.format("%-3s %-14s %-11s %-5s %-8s %s", "#", "Treinador", "Inicial", "Vit.", "Capturas", "Situação"));
        int posicao = 1;
        for (Treinador t : ordem) {
            System.out.println(String.format("%-3d %-14s %-11s %-5d %-8d %s", posicao++, t.getNome(),
                    t.getEquipe().get(0).getNome(), vitorias.get(t), t.getEquipe().size() - 1, situacao.get(t)));
        }
    }

    private void oferecerCaptura(Treinador vencedor, Pokesal derrotado) {
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

    private void registrarCaptura(Treinador vencedor, Pokesal capturado) {
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

    private void prepararTreinador(Treinador t, Treinador adversario) {
        if (t.isCpu()) {
            return;
        }
        Pokesal inimigo = adversario.getPokesal();
        System.out.println("\n--- PRÓXIMA LUTA de " + t.getNome() + " ---");
        System.out.println("Seus Pokésals foram revitalizados para a luta.");
        System.out.println("Você enfrentará " + adversario.getNome() + " com " + inimigo.getNome()
                + " (" + inimigo.getTipo().getNomeTipo() + ") | HP " + inimigo.getHp() + "/" + inimigo.getMaxHp()
                + " | Atk " + inimigo.getAtk() + " | Def " + inimigo.getDef() + " | Spd " + inimigo.getSpd());
        oferecerTroca(t);
    }

    private void oferecerTroca(Treinador t) {
        List<Pokesal> equipe = t.getEquipe();
        if (equipe.size() < 2) {
            return;
        }

        System.out.println("Escolha quem vai lutar:");
        for (int i = 0; i < equipe.size(); i++) {
            Pokesal p = equipe.get(i);
            String emCampo = (p == t.getPokesal()) ? " <- em campo" : "";
            System.out.println((i + 1) + " - " + p.getNome() + " (" + p.getTipo().getNomeTipo() + ") HP " + p.getHp() + "/"
                    + p.getMaxHp() + " | Atk " + p.getAtk() + " | Def " + p.getDef() + " | Spd " + p.getSpd() + emCampo);
        }
        int escolha = lerOpcao(1, equipe.size());

        if (escolha >= 1) {
            Pokesal novo = equipe.get(escolha - 1);
            if (novo != t.getPokesal()) {
                System.out.println(t.getNome() + " trocou para " + novo.getNome() + "!");
            }
            t.trocarPokesal(novo);
        }
    }

    private int lerOpcao(int min, int max) {
        while (true) {
            System.out.print("> ");
            if (!sc.hasNext()) {
                return min - 1;
            }
            if (sc.hasNextInt()) {
                int v = sc.nextInt();
                if (v >= min && v <= max) {
                    return v;
                }
            } else {
                sc.next();
            }
            System.out.println("Opção inválida! Digite um número de " + min + " a " + max + ".");
        }
    }
}
