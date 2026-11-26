package dev.emerald.funnel;

/** Independent wing and full-burst deployment modes. */
enum FreedomMode {
    CLOSED("収納"), HIMAT("ハイマットモード"), FULL_BURST("フルバーストモード");
    private final String label;
    FreedomMode(String label){this.label=label;}
    String label(){return label;}
    boolean wingsOpen(){return this!=CLOSED;}
    boolean fullBurst(){return this==FULL_BURST;}
    FreedomMode next(boolean sneaking){
        if(sneaking)return switch(this){
            case CLOSED -> HIMAT;
            case HIMAT -> FULL_BURST;
            case FULL_BURST -> HIMAT;
        };
        return this==CLOSED?HIMAT:CLOSED;
    }
}
