package toolbox.client.handler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import toolbox.Toolbox;

@SideOnly(Side.CLIENT)
public class ExtraBlockBreakHandler implements IResourceManagerReloadListener {
	
	public static final ExtraBlockBreakHandler INSTANCE = new ExtraBlockBreakHandler(Minecraft.getMinecraft());
	
	private final Map<Integer, DestroyExtraBlocksProgress> extraDamagedBlocks = new HashMap<Integer, DestroyExtraBlocksProgress>();
	private Minecraft mc;
	private final TextureManager renderEngine;
	private final TextureAtlasSprite[] destroyBlockIcons = new TextureAtlasSprite[10];
	
	private ExtraBlockBreakHandler(Minecraft mcIn) {
		this.mc = mcIn;
		this.renderEngine = mcIn.getTextureManager();
		((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(this);
	}
	
	@SubscribeEvent
	public void renderBlockBreakAnim(RenderWorldLastEvent event) {
		GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        this.drawBlockDamageTexture(Tessellator.getInstance(), Tessellator.getInstance().getBuffer(), this.mc.getRenderViewEntity(), event.getPartialTicks());
        this.mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableBlend();
	}
	
	@SubscribeEvent
	public void worldUnload(WorldEvent.Unload event) {
		this.extraDamagedBlocks.clear();
	}
	
	@SubscribeEvent
	public void worldLoad(WorldEvent.Load event) {
		this.extraDamagedBlocks.clear();
	}
	
	private void preRenderDamagedBlocks() {
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F);
        GlStateManager.doPolygonOffset(-3.0F, -3.0F);
        GlStateManager.enablePolygonOffset();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableAlpha();
        GlStateManager.pushMatrix();
    }

    private void postRenderDamagedBlocks() {
        GlStateManager.disableAlpha();
        GlStateManager.doPolygonOffset(0.0F, 0.0F);
        GlStateManager.disablePolygonOffset();
        GlStateManager.enableAlpha();
        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
    }
    
    private void drawBlockDamageTexture(Tessellator tessellatorIn, BufferBuilder bufferBuilderIn, Entity entityIn, float partialTicks) {
        double d3 = entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * (double) partialTicks;
        double d4 = entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * (double) partialTicks;
        double d5 = entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * (double) partialTicks;

        if (this.mc.world.getWorldTime() % 20 == 0) {
            this.cleanupExtraDamagedBlocks(this.extraDamagedBlocks.values().iterator());
        }
        
        if (!this.extraDamagedBlocks.isEmpty()) {
            this.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            this.preRenderDamagedBlocks();
            bufferBuilderIn.begin(7, DefaultVertexFormats.BLOCK);
            bufferBuilderIn.setTranslation(-d3, -d4, -d5);
            bufferBuilderIn.noColor();
            Iterator<DestroyExtraBlocksProgress> iterator = this.extraDamagedBlocks.values().iterator();

            while (iterator.hasNext()) {
            	DestroyExtraBlocksProgress destroyblockprogress = iterator.next();
                BlockPos[] blockpositions = destroyblockprogress.getPositions();
                for (int i = 0; i < blockpositions.length; i++) {
                	BlockPos blockpos = blockpositions[i];
                	double d6 = (double) blockpos.getX() - d3;
                	double d7 = (double) blockpos.getY() - d4;
                	double d8 = (double) blockpos.getZ() - d5;
                	Block block = this.mc.world.getBlockState(blockpos).getBlock();
                	TileEntity te = this.mc.world.getTileEntity(blockpos);
                	boolean hasBreak = block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockSign || block instanceof BlockSkull;
                	if (!hasBreak) hasBreak = te != null && te.canRenderBreaking();

                	if (!hasBreak) {
                		if (d6 * d6 + d7 * d7 + d8 * d8 > 1024.0D) {
                			iterator.remove();
                		} else {
                			IBlockState iblockstate = mc.world.getBlockState(blockpos);

                			if (iblockstate.getMaterial() != Material.AIR) {
                				int k1 = destroyblockprogress.getPartialBlockDamage();
                				TextureAtlasSprite textureatlassprite = this.destroyBlockIcons[k1];
                				BlockRendererDispatcher blockrendererdispatcher = this.mc.getBlockRendererDispatcher();
                				blockrendererdispatcher.renderBlockDamage(iblockstate, blockpos, textureatlassprite, this.mc.world);
                			}
                		}
                	}
                }
            }

            tessellatorIn.draw();
            bufferBuilderIn.setTranslation(0.0D, 0.0D, 0.0D);
            this.postRenderDamagedBlocks();
        }
    }
    
    private void cleanupExtraDamagedBlocks(Iterator<DestroyExtraBlocksProgress> iteratorIn) {
        while (iteratorIn.hasNext()) {
        	DestroyExtraBlocksProgress destroyblockprogress = iteratorIn.next();
            int k1 = destroyblockprogress.getCreationWorldTick();

            if (this.mc.world.getWorldTime() - k1 > 400) {
                iteratorIn.remove();
            }
        }
    }
    
    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
    	TextureMap texturemap = this.mc.getTextureMapBlocks();

        for (int i = 0; i < this.destroyBlockIcons.length; ++i) {
            this.destroyBlockIcons[i] = texturemap.getAtlasSprite("minecraft:blocks/destroy_stage_" + i);
        }
    }
	
	public void sendBlockBreakProgress(int breakerId, BlockPos[] positions, int progress) {
        if (positions.length > 0 && progress >= 0 && progress < 10) {
            DestroyExtraBlocksProgress destroyextrablocksprogress = new DestroyExtraBlocksProgress(breakerId, positions);
            this.extraDamagedBlocks.put(Integer.valueOf(breakerId), destroyextrablocksprogress);

            destroyextrablocksprogress.setPartialBlockDamage(progress);
            destroyextrablocksprogress.setWorldTick((int) this.mc.world.getWorldTime());
        } else {
            this.extraDamagedBlocks.remove(breakerId);
        }
    }
	
	private static class DestroyExtraBlocksProgress {
		
		private final int miningPlayerEntId;
	    private final BlockPos[] positions;
	    /** damage ranges from 1 to 10. -1 causes the client to delete the partial block renderer. */
	    private int partialBlockProgress;
	    /** keeps track of how many ticks this PartiallyDestroyedBlock already exists */
	    private int createdAtWorldTick;
	    
	    public DestroyExtraBlocksProgress(int miningPlayerEntIdIn, BlockPos... positionsIn) {
	        this.miningPlayerEntId = miningPlayerEntIdIn;
	        this.positions = positionsIn;
	    }
	    
	    public BlockPos[] getPositions() {
	        return this.positions;
	    }
	    
	    public void setPartialBlockDamage(int damage) {
	        if (damage > 10) {
	            damage = 10;
	        }

	        this.partialBlockProgress = damage;
	    }
	    
	    public int getPartialBlockDamage() {
	        return this.partialBlockProgress;
	    }
	    
	    /**
	     * saves the current world tick into the DestroyExtraBlocksProgress
	     */
	    public void setWorldTick(int createdAtWorldTickIn) {
	        this.createdAtWorldTick = createdAtWorldTickIn;
	    }

	    /**
	     * retrieves the 'date' at which the DestroyExtraBlocksProgress was created
	     */
	    public int getCreationWorldTick() {
	        return this.createdAtWorldTick;
	    }
	   
	}

}
