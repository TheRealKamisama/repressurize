package net.bdew.pressure.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.bdew.pressure.blocks.tank.TankStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Frames follow synchronized controller ownership, including concave diagonal corners. */
public final class TankFrameModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Integer> CONNECTIONS = new ModelProperty<>();
    private static final ResourceLocation EDGE = new ResourceLocation("pressure", "block/connected/edge");
    private final Map<Integer, List<BakedQuad>> frames = new ConcurrentHashMap<>();

    public TankFrameModel(BakedModel original) { super(original); }

    private static int bit(int x, int y, int z) { return 1 << ((x + 1) * 9 + (y + 1) * 3 + z + 1); }
    private static boolean connected(int mask, Direction side) {
        return (mask & bit(side.getStepX(), side.getStepY(), side.getStepZ())) != 0;
    }
    private static boolean diagonal(int mask, Direction a, Direction b) {
        return (mask & bit(a.getStepX()+b.getStepX(), a.getStepY()+b.getStepY(), a.getStepZ()+b.getStepZ())) != 0;
    }

    @Override public ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData data) {
        int mask = 0;
        BlockPos owner = TankStructure.owner(world, pos);
        if (owner != null) for (int x=-1; x<=1; x++) for (int y=-1; y<=1; y++) for (int z=-1; z<=1; z++) {
            if (owner.equals(TankStructure.owner(world, pos.offset(x,y,z)))) mask |= bit(x,y,z);
        }
        return super.getModelData(world, pos, state, data).derive().with(CONNECTIONS, mask).build();
    }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
        int mask = data.has(CONNECTIONS) ? data.get(CONNECTIONS) : 0;
        if (side != null && connected(mask, side)) return List.of();
        List<BakedQuad> result = new ArrayList<>(originalModel.getQuads(state, side, random, data, type));
        if (side == null) return result;
        Direction top = side == Direction.UP ? Direction.NORTH : side == Direction.DOWN ? Direction.SOUTH : Direction.UP;
        Direction right = switch (side) {
            case NORTH -> Direction.WEST; case SOUTH, UP, DOWN -> Direction.EAST;
            case EAST -> Direction.NORTH; case WEST -> Direction.SOUTH;
        };
        Direction bottom=top.getOpposite(), left=right.getOpposite();
        boolean t=!connected(mask,top), b=!connected(mask,bottom), l=!connected(mask,left), r=!connected(mask,right);
        int edges=(t?1:0)|(b?2:0)|(l?4:0)|(r?8:0)
                |(!t&&!l&&!diagonal(mask,top,left)?16:0)|(!t&&!r&&!diagonal(mask,top,right)?32:0)
                |(!b&&!l&&!diagonal(mask,bottom,left)?64:0)|(!b&&!r&&!diagonal(mask,bottom,right)?128:0);
        result.addAll(frames.computeIfAbsent(side.ordinal()*256+edges, key -> bake(side,top,right,edges)));
        return result;
    }

    private static List<BakedQuad> bake(Direction face, Direction top, Direction right, int mask) {
        List<BakedQuad> result=new ArrayList<>();
        if ((mask&1)!=0) add(result,face,top,right,0,0,16,1);
        if ((mask&2)!=0) add(result,face,top,right,0,15,16,16);
        if ((mask&4)!=0) add(result,face,top,right,0,1,1,15);
        if ((mask&8)!=0) add(result,face,top,right,15,1,16,15);
        if ((mask&16)!=0) add(result,face,top,right,0,0,1,1);
        if ((mask&32)!=0) add(result,face,top,right,15,0,16,1);
        if ((mask&64)!=0) add(result,face,top,right,0,15,1,16);
        if ((mask&128)!=0) add(result,face,top,right,15,15,16,16);
        // Vertical borders also own their end pixels when the corresponding horizontal border is absent.
        if ((mask&4)!=0 && (mask&1)==0) add(result,face,top,right,0,0,1,1);
        if ((mask&4)!=0 && (mask&2)==0) add(result,face,top,right,0,15,1,16);
        if ((mask&8)!=0 && (mask&1)==0) add(result,face,top,right,15,0,16,1);
        if ((mask&8)!=0 && (mask&2)==0) add(result,face,top,right,15,15,16,16);
        return List.copyOf(result);
    }

    private static void add(List<BakedQuad> list, Direction face, Direction top, Direction right, float u1,float v1,float u2,float v2) {
        Vector3f a=point(face,top,right,u1,v1), b=point(face,top,right,u2,v2);
        Vector3f min=new Vector3f(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.min(a.z,b.z));
        Vector3f max=new Vector3f(Math.max(a.x,b.x),Math.max(a.y,b.y),Math.max(a.z,b.z));
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(EDGE);
        list.add(new FaceBakery().bakeQuad(min,max,new BlockElementFace(null,-1,"#edge",new BlockFaceUV(new float[]{u1,v1,u2,v2},0)),
                sprite,face,BlockModelRotation.X0_Y0,null,true,EDGE));
    }

    private static Vector3f point(Direction face,Direction top,Direction right,float u,float v) {
        return new Vector3f(8+face.getStepX()*8.008F+right.getStepX()*(u-8)+top.getStepX()*(8-v),
                8+face.getStepY()*8.008F+right.getStepY()*(u-8)+top.getStepY()*(8-v),
                8+face.getStepZ()*8.008F+right.getStepZ()*(u-8)+top.getStepZ()*(8-v));
    }

    @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        originalModel.applyTransform(context, pose, leftHand);
        return this;
    }
    @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
}
