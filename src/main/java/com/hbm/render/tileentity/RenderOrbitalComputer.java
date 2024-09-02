package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityAirScrubber;
import com.hbm.tileentity.machine.TileEntityOrbitalStationComputer;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;

public class RenderOrbitalComputer extends TileEntitySpecialRenderer implements IItemRendererProvider {

	@Override
	public void renderTileEntityAt(TileEntity te, double x, double y, double z, float interp) {
		if(!(te instanceof TileEntityOrbitalStationComputer)) return;
		TileEntityOrbitalStationComputer scrubber = (TileEntityOrbitalStationComputer) te;

		GL11.glPushMatrix();
		{

			GL11.glTranslated(x + 0.5D, y, z + 0.5D);
			GL11.glEnable(GL11.GL_LIGHTING);
	
			GL11.glRotatef(180, 0F, 1F, 0F);
	
			switch(te.getBlockMetadata() - BlockDummyable.offset) {
			case 2: GL11.glRotatef(0, 0F, 1F, 0F); break;
			case 4: GL11.glRotatef(90, 0F, 1F, 0F); break;
			case 3: GL11.glRotatef(180, 0F, 1F, 0F); break;
			case 5: GL11.glRotatef(270, 0F, 1F, 0F); break;
			}
	
			bindTexture(ResourceManager.orbital_computer_tex);
	
			GL11.glShadeModel(GL11.GL_SMOOTH);
			ResourceManager.orbital_computer.renderAll();


			GL11.glShadeModel(GL11.GL_FLAT);

		}
		GL11.glPopMatrix();
	}

	@Override
	public IItemRenderer getRenderer() {
		return new ItemRenderBase() {
			public void renderInventory() {
				GL11.glTranslated(0, -2, 0);
				GL11.glScaled(4, 4, 4);
			}
			public void renderCommon() {
				GL11.glDisable(GL11.GL_CULL_FACE);
				GL11.glShadeModel(GL11.GL_SMOOTH);
				bindTexture(ResourceManager.orbital_computer_tex);
				ResourceManager.orbital_computer.renderAll();
				GL11.glShadeModel(GL11.GL_FLAT);
				GL11.glEnable(GL11.GL_CULL_FACE);
			}
		};
	}

	@Override
	public Item getItemForRenderer() {
		return Item.getItemFromBlock(ModBlocks.air_scrubber);
	}

}
