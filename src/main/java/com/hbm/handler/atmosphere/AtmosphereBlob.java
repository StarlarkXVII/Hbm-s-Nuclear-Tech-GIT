package com.hbm.handler.atmosphere;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.hbm.blocks.BlockDummyable;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.handler.ThreeInts;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.MainRegistry;
import com.hbm.util.AdjacencyGraph;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class AtmosphereBlob implements Runnable {
    
    /**
     * Somewhat based on the Advanced-Rocketry implementation, but extended to
     * define the gases and gas pressure inside the enclosed volume
     */

    // Graph containing the enclosed area
	protected final AdjacencyGraph<ThreeInts> graph;

	// Handler, provides atmosphere information and receives callbacks
	protected IAtmosphereProvider handler;


    private static ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(32));

    
	private boolean executing;
	private ThreeInts blockPos;


	public AtmosphereBlob(IAtmosphereProvider handler) {
		this.handler = handler;
		graph = new AdjacencyGraph<ThreeInts>();
	}
	
	public boolean isPositionAllowed(World world, ThreeInts pos) {
        return !isBlockSealed(world, pos);
	}

    public static boolean isBlockSealed(World world, ThreeInts pos) {
        if(pos.y < 0 || pos.y > 256) return false;

        Block block = world.getBlock(pos.x, pos.y, pos.z);

        if(block.isAir(world, pos.x, pos.y, pos.z)) return false; // Air obviously doesn't seal
        if(block instanceof IBlockSealable) { // Custom semi-sealables, like doors
            return ((IBlockSealable)block).isSealed(world, pos.x, pos.y, pos.z);
        }
        if(block instanceof BlockDummyable) return false; // Machines can't seal, almost all have gaps

        Material material = block.getMaterial();
        if(material.isLiquid() || !material.isSolid()) return false; // Liquids need to know what pressurized atmosphere they're in to determine evaporation

        AxisAlignedBB bb = block.getCollisionBoundingBoxFromPool(world, pos.x, pos.y, pos.z);

        if(bb == null) return false; // No collision, can't seal (like lamps)

        // size * 100 to correct rounding errors
        int minX = (int) ((bb.minX - pos.x) * 100);
        int minY = (int) ((bb.minY - pos.y) * 100);
        int minZ = (int) ((bb.minZ - pos.z) * 100);
        int maxX = (int) ((bb.maxX - pos.x) * 100);
        int maxY = (int) ((bb.maxY - pos.y) * 100);
        int maxZ = (int) ((bb.maxZ - pos.z) * 100);

        return minX == 0 && minY == 0 && minZ == 0 && maxX == 100 && maxY == 100 && maxZ == 100;
    }
	
	public int getBlobMaxRadius() {
		return handler.getMaxBlobRadius();
	}
	
	/**
	 * Adds a block position to the blob
	 */
	public void addBlock(int x, int y , int z) {
		addBlock(new ThreeInts(x, y, z));
	}
	
	/**
	 * Recursively checks for contiguous blocks and adds them to the graph
	 */
	public void addBlock(ThreeInts blockPos) {
		if(!this.contains(blockPos) && 
				(this.graph.size() == 0 || this.contains(blockPos.getPositionAtOffset(ForgeDirection.UP)) || this.contains(blockPos.getPositionAtOffset(ForgeDirection.DOWN)) ||
						this.contains(blockPos.getPositionAtOffset(ForgeDirection.EAST)) || this.contains(blockPos.getPositionAtOffset(ForgeDirection.WEST)) ||
						this.contains(blockPos.getPositionAtOffset(ForgeDirection.NORTH)) || this.contains(blockPos.getPositionAtOffset(ForgeDirection.SOUTH)))) {
			if(!executing) {
				this.blockPos = blockPos;
				executing = true;
				
				try {
					pool.execute(this);
				} catch (RejectedExecutionException e) {
					MainRegistry.logger.warn("Atmosphere calculation at " + this.getRootPosition() + " aborted due to oversize queue!");
				}
			}
		}
	}

	private void addSingleBlock(ThreeInts blockPos) {
        if(!graph.contains(blockPos)) {
            MainRegistry.logger.info("ADDING: " + blockPos.x + " - " + blockPos.y + " - " + blockPos.z);
			graph.add(blockPos, getPositionsToAdd(blockPos));
		}
	}
	
	/**
	 * @return the BlockPosition of the root of the blob
	 */
	public ThreeInts getRootPosition() {
		return handler.getRootPosition();
	}
	
	/**
	 * Gets adjacent blocks if they exist in the blob
	 * @param blockPos block to find things adjacent to
	 * @return list containing valid adjacent blocks
	 */
	protected HashSet<ThreeInts> getPositionsToAdd(ThreeInts blockPos) {
		HashSet<ThreeInts> set = new HashSet<>();
		
		for(ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			
			ThreeInts offset = blockPos.getPositionAtOffset(direction);
			if(graph.contains(offset))
				set.add(offset);
		}
		
		return set;
	}

	/**
	 * Given a block position returns whether or not it exists in the graph
	 * @return true if the block exists in the blob
	 */
	public boolean contains(ThreeInts position) {
		boolean contains;
		
		synchronized (graph) {
			contains = graph.contains(position);
		}

		return contains;
	}
	
	/**
	 * Given a block position returns whether or not it exists in the graph
	 * @param x
	 * @param y
	 * @param z
	 * @return true if the block exists in the blob
	 */
	public boolean contains(int x, int y, int z) {
		return contains(new ThreeInts(x, y, z));
	}

	/**
	 * Removes the block at the given coords for this blob
	 * @param blockPos
	 */
	public void removeBlock(ThreeInts blockPos) {
        synchronized (graph) {
			graph.remove(blockPos);

			for(ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {

				ThreeInts newBlock = blockPos.getPositionAtOffset(direction);
				if(graph.contains(newBlock) && !graph.doesPathExist(newBlock, handler.getRootPosition()))
					runEffectOnWorldBlocks(handler.getWorldObj(), graph.removeAllNodesConnectedTo(newBlock));
			}
		}
	}
	
	/**
	 * Removes all nodes from the blob
	 */
	public void clearBlob() {
        World world = handler.getWorldObj();

		runEffectOnWorldBlocks(world, getLocations());
        
		graph.clear();
	}
	
	/**
	 * @return a set containing all locations
	 */
	public Set<ThreeInts> getLocations() {
		return graph.getKeys();
	}
	
	/**
	 * @return the number of elements in the blob
	 */
	public int getBlobSize() {
		return graph.size();
	}

    @Override
    public void run() {
		Stack<ThreeInts> stack = new Stack<>();
		stack.push(blockPos);

        final int maxSize = this.getBlobMaxRadius();
		final HashSet<ThreeInts> addableBlocks = new HashSet<>();

		// Breadth first search; non recursive
		while(!stack.isEmpty()) {
			ThreeInts stackElement = stack.pop();
			addableBlocks.add(stackElement);

			for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
				ThreeInts searchNextPosition = stackElement.getPositionAtOffset(dir);

				// Don't path areas we have already scanned
				if(!graph.contains(searchNextPosition) && !addableBlocks.contains(searchNextPosition)) {

					try {
						if(isPositionAllowed(handler.getWorldObj(), searchNextPosition)) {
							if(searchNextPosition.getDistanceSquared(this.getRootPosition()) <= maxSize * maxSize) {
								stack.push(searchNextPosition);
								addableBlocks.add(searchNextPosition);
							} else {
								// Failed to seal, void
								clearBlob();
								executing = false;
								return;
							}
						}
					} catch (Exception e){
						// Catches errors with additional information
						MainRegistry.logger.info("Error: AtmosphereBlob has failed to form correctly due to an error. \nCurrentBlock: " + stackElement + "\tNextPos: " + searchNextPosition + "\tDir: " + dir + "\tStackSize: " + stack.size());
						e.printStackTrace();

						// Failed to seal, void
						clearBlob();
						executing = false;
						return;
					}
				}
			}
		}

		//only one instance can editing this at a time because this will not run again b/c "worker" is not null
		synchronized (graph) {
			for(ThreeInts addableBlock : addableBlocks) {
				addSingleBlock(addableBlock);
			}

			handler.onBlobCreated(this);
		}

		executing = false;
    }


    /**
	 * @param world
	 * @param blocks Collection containing affected locations
	 */
	protected void runEffectOnWorldBlocks(World world, Collection<ThreeInts> blocks) {
		CBT_Atmosphere globalAtmosphere = CelestialBody.getTrait(world, CBT_Atmosphere.class);

		List<AtmosphereBlob> nearbyBlobs = ChunkAtmosphereManager.proxy.getBlobsWithinRadius(world, getRootPosition(), getBlobMaxRadius());

		for(ThreeInts pos : blocks) {
			for(AtmosphereBlob blob : nearbyBlobs) {
				if(blob != this && blob.contains(pos)) continue;
			}

			final Block block = world.getBlock(pos.x, pos.y, pos.z);
			if(block == Blocks.torch) {
				if(globalAtmosphere == null || (!globalAtmosphere.hasFluid(Fluids.OXYGEN, 0.09) && !globalAtmosphere.hasFluid(Fluids.AIR, 0.21))) {
					block.dropBlockAsItem(world, pos.x, pos.y, pos.z, world.getBlockMetadata(pos.x, pos.y, pos.z), 0);
					world.setBlockToAir(pos.x, pos.y, pos.z);
				}
			} else if(block == Blocks.water || block == Blocks.flowing_water) {
				if(globalAtmosphere == null || globalAtmosphere.getPressure() < 0.2) {
					world.setBlockToAir(pos.x, pos.y, pos.z);
				}
			}
		}
	}

}
