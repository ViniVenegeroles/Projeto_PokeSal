package pokeucsal;

public class Pokemon {
    public static final int PRECISAO_MAXIMA = 100;
    public static final int PRECISAO_MINIMA = 30;

    private final String nome;
    private final TipoPoke tipo;
    private final int maxHp;
    private final Golpe[] golpes;

    // Valores ORIGINAIS dos atributos. Antes não eram guardados, então buffs, debuffs, precisão
    // reduzida e status de uma batalha "vazavam" para a batalha seguinte (o Main só curava o HP).
    private final int atkBase;
    private final int defBase;
    private final int spdBase;

    private int atk; private int def; private int hp; private int spd;
    private int precisao = PRECISAO_MAXIMA;
    private boolean queimado = false; private boolean envenenado = false; private boolean paralisado = false;
    private int turnosEnvenenado = 0;
    // Quanto de velocidade a paralisia tirou, para devolver exatamente isso ao curar.
    private int spdPerdidoParalisia = 0;

    public Pokemon(String nome, TipoPoke tipo, int atk, int def, int hp, int spd, Golpe[] golpes) {
        this.nome = nome; this.tipo = tipo; this.atk = atk; this.def = def;
        this.hp = hp; this.maxHp = hp; this.spd = spd; this.golpes = golpes;
        this.atkBase = atk; this.defBase = def; this.spdBase = spd;
    }
    public String getNome() { return nome; }
    public TipoPoke getTipo() { return tipo; }
    public int getAtk() { return atk; } public void setAtk(int atk) { this.atk = Math.max(0, atk); }
    public int getDef() { return def; } public void setDef(int def) { this.def = Math.max(0, def); }
    public int getHp() { return hp; } public int getMaxHp() { return maxHp; }
    public int getSpd() { return spd; } public void setSpd(int spd) { this.spd = Math.max(0, spd); }
    public int getPrecisao() { return precisao; }
    public Golpe[] getGolpes() { return golpes; }

    public void setQueimado(boolean q) { this.queimado = q; if(q && !Batalha.modoTeste) System.out.println(nome + " foi queimado!"); } 
    public boolean isQueimado() { return queimado; }
    public void setEnvenenado(boolean e) { this.envenenado = e; if(e) { turnosEnvenenado = 0; if(!Batalha.modoTeste) System.out.println(nome + " foi envenenado!"); } } 
    public boolean isEnvenenado() { return envenenado; }

    public void setParalisado(boolean p) {
        if (p && !this.paralisado) {
            // Guarda quanto foi perdido para poder devolver na cura. Antes o Antídoto removia a
            // paralisia mas a velocidade ficava 25% menor para sempre.
            int novaSpd = (int) (this.spd * 0.75);
            this.spdPerdidoParalisia = this.spd - novaSpd;
            this.spd = novaSpd;
            if(!Batalha.modoTeste) System.out.println(nome + " foi paralisado! Velocidade caiu.");
        } else if (!p && this.paralisado) {
            this.spd += this.spdPerdidoParalisia;
            this.spdPerdidoParalisia = 0;
        }
        this.paralisado = p;
    } 
    public boolean isParalisado() { return paralisado; }

    public void diminuirPrecisao(int valor) { 
        this.precisao = Math.max(PRECISAO_MINIMA, this.precisao - valor); 
        if(!Batalha.modoTeste) System.out.println("Efeito de Golpe: A Precisão de " + this.nome + " Caiu Para " + this.precisao + "%!");
    }

    public void processarStatusFimDeTurno(RegistroBatalha registro) {
        if (queimado) {
            int d = Math.max(1, maxHp / 16);
            this.hp = Math.max(0, this.hp - d);
            if(!Batalha.modoTeste) System.out.println(nome + " Sofreu " + d + " de Dano por Queimadura! HP: " + this.hp);
            registro.registrarEfeitoStatus(nome, "Dano Queimadura");
        }
        if (envenenado) {
            turnosEnvenenado++;
            int d = Math.max(1, (maxHp / 16) * turnosEnvenenado);
            this.hp = Math.max(0, this.hp - d);
            if(!Batalha.modoTeste) System.out.println(nome + " Sofreu " + d + " de Dano por Veneno Acumulado! HP: " + this.hp);
            registro.registrarEfeitoStatus(nome, "Dano Veneno");
        }
    }

    public void dano(int danoBruto) {
        // Reduz a mitigação para que a Defesa afete fortemente o dano tomado.
        // Se def for menor, a mitigação cai e o dano final sobe de forma expressiva.
        int mitigacao = this.def / 3; 
        int danoEfetivo = Math.max(2, danoBruto - mitigacao);
        int danoFinal = Math.min(danoEfetivo, (int)(this.maxHp * 0.85));
        
        this.hp = Math.max(0, this.hp - danoFinal);
        if(!Batalha.modoTeste) {
            System.out.println(this.nome + " recebeu " + danoFinal + " de dano! (Bruto: " + danoBruto + ", Defesa atual do alvo: " + this.def + ") | Vida restante: " + this.hp + "/" + this.maxHp);
        }
    }
    public void curar(double pct) { this.hp = Math.min(this.maxHp, this.hp + (int)(this.maxHp * pct)); }

    /** Remove queimadura, veneno e paralisia (devolvendo a velocidade perdida). Não mexe no HP nem em buffs. */
    public void curarStatus() {
        setParalisado(false); // passa pelo setter justamente para devolver a velocidade
        this.queimado = false; this.envenenado = false; this.turnosEnvenenado = 0;
    }

    /** Volta tudo ao estado de "recém-criado": HP cheio, atributos originais, precisão 100% e sem status. */
    public void restaurar() {
        curarStatus(); // antes de restaurar a velocidade, para não somar a perda da paralisia por cima
        this.atk = atkBase; this.def = defBase; this.spd = spdBase;
        this.precisao = PRECISAO_MAXIMA;
        this.hp = maxHp;
    }

    public void exibirSts() {
        if(!Batalha.modoTeste) {
            System.out.println("--- Status de " + nome + " ---");
            System.out.println("Tipo: " + tipo.getNomeTipo() + " | Vida: " + hp + "/" + maxHp);
            System.out.println("Atk: " + atk + " | Def: " + def + " | Spd: " + spd);
            System.out.println("--------------------------");
        }
    }
}
