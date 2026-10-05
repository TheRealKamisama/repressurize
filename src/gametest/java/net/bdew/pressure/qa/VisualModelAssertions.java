package net.bdew.pressure.qa;

import net.bdew.pressure.config.PressureContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraftforge.client.model.data.ModelData;
import java.util.List;

/** Fresh-client bake checks supplement the screenshots; they do not replace visual comparisons. */
public final class VisualModelAssertions {
    private static void check(List<BakedQuad> quads,String name){
        for(var quad:quads)if(quad.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()))
            throw new IllegalStateException("Missing baked texture in "+name);
    }
    public static void verify(){
        var mc=Minecraft.getInstance();int states=0,items=0;
        for(var entry:PressureContent.BLOCKS.getEntries())for(var state:entry.get().getStateDefinition().getPossibleStates()){
            if(state.getRenderShape()!=RenderShape.MODEL)throw new IllegalStateException("Invisible block "+entry.getId());
            var model=mc.getBlockRenderer().getBlockModel(state);
            check(model.getQuads(state,null,RandomSource.create(0),ModelData.EMPTY,null),state.toString());
            for(Direction side:Direction.values())check(model.getQuads(state,side,RandomSource.create(0),ModelData.EMPTY,null),state.toString());
            states++;
        }
        for(var entry:PressureContent.ITEMS.getEntries()){
            ItemStack stack=new ItemStack(entry.get());var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
            check(model.getQuads(null,null,RandomSource.create(0)),entry.getId().toString());
            for(Direction side:Direction.values())check(model.getQuads(null,side,RandomSource.create(0)),entry.getId().toString());
            if(!net.minecraft.client.resources.language.I18n.exists(stack.getDescriptionId()))throw new IllegalStateException("Missing item translation "+stack.getDescriptionId());
            items++;
        }
        System.out.println("PRESSURE_MODELS_QA_PASSED states="+states+" items="+items);
    }
}
