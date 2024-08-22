package com.hbm.items.tool;

import java.util.List;

import com.hbm.config.SpaceConfig;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.DebugTeleporter;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.dim.trait.CBT_Atmosphere.FluidEntry;
import com.hbm.dim.trait.CBT_War;
import com.hbm.dim.trait.CelestialBodyTrait.CBT_Destroyed;
import com.hbm.lib.Library;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class ItemWandD extends Item {

	
	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		
		if(world.isRemote)
			return stack;
		
		MovingObjectPosition pos = Library.rayTrace(player, 500, 1, false, true, false);
		
		if(pos != null) {

			if(stack.stackTagCompound == null)
				stack.stackTagCompound = new NBTTagCompound();
			
			if(!player.isSneaking()) {
				int targetId = stack.stackTagCompound.getInteger("dim");
				if(targetId == 0) targetId++; // skip blank

				SolarSystem.Body target = SolarSystem.Body.values()[targetId];

				DebugTeleporter.teleport(player, target.getBody().dimensionId, player.posX, 300, player.posZ, true);
				player.addChatMessage(new ChatComponentText("Teleported to: " + target.getBody().getUnlocalizedName()));

			} else {
				int targetId = stack.stackTagCompound.getInteger("dim");
				targetId++;

				if(targetId >= SolarSystem.Body.values().length) {
					targetId = 1;
				}
				
				stack.stackTagCompound.setInteger("dim", targetId);

				SolarSystem.Body target = SolarSystem.Body.values()[targetId];

				player.addChatMessage(new ChatComponentText("Set teleport target to: " + target.getBody().getUnlocalizedName()));
			}
		} else {
			if(!player.isSneaking()) {
				// TESTING: View atmospheric data
				CBT_Atmosphere atmosphere = CelestialBody.getTrait(world, CBT_Atmosphere.class);
	
				boolean isVacuum = true;
				if(atmosphere != null) {
					for(FluidEntry entry : atmosphere.fluids) {
						// if(entry.pressure > 0.001) {
							player.addChatMessage(new ChatComponentText("Atmosphere: " + entry.fluid.getUnlocalizedName() + " - " + entry.pressure + "bar"));
							isVacuum = false;
						// }
					}
				}
	
				if(isVacuum)
					player.addChatMessage(new ChatComponentText("Atmosphere: NEAR VACUUM"));
			} else {
				// TESTING: END OF LIFE
				World targetBody = DimensionManager.getWorld(SpaceConfig.dunaDimension);
				
				CelestialBody target = CelestialBody.getPlanet(targetBody);
				
				
				if(!target.hasTrait(CBT_War.class)) {

					// TESTING: END OF TIME
					target.setTraits(new CBT_War());		
			
				} else {

					CBT_War war = CelestialBody.getTrait(targetBody, CBT_War.class);
					
					if(war != null) {
						war.Damage(20);
						if(war.health < 0) {
							war.health = 0;
						}
						System.out.println(war.health);
					}
	
					player.addChatMessage(new ChatComponentText("kidding"));
				}
			}
		}

		return stack;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		list.add("Dimension teleporter and atmosphere debugger.");

		if(stack.stackTagCompound != null) {
			int targetId = stack.stackTagCompound.getInteger("dim");
			SolarSystem.Body target = SolarSystem.Body.values()[targetId];

			list.add("Teleportation target: " + target.getBody().getUnlocalizedName());
		}
	}
}