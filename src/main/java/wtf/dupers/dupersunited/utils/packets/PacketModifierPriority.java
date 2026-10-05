package wtf.dupers.dupersunited.utils.packets;

public enum PacketModifierPriority {

    MONITOR(5),
    HIGHEST(4),
    HIGH(3),
    NORMAL(2),
    LOW(1),
    LOWEST(0);

    private int priority;
    PacketModifierPriority(int priority) {
        this.priority = priority;
    }
    public int value() {
        return this.priority;
    }
}