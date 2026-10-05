package net.bdew.pressure.qa;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Run real Minecraft GameTests against an installed Forge distribution and reobfuscated mod. */
@Mod.EventBusSubscriber(modid="pressureqa")
public final class PackagedServerQA {
    private static final GameTestTicker ticker=new GameTestTicker();
    private static Collection<GameTestInfo> tests;
    private static net.minecraft.server.MinecraftServer server;
    @SubscribeEvent public static void start(ServerStartedEvent event){
        if(!Boolean.getBoolean("pressure.packagedGameTests"))return;
        server=event.getServer();
        GameTestRegistry.register(PressureGameTests.class);GameTestRegistry.register(RevisionGameTests.class);
        if(ModList.get().isLoaded("mekanism"))GameTestRegistry.register(MekanismGameTests.class);
        if(ModList.get().isLoaded("enderio"))GameTestRegistry.register(EnderIOGameTests.class);
        if(ModList.get().isLoaded("computercraft"))GameTestRegistry.register(ComputerCraftGameTests.class);
        var functions=GameTestRegistry.getAllTestFunctions();
        System.out.println("PRESSURE_PACKAGED_TESTS_STARTED count="+functions.size());
        tests=GameTestRunner.runTests(functions,new BlockPos(0,100,0),Rotation.NONE,server.overworld(),ticker,8);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(tests==null || event.phase!=TickEvent.Phase.END)return;
        ticker.tick();
        if(tests.isEmpty() || tests.stream().anyMatch(test->!test.isDone()))return;
        var failures=tests.stream().filter(GameTestInfo::hasFailed).map(test->test.getTestName()+": "+test.getError()).toList();
        tests.stream().filter(GameTestInfo::hasFailed).forEach(test->test.getError().printStackTrace(System.out));
        try{Files.writeString(Path.of("packaged-gametest-results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed",failures.isEmpty(),"tests",tests.size(),"failures",failures)));}
        catch(Exception e){throw new IllegalStateException(e);}
        System.out.println("PRESSURE_PACKAGED_TESTS_COMPLETE count="+tests.size()+" failed="+failures.size());
        tests=null;server.halt(false);
        if(!failures.isEmpty())throw new IllegalStateException("Packaged tests failed: "+failures);
    }
}
