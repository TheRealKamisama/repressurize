package net.bdew.pressure.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PressureConfig {
    private PressureConfig() {}

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue FLUID_TRANSFER_PER_TICK;
    public static final ForgeConfigSpec.IntValue TANK_CAPACITY_PER_BLOCK;
    public static final ForgeConfigSpec.IntValue CANISTER_CAPACITY;
    public static final ForgeConfigSpec.IntValue CANISTER_MAX_POUR;
    public static final ForgeConfigSpec.IntValue HAND_PUMP_MAX_DRAIN;
    public static final ForgeConfigSpec.BooleanValue SHOW_CANISTERS;
    public static final ForgeConfigSpec.BooleanValue DRAIN_XP;
    public static final ForgeConfigSpec.IntValue XP_JUICE_RATIO, MOB_ESSENCE_RATIO;
    public static final java.util.Map<String, ForgeConfigSpec.IntValue> TANK_MODULE_LIMITS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("pressure");
        DRAIN_XP = builder.comment("Convert supported XP fluids to experience orbs at the drain.").define("drainMakesXp", true);
        XP_JUICE_RATIO = builder.defineInRange("xpJuiceMbPerXp", 20, 1, Integer.MAX_VALUE);
        MOB_ESSENCE_RATIO = builder.defineInRange("mobEssenceMbPerXp", 125, 1, Integer.MAX_VALUE);
        builder.push("tankModuleLimits");
        java.util.Map<String, ForgeConfigSpec.IntValue> moduleLimits = new java.util.LinkedHashMap<>();
        for (var entry : java.util.Map.of("tank", 200, "input", 10, "output", 6, "filter", 1,
                "interface", 10, "sensor", 10, "data", 5, "access", 5).entrySet())
            moduleLimits.put(entry.getKey(), builder.defineInRange(entry.getKey(), entry.getValue(), 0, Integer.MAX_VALUE));
        TANK_MODULE_LIMITS = java.util.Collections.unmodifiableMap(moduleLimits);
        builder.pop();
        SHOW_CANISTERS = builder.comment("Show filled canister variants in JEI. The dedicated creative tab always contains them.")
                .define("showCanisters", false);
        FLUID_TRANSFER_PER_TICK = builder.comment("Maximum fluid moved by automatic inputs, pumps, and routers per tick. The default preserves legacy unrestricted throughput.")
                .defineInRange("fluidTransferPerTick", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        TANK_CAPACITY_PER_BLOCK = builder.comment("Tank capacity contributed by each Tank Block, in mB.")
                .defineInRange("tankCapacityPerBlock", 32000, 1, Integer.MAX_VALUE);
        CANISTER_CAPACITY = builder.comment("Canister capacity, in mB.")
                .defineInRange("canisterCapacity", 64000, 1, Integer.MAX_VALUE);
        CANISTER_MAX_POUR = builder.comment("Maximum fluid poured from a canister into a machine at once, in mB.")
                .defineInRange("canisterMaxPour", 4000, 1, Integer.MAX_VALUE);
        HAND_PUMP_MAX_DRAIN = builder.comment("Maximum fluid drained from a machine in one hand-pump use, in mB.")
                .defineInRange("handPumpMaxDrain", 4000, 1, Integer.MAX_VALUE);
        builder.pop();
        COMMON_SPEC = builder.build();
    }
}
