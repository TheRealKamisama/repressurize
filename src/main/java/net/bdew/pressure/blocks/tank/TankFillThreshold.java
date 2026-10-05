package net.bdew.pressure.blocks.tank;

public enum TankFillThreshold {
    EMPTY,
    NON_EMPTY,
    GT_5,
    GT_25,
    GT_50,
    GT_75,
    GT_95,
    NON_FULL,
    FULL;

    public TankFillThreshold next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public boolean matches(int amount, int capacity) {
        return switch (this) {
            case EMPTY -> amount <= 0;
            case NON_EMPTY -> amount > 0;
            case GT_5 -> percentAtLeast(amount, capacity, 5);
            case GT_25 -> percentAtLeast(amount, capacity, 25);
            case GT_50 -> percentAtLeast(amount, capacity, 50);
            case GT_75 -> percentAtLeast(amount, capacity, 75);
            case GT_95 -> percentAtLeast(amount, capacity, 95);
            case NON_FULL -> capacity > 0 && amount < capacity;
            case FULL -> capacity > 0 && amount >= capacity;
        };
    }

    private static boolean percentAtLeast(int amount, int capacity, int percent) {
        return capacity > 0 && (long) amount * 100 >= (long) capacity * percent;
    }

    public String translationKey() {
        return switch (this) {
            case EMPTY -> "pressure.sensor.param.fill.empty";
            case NON_EMPTY -> "pressure.sensor.param.fill.not.empty";
            case GT_5 -> "pressure.sensor.param.fill.gt5";
            case GT_25 -> "pressure.sensor.param.fill.gt25";
            case GT_50 -> "pressure.sensor.param.fill.gt50";
            case GT_75 -> "pressure.sensor.param.fill.gt75";
            case GT_95 -> "pressure.sensor.param.fill.gt95";
            case NON_FULL -> "pressure.sensor.param.fill.not.full";
            case FULL -> "pressure.sensor.param.fill.full";
        };
    }
}
