package net.bdew.pressure.compat.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.blockentity.TankModuleBlockEntity;
import net.bdew.pressure.blocks.router.RouterRedstoneMode;
import net.bdew.pressure.api.PressureAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** Legacy data-port command contract. All access runs on the authoritative server thread. */
public final class TankPeripheral implements IPeripheral {
    private static final List<String> OUTPUTS = List.of("red", "green", "blue", "yellow", "cyan", "purple");
    private final TankModuleBlockEntity module;
    public TankPeripheral(TankModuleBlockEntity module) { this.module = module; }
    @Override public String getType() { return "pp_tank"; }
    @Override public Object getTarget() { return module; }
    @Override public boolean equals(IPeripheral other) { return other instanceof TankPeripheral p && p.module == module; }

    private TankControllerBlockEntity core() throws LuaException {
        var core = module.isRemoved() ? null : module.tankController();
        if (core == null) throw new LuaException("Tank is not connected");
        return core;
    }

    @LuaFunction(mainThread = true)
    public boolean isConnected() { return !module.isRemoved() && module.tankController() != null; }
    @LuaFunction(mainThread = true)
    public int getCapacity() throws LuaException { return core().getTank().getCapacity(); }
    @LuaFunction(mainThread = true)
    public boolean hasFluid() throws LuaException { return !core().getTank().isEmpty(); }
    @LuaFunction(mainThread = true)
    public Object getFluid() throws LuaException {
        var fluid = core().getTank().getFluid();
        return fluid.isEmpty() ? null : Map.of("name", ForgeRegistries.FLUIDS.getKey(fluid.getFluid()).toString(), "amount", fluid.getAmount());
    }
    @LuaFunction(mainThread = true)
    public Map<String, Object> getOutputs() throws LuaException {
        var core = core();
        Map<String, Object> result = new LinkedHashMap<>();
        for (int slot = 0; slot < OUTPUTS.size(); slot++) {
            var output = core.getOutput(slot);
            result.put(OUTPUTS.get(slot), output == null ? Map.of("type", "unconnected")
                    : Map.of("type", "fluid", "mode", output.mode.name(), "average", output.average));
        }
        return result;
    }
    @LuaFunction(mainThread = true)
    public boolean setOutputMode(String name, String mode) throws LuaException {
        int slot = OUTPUTS.indexOf(name.toLowerCase(Locale.ROOT));
        if (slot < 0) throw new LuaException("Invalid output name");
        RouterRedstoneMode value;
        try { value = RouterRedstoneMode.valueOf(mode.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) { throw new LuaException("Invalid output mode"); }
        if (!core().setOutputMode(slot, value)) throw new LuaException("Unable to set output state");
        return true;
    }
    @LuaFunction(mainThread = true)
    public String getFilter() throws LuaException {
        var filter = core().getFluidFilter();
        return filter == null ? null : ForgeRegistries.FLUIDS.getKey(filter).toString();
    }
    @LuaFunction(mainThread = true)
    public boolean setFilter(IArguments arguments) throws LuaException {
        var core = core();
        var filter = core.getCapability(PressureAPI.FILTERABLE).orElseThrow(() -> new IllegalStateException("Missing tank filter capability"));
        var name = arguments.optString(0);
        if (name.isEmpty()) filter.clearFluidFilter();
        else {
            ResourceLocation id = ResourceLocation.tryParse(name.get());
            if (id == null || !ForgeRegistries.FLUIDS.containsKey(id)) throw new LuaException("Unknown fluid");
            var fluid = ForgeRegistries.FLUIDS.getValue(id);
            if (fluid == null || fluid == Fluids.EMPTY) throw new LuaException("Unknown fluid");
            filter.setFluidFilter(fluid);
        }
        return true;
    }
    @LuaFunction(mainThread = true)
    public Object getCountsIn() throws LuaException { return fluidValues(core().getCountsIn(), "count"); }
    @LuaFunction(mainThread = true)
    public Object getCountsOut() throws LuaException { return fluidValues(core().getCountsOut(), "count"); }

    // A Lua sequence of records retains the legacy name/count (or name/average) data shape.
    static Map<Integer, Object> fluidValues(Map<String, ? extends Number> values, String field) {
        Map<Integer, Object> result = new LinkedHashMap<>();
        int index = 1;
        for (var entry : new TreeMap<>(values).entrySet())
            result.put(index++, Map.of("name", entry.getKey(), field, entry.getValue()));
        return result;
    }
}
