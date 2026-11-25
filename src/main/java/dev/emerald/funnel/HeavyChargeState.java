package dev.emerald.funnel;

/** Charge progress is retained on folding; only a manual shot spends energy. */
final class HeavyChargeState {
    private final int duration, capacity;
    private int progress, shots;
    private boolean active;
    HeavyChargeState(int duration,int capacity) {
        if(duration<1||capacity<1)throw new IllegalArgumentException();
        this.duration=duration;this.capacity=capacity;
    }
    boolean active(){return active;}
    boolean ready(){return shots>0;}
    int shots(){return shots;}
    int percent(){return (int)(100L*progress/duration);}
    void resume(){if(!ready())active=true;}
    void pause(){active=false;}
    boolean advance(){
        if(!active)return false;
        if(++progress>=duration){progress=duration;shots=capacity;active=false;return true;}
        return false;
    }
    void consume(){
        if(!ready())throw new IllegalStateException("not charged");
        if(--shots==0)progress=0;
    }
}
