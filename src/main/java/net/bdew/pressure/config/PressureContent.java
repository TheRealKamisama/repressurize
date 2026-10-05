package net.bdew.pressure.config;

import net.bdew.pressure.Pressure;
import net.bdew.pressure.blockentity.CreativeSourceBlockEntity;
import net.bdew.pressure.blockentity.CheckValveBlockEntity;
import net.bdew.pressure.blockentity.DrainBlockEntity;
import net.bdew.pressure.blockentity.InputBlockEntity;
import net.bdew.pressure.blockentity.OutputBlockEntity;
import net.bdew.pressure.blockentity.PumpBlockEntity;
import net.bdew.pressure.blockentity.PipeSensorBlockEntity;
import net.bdew.pressure.blockentity.RouterBlockEntity;
import net.bdew.pressure.blockentity.SluiceBlockEntity;
import net.bdew.pressure.blockentity.TankControllerBlockEntity;
import net.bdew.pressure.blockentity.TankModuleBlockEntity;
import net.bdew.pressure.blockentity.WaterSourceBlockEntity;
import net.bdew.pressure.blocks.CheckValveBlock;
import net.bdew.pressure.blocks.DrainBlock;
import net.bdew.pressure.blocks.InputBlock;
import net.bdew.pressure.blocks.OutputBlock;
import net.bdew.pressure.blocks.PipeBlock;
import net.bdew.pressure.blocks.PipeSensorBlock;
import net.bdew.pressure.blocks.PumpBlock;
import net.bdew.pressure.blocks.RouterBlock;
import net.bdew.pressure.blocks.SluiceBlock;
import net.bdew.pressure.blocks.SourceBlock;
import net.bdew.pressure.blocks.SimpleTankBlock;
import net.bdew.pressure.blocks.TankControllerBlock;
import net.bdew.pressure.blocks.TankModuleBlock;
import net.bdew.pressure.blocks.TankSensorModuleBlock;
import net.bdew.pressure.items.ConfiguratorItem;
import net.bdew.pressure.items.HandPumpItem;
import net.bdew.pressure.items.CanisterItem;
import net.bdew.pressure.items.DebuggerItem;
import net.bdew.pressure.blocks.router.RouterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.function.Supplier;
import java.util.Arrays;

public final class PressureContent {
    private PressureContent() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Pressure.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Pressure.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Pressure.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Pressure.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Pressure.MOD_ID);

    public static final RegistryObject<PipeBlock> PIPE = block("pipe", PipeBlock::new);
    public static final RegistryObject<InputBlock> INPUT = block("input", InputBlock::new);
    public static final RegistryObject<OutputBlock> OUTPUT = block("output", OutputBlock::new);
    public static final RegistryObject<PumpBlock> PUMP = block("pump", PumpBlock::new);
    public static final RegistryObject<Block> WATER_SOURCE = block("water_source", () -> new SourceBlock(false));
    public static final RegistryObject<Block> CREATIVE_SOURCE = block("creative_source", () -> new SourceBlock(true));
    public static final RegistryObject<DrainBlock> DRAIN = block("drain", DrainBlock::new);
    public static final RegistryObject<SluiceBlock> SLUICE = block("sluice", SluiceBlock::new);
    public static final RegistryObject<CheckValveBlock> CHECK_VALVE = block("check_valve", CheckValveBlock::new);
    public static final RegistryObject<PipeSensorBlock> PIPE_SENSOR = block("pipe_sensor", PipeSensorBlock::new);
    public static final RegistryObject<RouterBlock> ROUTER = block("router", RouterBlock::new);
    public static final RegistryObject<Block> TANK_CONTROLLER = block("tank_controller", TankControllerBlock::new);
    public static final RegistryObject<Block> TANK_BLOCK = block("tank_block", SimpleTankBlock::new);
    public static final RegistryObject<Block> TANK_INDICATOR = tankModule("tank_indicator", false);
    public static final RegistryObject<Block> TANK_FLUID_OUTPUT = tankModule("tank_fluid_output", false);
    public static final RegistryObject<Block> TANK_FLUID_OUTPUT_AUTO = tankModule("tank_fluid_output_auto", false);
    public static final RegistryObject<Block> TANK_FLUID_INPUT = tankModule("tank_fluid_input", false);
    public static final RegistryObject<Block> TANK_FLUID_ACCESS = tankModule("tank_fluid_access", false);
    public static final RegistryObject<Block> TANK_PRESSURE_OUTPUT = tankModule("tank_pressure_output", true);
    public static final RegistryObject<Block> TANK_PRESSURE_INPUT = tankModule("tank_pressure_input", true);
    public static final RegistryObject<Block> TANK_INTERFACE = tankModule("tank_interface", false);
    public static final RegistryObject<Block> TANK_FILTER = tankModule("tank_filter", false);
    public static final RegistryObject<Block> TANK_SENSOR = block("tank_sensor", TankSensorModuleBlock::new);
    public static final RegistryObject<Block> TANK_DATA_PORT = tankModule("tank_data_port", false);

    public static final RegistryObject<BlockEntityType<InputBlockEntity>> INPUT_BE = blockEntity("input", InputBlockEntity::new, INPUT);
    public static final RegistryObject<BlockEntityType<OutputBlockEntity>> OUTPUT_BE = blockEntity("output", OutputBlockEntity::new, OUTPUT);
    public static final RegistryObject<BlockEntityType<PumpBlockEntity>> PUMP_BE = blockEntity("pump", PumpBlockEntity::new, PUMP);
    public static final RegistryObject<BlockEntityType<WaterSourceBlockEntity>> WATER_SOURCE_BE = blockEntity("water_source", WaterSourceBlockEntity::new, WATER_SOURCE);
    public static final RegistryObject<BlockEntityType<CreativeSourceBlockEntity>> CREATIVE_SOURCE_BE = blockEntity("creative_source", CreativeSourceBlockEntity::new, CREATIVE_SOURCE);
    public static final RegistryObject<BlockEntityType<DrainBlockEntity>> DRAIN_BE = blockEntity("drain", DrainBlockEntity::new, DRAIN);
    public static final RegistryObject<BlockEntityType<SluiceBlockEntity>> SLUICE_BE = blockEntity("sluice", SluiceBlockEntity::new, SLUICE);
    public static final RegistryObject<BlockEntityType<PipeSensorBlockEntity>> PIPE_SENSOR_BE = blockEntity("pipe_sensor", PipeSensorBlockEntity::new, PIPE_SENSOR);
    public static final RegistryObject<BlockEntityType<CheckValveBlockEntity>> CHECK_VALVE_BE = blockEntity("check_valve", CheckValveBlockEntity::new, CHECK_VALVE);
    public static final RegistryObject<BlockEntityType<RouterBlockEntity>> ROUTER_BE = blockEntity("router", RouterBlockEntity::new, ROUTER);
    public static final RegistryObject<BlockEntityType<TankControllerBlockEntity>> TANK_CONTROLLER_BE = blockEntity("tank_controller", TankControllerBlockEntity::new, TANK_CONTROLLER);
    public static final RegistryObject<BlockEntityType<TankModuleBlockEntity>> TANK_MODULE_BE = blockEntity("tank_module", TankModuleBlockEntity::new,
            TANK_BLOCK, TANK_INDICATOR, TANK_FLUID_OUTPUT, TANK_FLUID_OUTPUT_AUTO, TANK_FLUID_INPUT, TANK_FLUID_ACCESS,
            TANK_PRESSURE_OUTPUT, TANK_PRESSURE_INPUT, TANK_INTERFACE, TANK_FILTER, TANK_SENSOR, TANK_DATA_PORT);

    public static final RegistryObject<MenuType<RouterMenu>> ROUTER_MENU = MENUS.register("router",
            () -> IForgeMenuType.create(RouterMenu::new));
    public static final RegistryObject<MenuType<net.bdew.pressure.blocks.tank.TankMenu>> TANK_MENU = MENUS.register("tank",
            () -> IForgeMenuType.create(net.bdew.pressure.blocks.tank.TankMenu::new));
    public static final RegistryObject<MenuType<net.bdew.pressure.blocks.FilterMenu>> FILTER_MENU = MENUS.register("filter",
            () -> IForgeMenuType.create(net.bdew.pressure.blocks.FilterMenu::new));
    public static final RegistryObject<MenuType<net.bdew.pressure.blocks.tank.SensorMenu>> SENSOR_MENU = MENUS.register("sensor",
            () -> IForgeMenuType.create(net.bdew.pressure.blocks.tank.SensorMenu::new));

    public static final RegistryObject<Item> CANISTER = ITEMS.register("canister", () -> new CanisterItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> HAND_PUMP = ITEMS.register("hand_pump", () -> new HandPumpItem(new Item.Properties().durability(256)));
    public static final RegistryObject<Item> CONFIGURATOR = ITEMS.register("configurator", () -> new ConfiguratorItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DEBUGGER = ITEMS.register("debugger",
            () -> new DebuggerItem(new Item.Properties()));
    public static final RegistryObject<Item> INTERFACE = item("interface");
    public static final RegistryObject<Item> TANK_WALL = item("tank_wall");
    public static final RegistryObject<Item> FLUID_INTERFACE = item("fluid_interface");

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.pressure"))
            .icon(() -> PIPE.get().asItem().getDefaultInstance())
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))
            .build());

    public static final RegistryObject<CreativeModeTab> CANISTER_TAB = TABS.register("canisters", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.pressure_canisters"))
            .icon(() -> CANISTER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(CANISTER.get());
                for (var fluid : ForgeRegistries.FLUIDS.getValues()) {
                    if (fluid == net.minecraft.world.level.material.Fluids.EMPTY
                            || fluid instanceof net.minecraft.world.level.material.FlowingFluid flowing && flowing.getSource() != fluid) continue;
                    net.minecraft.world.item.ItemStack filled = new net.minecraft.world.item.ItemStack(CANISTER.get());
                    filled.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler ->
                            handler.fill(new FluidStack(fluid, PressureConfig.CANISTER_CAPACITY.get()), FluidAction.EXECUTE));
                    output.accept(filled);
                }
            })
            .build());

    private static <B extends Block> RegistryObject<B> block(String name, Supplier<B> supplier) {
        RegistryObject<B> block = BLOCKS.register(name, supplier);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Item> item(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Block> tankModule(String name, boolean pressureNode) {
        return block(name, () -> new TankModuleBlock(pressureNode));
    }

    private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> blockEntity(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, RegistryObject<? extends Block>... blocks) {
        return BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder.of(factory,
                Arrays.stream(blocks).map(RegistryObject::get).toArray(Block[]::new)).build(null));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(PressureContent::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(net.bdew.pressure.api.properties.IFilterable.class);
    }
}
