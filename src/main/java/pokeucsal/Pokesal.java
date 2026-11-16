package pokeucsal;

/**
 * Pokésal com atributos, tipo, golpes, status e precisão. HP, status e atributos pertencem ao objeto.
 */
public class Pokesal {
    public static final int PRECISAO_MAXIMA = 100;
    public static final int PRECISAO_MINIMA = 30;
    public static final double FATOR_ATK_QUEIMADO = 0.75;

    private final String nome;
    private final TipoPoke tipo;
    private final int maxHp;
    private final Golpe[] golpes;

    private final int atkBase;
    private final int defBase;
    private final int spdBase;

    private int atk;
    private int def;
    private int hp;
    private int spd;
    private int precisao = PRECISAO_MAXIMA;
    private boolean queimado = false;
    private boolean envenenado = false;
    private boolean paralisado = false;
    private int turnosEnvenenado = 0;

    private int spdPerdidoParalisia = 0;
    private int atkPerdidoQueimadura = 0;

    /**
     * Cria um Pokésal com HP cheio e sem status.
     *
     * @param nome nome do Pokésal
     * @param tipo tipo elemental
     * @param atk ataque
     * @param def defesa
     * @param hp pontos de vida máximos
     * @param spd velocidade
     * @param golpes golpes disponíveis
     */
    public Pokesal(String nome, TipoPoke tipo, int atk, int def, int hp, int spd, Golpe[] golpes) {
        this.nome = nome;
        this.tipo = tipo;
        this.atk = atk;
        this.def = def;
        this.hp = hp;
        this.maxHp = hp;
        this.spd = spd;
        this.golpes = golpes;
        this.atkBase = atk;
        this.defBase = def;
        this.spdBase = spd;
    }
    /** Devolve o nome. */
    public String getNome() {
        return nome;
    }
    /** Devolve o tipo elemental. */
    public TipoPoke getTipo() {
        return tipo;
    }
    /** Devolve o ataque atual. */
    public int getAtk() {
        return atk;
    }
    /**
     * Define o ataque atual (nunca negativo).
     *
     * @param atk novo ataque
     */
    public void setAtk(int atk) {
        this.atk = Math.max(0, atk);
    }
    /** Devolve a defesa atual. */
    public int getDef() {
        return def;
    }
    /**
     * Define a defesa atual (nunca negativa).
     *
     * @param def nova defesa
     */
    public void setDef(int def) {
        this.def = Math.max(0, def);
    }
    /** Devolve o HP atual. */
    public int getHp() {
        return hp;
    }
    /** Devolve o HP máximo. */
    public int getMaxHp() {
        return maxHp;
    }
    /** Devolve a velocidade atual. */
    public int getSpd() {
        return spd;
    }
    /**
     * Define a velocidade atual (nunca negativa).
     *
     * @param spd nova velocidade
     */
    public void setSpd(int spd) {
        this.spd = Math.max(0, spd);
    }
    /** Devolve a precisão atual dos golpes, de 30 a 100. */
    public int getPrecisao() {
        return precisao;
    }
    /** Devolve os golpes do Pokésal. */
    public Golpe[] getGolpes() {
        return golpes;
    }

    /**
     * Liga ou desliga a queimadura. Ao queimar, o ataque cai 25% até a cura.
     *
     * @param q true para queimar, false para curar
     */
    public void setQueimado(boolean q) {
        if (q && !this.queimado) {
            int novoAtk = (int) (this.atk * FATOR_ATK_QUEIMADO);
            this.atkPerdidoQueimadura = this.atk - novoAtk;
            this.atk = novoAtk;
            if (!Batalha.modoTeste) {
                System.out.println(nome + " foi queimado! Ataque caiu.");
            }
        } else if (!q && this.queimado) {
            this.atk += this.atkPerdidoQueimadura;
            this.atkPerdidoQueimadura = 0;
        }
        this.queimado = q;
    }
    /** Informa se está queimado. */
    public boolean isQueimado() {
        return queimado;
    }
    /**
     * Liga ou desliga o veneno. Ao envenenar, a contagem do dano progressivo recomeça.
     *
     * @param e true para envenenar, false para curar
     */
    public void setEnvenenado(boolean e) {
        this.envenenado = e;
        if(e) {
            turnosEnvenenado = 0;
            if(!Batalha.modoTeste) {
                System.out.println(nome + " foi envenenado!");
            }
        }
    }
    /** Informa se está envenenado. */
    public boolean isEnvenenado() {
        return envenenado;
    }

    /**
     * Liga ou desliga a paralisia. Ao paralisar, a velocidade cai 25% até a cura.
     *
     * @param p true para paralisar, false para curar
     */
    public void setParalisado(boolean p) {
        if (p && !this.paralisado) {
            int novaSpd = (int) (this.spd * 0.75);
            this.spdPerdidoParalisia = this.spd - novaSpd;
            this.spd = novaSpd;
            if(!Batalha.modoTeste) {
                System.out.println(nome + " foi paralisado! Velocidade caiu.");
            }
        } else if (!p && this.paralisado) {
            this.spd += this.spdPerdidoParalisia;
            this.spdPerdidoParalisia = 0;
        }
        this.paralisado = p;
    }
    /** Informa se está paralisado. */
    public boolean isParalisado() {
        return paralisado;
    }

    /**
     * Reduz a precisão dos golpes, sem passar do mínimo de 30.
     *
     * @param valor pontos de precisão a perder
     */
    public void diminuirPrecisao(int valor) {
        this.precisao = Math.max(PRECISAO_MINIMA, this.precisao - valor);
        if(!Batalha.modoTeste) {
            System.out.println("Efeito de Golpe: A Precisão de " + this.nome + " Caiu Para " + this.precisao + "%!");
        }
    }

    /**
     * Aplica o dano de queimadura e de veneno do fim do turno e registra cada um.
     *
     * @param registro destino dos eventos de status
     */
    public void processarStatusFimDeTurno(RegistroBatalha registro) {
        if (queimado) {
            int d = Math.max(1, maxHp / 16);
            this.hp = Math.max(0, this.hp - d);
            if(!Batalha.modoTeste) {
                System.out.println(nome + " Sofreu " + d + " de Dano por Queimadura! HP: " + this.hp);
            }
            registro.registrarEfeitoStatus(nome, "Dano Queimadura");
        }
        if (envenenado) {
            turnosEnvenenado++;
            int d = Math.max(1, (maxHp / 16) * turnosEnvenenado);
            this.hp = Math.max(0, this.hp - d);
            if(!Batalha.modoTeste) {
                System.out.println(nome + " Sofreu " + d + " de Dano por Veneno Acumulado! HP: " + this.hp);
            }
            registro.registrarEfeitoStatus(nome, "Dano Veneno");
        }
    }

    /**
     * Aplica dano: a defesa reduz um terço do dano bruto, o mínimo é 2 e cada golpe tira no máximo 85% do HP máximo.
     *
     * @param danoBruto dano antes da defesa
     */
    public void dano(int danoBruto) {
        int mitigacao = this.def / 3;
        int danoEfetivo = Math.max(2, danoBruto - mitigacao);
        int danoFinal = Math.min(danoEfetivo, Math.max(1, (int)(this.maxHp * 0.85)));

        this.hp = Math.max(0, this.hp - danoFinal);
        if(!Batalha.modoTeste) {
            System.out.println(this.nome + " recebeu " + danoFinal + " de dano! (Bruto: " + danoBruto + ", Defesa atual do alvo: " + this.def + ") | Vida restante: " + this.hp + "/" + this.maxHp);
        }
    }
    /**
     * Recupera uma porcentagem do HP máximo, sem passar do máximo.
     *
     * @param pct fração do HP máximo, por exemplo 0.15 para 15%
     */
    public void curar(double pct) {
        this.hp = Math.min(this.maxHp, this.hp + (int)(this.maxHp * pct));
    }

    /** Remove queimadura, veneno e paralisia, devolvendo o que eles tiraram dos atributos. */
    public void curarStatus() {
        setParalisado(false);
        setQueimado(false);
        this.envenenado = false;
        this.turnosEnvenenado = 0;
    }

    /**
     * Devolve o Pokésal ao estado inicial: HP cheio, sem status, atributos e precisão originais.
     */
    public void restaurar() {
        curarStatus();
        this.atk = atkBase;
        this.def = defBase;
        this.spd = spdBase;
        this.precisao = PRECISAO_MAXIMA;
        this.hp = maxHp;
    }

    /** Mostra no console o status atual do Pokésal. */
    public void exibirSts() {
        if(!Batalha.modoTeste) {
            System.out.println("--- Status de " + nome + " ---");
            System.out.println("Tipo: " + tipo.getNomeTipo() + " | Vida: " + hp + "/" + maxHp);
            System.out.println("Atk: " + atk + " | Def: " + def + " | Spd: " + spd);
            System.out.println("--------------------------");
        }
    }
}
