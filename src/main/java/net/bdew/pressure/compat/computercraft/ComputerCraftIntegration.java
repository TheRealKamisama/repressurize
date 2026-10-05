package net.bdew.pressure.compat.computercraft;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.bdew.pressure.blockentity.PipeSensorBlockEntity;
import net.bdew.pressure.blockentity.TankModuleBlockEntity;
import net.minecraftforge.common.util.LazyOptional;

/** Loaded only when CC: Tweaked is installed. Providers are queried for all six sides. */
public final class ComputerCraftIntegration {
    private ComputerCraftIntegration() {}

    public static void register() {
        ForgeComputerCraftAPI.registerPeripheralProvider((level, pos, side) -> {
            var entity = level.getBlockEntity(pos);
            IPeripheral peripheral;
            if (entity instanceof PipeSensorBlockEntity sensor) peripheral = new SensorPeripheral(sensor);
            else if (entity instanceof TankModuleBlockEntity module && module.moduleName().equals("tank_data_port"))
                peripheral = new TankPeripheral(module);
            else return LazyOptional.empty();
            return LazyOptional.of(() -> peripheral);
        });
    }
}
