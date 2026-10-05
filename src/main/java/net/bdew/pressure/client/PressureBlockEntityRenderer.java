package net.bdew.pressure.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bdew.pressure.blockentity.*;
import net.bdew.pressure.blocks.DirectionalMachineBlock;
import net.bdew.pressure.blocks.tank.TankStructure;
import net.bdew.pressure.config.PressureContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** Dynamic visuals use synchronized block entity data; no shared mutable item/model state. */
public final class PressureBlockEntityRenderer<T extends PressureNodeBlockEntity> implements BlockEntityRenderer<T> {
    private static final int[] COLORS={0xFFFF0000,0xFF00FF00,0xFF0000FF,0xFFFFFF00,0xFF00FFFF,0xFFFF00FF};
    public PressureBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(T entity, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (entity.getLevel()==null) return;
        var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        if (entity instanceof RouterBlockEntity router) {
            for (Direction side:Direction.values()) {
                String texture=switch(router.getSideMode(side)) {
                    case DISABLED -> "closed_over"; case INPUT_PASSIVE -> "input_over";
                    case INPUT_ACTIVE -> "input_act_over"; case OUTPUT_LOW -> "output_over_low";
                    case OUTPUT_MEDIUM -> "output_over_med"; case OUTPUT_HIGH -> "output_over_high";
                };
                quad(pose,consumer,side,0,0,1,1,0.001F,sprite("block/router/"+texture),COLORS[side.ordinal()],light);
                var filter=router.getSideFilter(side);
                if (filter!=null) fluid(pose,consumer,side,5.5F/16,5.5F/16,10.5F/16,10.5F/16,new FluidStack(filter,1),light);
            }
            return;
        }
        if (entity instanceof TankModuleBlockEntity module) {
            if (module.moduleName().equals("tank_indicator")) { indicator(module,pose,consumer,light); return; }
            TankControllerBlockEntity core=module.tankController();
            if (core!=null) {
                for (int i=0;i<6;i++) {
                    var output=core.getOutput(i);
                    if (output!=null && output.pos.equals(entity.getBlockPos())) {
                        quad(pose,consumer,output.side,0,0,1,1,0.001F,sprite("block/connected/output"),COLORS[i],light);
                        for (Direction face : Direction.values()) if (face.getAxis() != output.side.getAxis()) {
                            Direction top = face == Direction.UP ? Direction.NORTH : face == Direction.DOWN ? Direction.SOUTH : Direction.UP;
                            Direction right = switch (face) {
                                case NORTH -> Direction.WEST; case SOUTH, UP, DOWN -> Direction.EAST;
                                case EAST -> Direction.NORTH; case WEST -> Direction.SOUTH;
                            };
                            int turn = output.side == top ? 0 : output.side == right ? 1 : output.side == top.getOpposite() ? 2 : 3;
                            quad(pose,consumer,face,0,0,1,1,0.001F,sprite("block/connected/arrow"),COLORS[i],light,turn);
                        }
                    }
                }
            }
        }
        var filter=entity.getFluidFilter();
        if (entity instanceof TankModuleBlockEntity module) {
            if (!module.moduleName().equals("tank_filter")) return;
            var core=module.tankController(); filter=core==null?null:core.getFluidFilter();
        }
        if (entity instanceof TankControllerBlockEntity || filter==null) return;
        for (Direction side:Direction.values()) {
            var state=entity.getBlockState();
            if (state.getBlock() instanceof DirectionalMachineBlock && state.hasProperty(DirectionalMachineBlock.FACING)) {
                Direction facing=state.getValue(DirectionalMachineBlock.FACING);
                if(side==facing || side==facing.getOpposite()) continue;
            }
            fluid(pose,consumer,side,5.5F/16,5.5F/16,10.5F/16,10.5F/16,new FluidStack(filter,1),light);
        }
    }

    private static void indicator(TankModuleBlockEntity module,PoseStack pose,VertexConsumer consumer,int light) {
        var core=module.tankController();
        if(core==null) return;
        var tank=core.getTank();
        for(Direction side:Direction.Plane.HORIZONTAL) {
            if(core.getBlockPos().equals(TankStructure.owner(module.getLevel(),module.getBlockPos().relative(side)))) continue;
            int below=column(module,side,Direction.DOWN),above=column(module,side,Direction.UP);
            String texture=below>0 ? (above>0?"full":"up") : (above>0?"down":"single");
            quad(pose,consumer,side,0,0,1,1,0.0002F,sprite("block/tank/indicator/"+texture),0xFFFFFFFF,light);
            if(tank.isEmpty() || tank.getCapacity()<=0) continue;
            float low=below>0?0:0.125F,high=above>0?1:0.875F;
            float fraction=Math.max(0,Math.min(1,(float)tank.getFluidAmount()/tank.getCapacity()*(above+below+1)-below));
            if(fraction>0) fluid(pose,consumer,side,7F/16,low,9F/16,low+(high-low)*fraction,tank.getFluid(),0xF000F0);
        }
    }
    private static int column(TankModuleBlockEntity module,Direction face,Direction vertical) {
        var level=module.getLevel(); int length=0;
        for(int n=1;n<=200;n++) {
            BlockPos p=module.getBlockPos().relative(vertical,n);
            if(!level.hasChunkAt(p) || !level.getBlockState(p).is(PressureContent.TANK_INDICATOR.get())
                    || !module.getControllerPos().equals(TankStructure.owner(level,p))
                    || module.getControllerPos().equals(TankStructure.owner(level,p.relative(face)))) break;
            length++;
        }
        return length;
    }
    private static TextureAtlasSprite sprite(String path) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("pressure",path));
    }
    private static void fluid(PoseStack pose,VertexConsumer consumer,Direction side,float u1,float v1,float u2,float v2,FluidStack fluid,int light) {
        var extension=IClientFluidTypeExtensions.of(fluid.getFluid());
        var texture=extension.getStillTexture(fluid); if(texture==null)return;
        quad(pose,consumer,side,u1,v1,u2,v2,0.002F,Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture),extension.getTintColor(fluid),light);
    }
    private static void quad(PoseStack pose,VertexConsumer consumer,Direction face,float u1,float v1,float u2,float v2,float offset,TextureAtlasSprite sprite,int color,int light) {
        quad(pose,consumer,face,u1,v1,u2,v2,offset,sprite,color,light,0);
    }
    private static void quad(PoseStack pose,VertexConsumer consumer,Direction face,float u1,float v1,float u2,float v2,float offset,TextureAtlasSprite sprite,int color,int light,int rotation) {
        Direction right=switch(face){case NORTH->Direction.WEST;case SOUTH,UP,DOWN->Direction.EAST;case EAST->Direction.NORTH;case WEST->Direction.SOUTH;};
        Direction up=face==Direction.UP?Direction.NORTH:face==Direction.DOWN?Direction.SOUTH:Direction.UP;
        float[][] coords={{u1,v1},{u2,v1},{u2,v2},{u1,v2}};
        for(float[] uv:coords) {
            float x=0.5F+face.getStepX()*(0.5F+offset)+right.getStepX()*(uv[0]-0.5F)+up.getStepX()*(uv[1]-0.5F);
            float y=0.5F+face.getStepY()*(0.5F+offset)+right.getStepY()*(uv[0]-0.5F)+up.getStepY()*(uv[1]-0.5F);
            float z=0.5F+face.getStepZ()*(0.5F+offset)+right.getStepZ()*(uv[0]-0.5F)+up.getStepZ()*(uv[1]-0.5F);
            float u=uv[0],v=1-uv[1];
            for(int turn=0;turn<rotation;turn++){float previous=u;u=v;v=1-previous;}
            consumer.vertex(pose.last().pose(),x,y,z).color(color>>16&255,color>>8&255,color&255,color>>>24)
                    .uv(sprite.getU(u*16),sprite.getV(v*16)).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light).normal(pose.last().normal(),face.getStepX(),face.getStepY(),face.getStepZ()).endVertex();
        }
    }
}
