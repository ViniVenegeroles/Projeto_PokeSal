package pokeucsal;

public abstract class Golpe {
    public static final double CHANCE_CRITICO = 0.0625;      // 6,25% (regra 2.1)
    public static final double MULTIPLICADOR_CRITICO = 1.5;  // dano x1.5 (regra 2.1)
    public static final double CHANCE_STATUS = 0.125;        // 12,5% de aplicar queimadura/veneno/paralisia

    protected final String nome;
    protected final int poderBase;

    public Golpe(String nome, int poderBase) {
        this.nome = nome; this.poderBase = poderBase;
    }
    public String getNome() { return nome; }

    protected boolean errou(Pokemon atacante, GeradorAleatorio gerador) {
        return gerador.sortearPrecisao() >= atacante.getPrecisao();
    }

    protected void anunciar(Pokemon atk, RegistroBatalha reg) {
        if(!Batalha.modoTeste) System.out.println("\n>>> " + atk.getNome() + " usou " + nome + "!");
        reg.registrarAtaque(atk.getNome(), nome);
    }

    /**
     * Caminho ÚNICO de dano para Normal, Elemental e Debuff (antes eram três cópias quase iguais,
     * e as cópias já tinham divergido: o crítico era registrado ANTES do dano, quebrando a ordem
     * ataque -> dano -> status -> terreno -> item exigida pela regra 2.4).
     *
     * @param elemental true = aplica vantagem de tipo e bônus de terreno; false = golpe Normal
     * @return true se o golpe acertou
     */
    protected boolean causarDano(Pokemon atk, Pokemon def, String clima, GeradorAleatorio ger, RegistroBatalha reg, boolean elemental) {
        if (errou(atk, ger)) { if(!Batalha.modoTeste) System.out.println("O Ataque Falhou!"); return false; }

        double mult = 1.0;
        if (elemental) {
            mult = atk.getTipo().calcMult(def.getTipo());
            if(!Batalha.modoTeste) {
                if (mult > 1.0) System.out.println("É Super Efetivo!");
                else if (mult < 1.0) System.out.println("Não é Muito Efetivo...");
            }
        }

        boolean critico = ger.sortearChance() <= CHANCE_CRITICO;
        int danoBase = poderBase + (atk.getAtk() / 2);
        if (critico) {
            danoBase = (int) (danoBase * MULTIPLICADOR_CRITICO);
            if(!Batalha.modoTeste) System.out.println("UM ACERTO CRÍTICO!");
        }

        double danoCalc = danoBase * mult;
        if (elemental) danoCalc *= bonusDeTerreno(atk, clima);

        int hpAntigo = def.getHp();
        def.dano((int) danoCalc);
        reg.registrarDano(def.getNome(), hpAntigo - def.getHp());
        // O crítico é um efeito do golpe: registrado DEPOIS do dano, na ordem da regra 2.4.
        if (critico) reg.registrarEfeitoStatus(atk.getNome(), "Acerto Crítico!");
        return true;
    }

    private double bonusDeTerreno(Pokemon atk, String clima) {
        String tipoAtk = atk.getTipo().getNomeTipo();
        if (Terrenos.ASFALTO_QUENTE.equals(clima) && tipoAtk.equals("Fogo")) {
            if(!Batalha.modoTeste) System.out.println("[TERRENO] O Asfalto Quente Potencializou o Golpe!");
            return 1.15;
        }
        if (Terrenos.PISO_ESCORREGADIO.equals(clima) && tipoAtk.equals("Água")) {
            if(!Batalha.modoTeste) System.out.println("[TERRENO] A Poça de Chuva Amplificou o Ataque!");
            return 1.10;
        }
        return 1.0;
    }

    public abstract void executar(Pokemon atacante, Pokemon defensor, String climaAtual, GeradorAleatorio gerador, RegistroBatalha registro);

    public static class Normal extends Golpe {
        public Normal(String nome, int poderBase) { super(nome, poderBase); }
        @Override
        public void executar(Pokemon atk, Pokemon def, String clima, GeradorAleatorio ger, RegistroBatalha reg) {
            anunciar(atk, reg);
            causarDano(atk, def, clima, ger, reg, false);
        }
    }

    public static class Elemental extends Golpe {
        public Elemental(String nome, int poderBase) { super(nome, poderBase); }
        @Override
        public void executar(Pokemon atk, Pokemon def, String clima, GeradorAleatorio ger, RegistroBatalha reg) {
            anunciar(atk, reg);
            if (!causarDano(atk, def, clima, ger, reg, true)) return;

            // Só tenta aplicar status em alvo que ainda está de pé (antes sorteava e "queimava" até quem já tinha desmaiado).
            if (def.getHp() > 0 && ger.sortearChance() < CHANCE_STATUS) {
                String tAtk = atk.getTipo().getNomeTipo();
                if (tAtk.equals("Fogo") && !def.isQueimado()) { def.setQueimado(true); reg.registrarEfeitoStatus(def.getNome(), "Queimado"); }
                else if (tAtk.equals("Planta") && !def.isEnvenenado()) { def.setEnvenenado(true); reg.registrarEfeitoStatus(def.getNome(), "Envenenado"); }
                else if (tAtk.equals("Água") && !def.isParalisado()) { def.setParalisado(true); reg.registrarEfeitoStatus(def.getNome(), "Paralisado"); }
            }
        }
    }

    public static class Buff extends Golpe {
        public Buff(String nome) { super(nome, 0); }
        @Override
        public void executar(Pokemon atk, Pokemon def, String clima, GeradorAleatorio ger, RegistroBatalha reg) {
            anunciar(atk, reg);
            atk.getTipo().apBf(atk);
            atk.diminuirPrecisao(10); // custo fixo do buff (escolha de design do grupo: mantido)
            reg.registrarEfeitoStatus(atk.getNome(), "Buff de Tipo");
        }
    }

    public static class Debuff extends Golpe {
        public Debuff(String nome, int poderBase) { super(nome, poderBase); }
        @Override
        public void executar(Pokemon atk, Pokemon def, String clima, GeradorAleatorio ger, RegistroBatalha reg) {
            anunciar(atk, reg);
            if (!causarDano(atk, def, clima, ger, reg, true)) return;

            // Antes o debuff era aplicado e registrado mesmo se o dano do próprio golpe já tinha derrubado o alvo.
            if (def.getHp() > 0) {
                atk.getTipo().apDb(def);
                reg.registrarEfeitoStatus(def.getNome(), "Debuff de Tipo");
            }
        }
    }
}
