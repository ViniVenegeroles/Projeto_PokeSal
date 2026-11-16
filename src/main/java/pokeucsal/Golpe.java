package pokeucsal;

/** Golpe de um Pokésal. As subclasses definem o que cada tipo de golpe faz. */
public abstract class Golpe {
    public static final double CHANCE_CRITICO = 0.0625;
    public static final double MULTIPLICADOR_CRITICO = 1.5;
    public static final double CHANCE_STATUS = 0.125;

    protected final String nome;
    protected final int poderBase;

    /**
     * Cria um golpe.
     *
     * @param nome nome do golpe
     * @param poderBase dano base do golpe
     */
    protected Golpe(String nome, int poderBase) {
        this.nome = nome;
        this.poderBase = poderBase;
    }
    /** Devolve o nome do golpe. */
    public String getNome() {
        return nome;
    }

    /**
     * Testa a precisão do atacante.
     *
     * @param atacante quem ataca
     * @param gerador sorteio da precisão
     * @return true se o golpe errou
     */
    protected boolean errou(Pokesal atacante, GeradorAleatorio gerador) {
        return gerador.sortearPrecisao() >= atacante.getPrecisao();
    }

    /**
     * Anuncia o ataque no console e o registra.
     *
     * @param atk quem ataca
     * @param reg destino do evento de ataque
     */
    protected void anunciar(Pokesal atk, RegistroBatalha reg) {
        if(!Batalha.modoTeste) {
            System.out.println("\n>>> " + atk.getNome() + " usou " + nome + "!");
        }
        reg.registrarAtaque(atk.getNome(), nome);
    }

    /**
     * Calcula e aplica o dano: testa a precisão, o crítico (x1.5), o multiplicador elemental e o bônus de terreno.
     *
     * @param atk quem ataca
     * @param def quem recebe
     * @param clima terreno atual
     * @param ger sorteio de precisão e crítico
     * @param reg destino dos eventos
     * @param elemental true para aplicar vantagem de tipo e bônus de terreno
     * @return false se o golpe errou
     */
    protected boolean causarDano(Pokesal atk, Pokesal def, String clima, GeradorAleatorio ger, RegistroBatalha reg, boolean elemental) {
        if (errou(atk, ger)) {
            if(!Batalha.modoTeste) {
                System.out.println("O Ataque Falhou!");
            }
            return false;
        }

        double mult = 1.0;
        if (elemental) {
            mult = atk.getTipo().calcMult(def.getTipo());
            if(!Batalha.modoTeste) {
                if (mult > 1.0) {
                    System.out.println("É Super Efetivo!");
                } else if (mult < 1.0) {
                    System.out.println("Não é Muito Efetivo...");
                }
            }
        }

        boolean critico = ger.sortearChance() <= CHANCE_CRITICO;
        int danoBase = poderBase + (atk.getAtk() / 2);
        if (critico) {
            danoBase = (int) (danoBase * MULTIPLICADOR_CRITICO);
            if(!Batalha.modoTeste) {
                System.out.println("UM ACERTO CRÍTICO!");
            }
        }

        double danoCalc = danoBase * mult;
        if (elemental) {
            danoCalc *= bonusDeTerreno(atk, clima);
        }

        int hpAntigo = def.getHp();
        def.dano((int) danoCalc);
        reg.registrarDano(def.getNome(), hpAntigo - def.getHp());

        if (critico) {
            reg.registrarEfeitoStatus(atk.getNome(), "Acerto Crítico!");
        }
        return true;
    }

    private double bonusDeTerreno(Pokesal atk, String clima) {
        String tipoAtk = atk.getTipo().getNomeTipo();
        if (Terrenos.ASFALTO_QUENTE.equals(clima) && tipoAtk.equals("Fogo")) {
            if(!Batalha.modoTeste) {
                System.out.println("[TERRENO] O Asfalto Quente Potencializou o Golpe!");
            }
            return 1.15;
        }
        if (Terrenos.PISO_ESCORREGADIO.equals(clima) && tipoAtk.equals("Água")) {
            if(!Batalha.modoTeste) {
                System.out.println("[TERRENO] A Poça de Chuva Amplificou o Ataque!");
            }
            return 1.10;
        }
        return 1.0;
    }

    /**
     * Executa o golpe.
     *
     * @param atacante quem ataca
     * @param defensor quem recebe
     * @param climaAtual terreno atual
     * @param gerador sorteio de precisão, crítico e status
     * @param registro destino dos eventos
     */
    public abstract void executar(Pokesal atacante, Pokesal defensor, String climaAtual, GeradorAleatorio gerador, RegistroBatalha registro);

    /** Golpe físico sem tipo: dano igual ao poder base mais metade do ataque. */
    public static class Normal extends Golpe {
        /**
         * Cria um golpe normal.
         *
         * @param nome nome do golpe
         * @param poderBase dano base
         */
        public Normal(String nome, int poderBase) {
            super(nome, poderBase);
        }
        @Override
        public void executar(Pokesal atk, Pokesal def, String clima, GeradorAleatorio ger, RegistroBatalha reg) {
            anunciar(atk, reg);
            causarDano(atk, def, clima, ger, reg, false);
        }
    }

    /**
     * Golpe do tipo do atacante, com vantagem de tipo, bônus de terreno e chance de causar status.
     */
    public static class Elemental extends Golpe {
        /**
         * Cria um golpe elemental.
         *
         * @param nome nome do golpe
         * @param poderBase dano base
         */
        public Elemental(String nome, int poderBase) {
            super(nome, poderBase);
        }
        @Override
        public void executar(Pokesal atk, Pokesal def, String clima, GeradorAleatorio ger, RegistroBatalha reg) {
            anunciar(atk, reg);
            if (!causarDano(atk, def, clima, ger, reg, true)) {
                return;
            }

            if (def.getHp() > 0 && ger.sortearChance() < CHANCE_STATUS) {
                String tAtk = atk.getTipo().getNomeTipo();
                if (tAtk.equals("Fogo") && !def.isQueimado()) {
                    def.setQueimado(true);
                    reg.registrarEfeitoStatus(def.getNome(), "Queimado");
                } else if (tAtk.equals("Planta") && !def.isEnvenenado()) {
                    def.setEnvenenado(true);
                    reg.registrarEfeitoStatus(def.getNome(), "Envenenado");
                } else if (tAtk.equals("Água") && !def.isParalisado()) {
                    def.setParalisado(true);
                    reg.registrarEfeitoStatus(def.getNome(), "Paralisado");
                }
            }
        }
    }

    /**
     * Golpe de suporte: não causa dano e aplica o buff do tipo de quem usa, ao custo de precisão.
     */
    public static class Buff extends Golpe {
        /**
         * Cria um golpe de suporte.
         *
         * @param nome nome do golpe
         */
        public Buff(String nome) {
            super(nome, 0);
        }
        @Override
        public void executar(Pokesal atk, Pokesal def, String clima, GeradorAleatorio ger, RegistroBatalha reg) {
            anunciar(atk, reg);
            atk.getTipo().apBf(atk);
            atk.diminuirPrecisao(10);
            reg.registrarEfeitoStatus(atk.getNome(), "Buff de Tipo");
        }
    }

    /** Golpe que causa dano e reduz um atributo do alvo conforme o tipo de quem usa. */
    public static class Debuff extends Golpe {
        /**
         * Cria um golpe de enfraquecimento.
         *
         * @param nome nome do golpe
         * @param poderBase dano base
         */
        public Debuff(String nome, int poderBase) {
            super(nome, poderBase);
        }
        @Override
        public void executar(Pokesal atk, Pokesal def, String clima, GeradorAleatorio ger, RegistroBatalha reg) {
            anunciar(atk, reg);
            if (!causarDano(atk, def, clima, ger, reg, true)) {
                return;
            }

            if (def.getHp() > 0) {
                atk.getTipo().apDb(def);
                reg.registrarEfeitoStatus(def.getNome(), "Debuff de Tipo");
            }
        }
    }
}
