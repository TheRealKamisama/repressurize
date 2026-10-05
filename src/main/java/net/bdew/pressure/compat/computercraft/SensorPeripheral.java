package net.bdew.pressure.compat.computercraft;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.bdew.pressure.blockentity.PipeSensorBlockEntity;

public final class SensorPeripheral implements IPeripheral {
    private final PipeSensorBlockEntity sensor;
    public SensorPeripheral(PipeSensorBlockEntity sensor) { this.sensor = sensor; }
    @Override public String getType() { return "pp_sensor"; }
    @Override public Object getTarget() { return sensor; }
    @Override public boolean equals(IPeripheral other) { return other instanceof SensorPeripheral p && p.sensor == sensor; }

    @LuaFunction(mainThread = true)
    public Object getAverages() { return TankPeripheral.fluidValues(sensor.getAverages(), "average"); }
    @LuaFunction(mainThread = true)
    public Object getCounts() { return TankPeripheral.fluidValues(sensor.getCounts(), "count"); }
}
