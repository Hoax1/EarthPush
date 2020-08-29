package me.hoax1.earthpush;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap; 

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.configuration.ConfigManager;


public class EarthPush extends EarthAbility implements AddonAbility {

	private EarthPushListener listener;
	
	private long cooldown;
	private long longCooldown;
	private long shortCooldown;
	
	private Location blockOrigin;
	private Block initialBlock;
	private int width;
	private int height;
	
	private int range;
	private double damage;
	
	private int maxClimbHeight;
	private int maxDecendHeight;
	
	private int brokenBlocks;
	private int minBrokenBlocksToBreak;
	
	private double blockCollisionRadius;
	private double boundingSphereRadius;
	
	private int minLocalRaiseDepth;
	private int maxLocalRaiseHeight;
	
	private double distanceFromPlayer;
	private Vector direction;
	private Location centerLocation;
	private Location centerOrigin;
	private List<Location> blockLocations;
	private List<Location> lastBlockLocations;
	private List<Location> groundBlockLocations;

	private List<TempBlock> tempBlocks;
	private double knockback;
	private double knockup;
	
	private double raisingSpeed;
	private double loweringSpeed;
	private double pushingSpeed;
	
	private long raiseTime;
	private long raiseInterval;
	
	private long lowerTime;
	private long lowerInterval;
	
	private long pushTime;
	private long pushInterval;
	
	public boolean isCanceled;
	public boolean isRaising;
	public boolean isLowering;
	public boolean isPushing;
	
	private int blocksRaised;
	private int blocksLowered;
	
	private Vector pushDirection;
	

	public EarthPush(Player player) {
		super(player);

		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		
		setFields();
		if (!prepare()) {
			return;
		}
		
		start();
	}

	private void setFields() {
		this.longCooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Hoax1.Earth.EarthPush.LongCooldown");
		this.shortCooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Hoax1.Earth.EarthPush.ShortCooldown");
		
		this.damage = ConfigManager.getConfig().getDouble("ExtraAbilities.Hoax1.Earth.EarthPush.Damage");
				
		this.width = ConfigManager.getConfig().getInt("ExtraAbilities.Hoax1.Earth.EarthPush.Width");
		this.height = ConfigManager.getConfig().getInt("ExtraAbilities.Hoax1.Earth.EarthPush.Height");
		
		this.minBrokenBlocksToBreak = ConfigManager.getConfig().getInt("ExtraAbilities.Hoax1.Earth.EarthPush.MinBrokenBlocksToBreak");
		
		this.knockback = ConfigManager.getConfig().getDouble("ExtraAbilities.Hoax1.Earth.EarthPush.Knockback");
		this.knockup = ConfigManager.getConfig().getDouble("ExtraAbilities.Hoax1.Earth.EarthPush.Knockup");
		
		this.pushingSpeed = ConfigManager.getConfig().getDouble("ExtraAbilities.Hoax1.Earth.EarthPush.Speed");
		this.raisingSpeed = ConfigManager.getConfig().getDouble("ExtraAbilities.Hoax1.Earth.EarthPush.RaiseSpeed");
		this.loweringSpeed = ConfigManager.getConfig().getDouble("ExtraAbilities.Hoax1.Earth.EarthPush.LowerSpeed");
		
		this.range = ConfigManager.getConfig().getInt("ExtraAbilities.Hoax1.Earth.EarthPush.Range");
		
		this.maxClimbHeight = ConfigManager.getConfig().getInt("ExtraAbilities.Hoax1.Earth.EarthPush.MaxClimbHeight");
		this.maxDecendHeight = ConfigManager.getConfig().getInt("ExtraAbilities.Hoax1.Earth.EarthPush.MaxDecendHeight");
		
		this.distanceFromPlayer = ConfigManager.getConfig().getDouble("ExtraAbilities.Hoax1.Earth.EarthPush.DistanceFromPlayer");
		
		this.blockCollisionRadius = ConfigManager.getConfig().getDouble("ExtraAbilities.Hoax1.Earth.EarthPush.BlockCollisionRadius");
		

		
		this.brokenBlocks = 0;
		
		this.boundingSphereRadius = ((double)Math.max(width, height) / 2) + this.blockCollisionRadius;
		
		this.minLocalRaiseDepth = -5;
		this.maxLocalRaiseHeight = 4;
		
		this.initialBlock = player.getLocation().add(0, -height, 0).getBlock();
		
		this.blockOrigin = initialBlock.getLocation().clone();
		
		blockOrigin.clone();
		
		this.direction = player.getLocation().getDirection();
		this.direction.setY(0);
		this.direction.normalize();
		
		this.blockLocations = new ArrayList<Location>();
		this.lastBlockLocations = new ArrayList<Location>();
		this.groundBlockLocations = new ArrayList<Location>();
		this.tempBlocks = new ArrayList<TempBlock>();
		
		this.isCanceled = false;
		this.isRaising = true;
		this.isLowering = false;
		this.isPushing = false;
		
		this.raiseInterval = (long)(1000.0 / this.raisingSpeed);
		this.lowerInterval = (long)(1000.0 / this.loweringSpeed);
		this.pushInterval = (long)(1000.0 / this.pushingSpeed);
		
		this.blocksRaised = 0;
		this.blocksLowered = 0;
	}
	
	private boolean prepare() {
		// Try to find a starting position for the bottom center block.
		Location bottomBlockLoc = null;
			
		final Location feetLoc = player.getLocation().add(this.direction.clone().multiply(this.distanceFromPlayer));
		for(int i = 0; i < (this.maxLocalRaiseHeight - this.minLocalRaiseDepth) + 1; i++) {
			int localYPos = this.maxLocalRaiseHeight - i;
			Location bLoc = feetLoc.clone().add(new Vector(0, localYPos, 0));
			Block block = bLoc.getBlock();
			
			if (TempBlock.isTempBlock(block)) {
				TempBlock.get(block).revertBlock();
			}
			
			if (!block.isPassable() && this.isEarthbendable(block)) {
				Block upperNeighbor = bLoc.clone().add(new Vector(0, 1, 0)).getBlock();
				if (upperNeighbor.isPassable() || TempBlock.isTempBlock(upperNeighbor)) {
					bottomBlockLoc = bLoc;

					break;
				}
			}
		}
		
		
		// Cancel move and don't go on cooldown if we couldn't find a starting position.
		if (bottomBlockLoc == null) {
			return false;
		}
		
		this.centerLocation = bottomBlockLoc.getBlock().getLocation();
		this.centerLocation.add(0, ((double)(this.height)/2) - 0.5, 0);
		this.centerOrigin = this.centerLocation.clone();
		
		Vector rightDir = new Vector(this.direction.getZ(), 0, -this.direction.getX());
		
		Location leftBottomBlockLoc = bottomBlockLoc.clone();
		leftBottomBlockLoc.add(rightDir.clone().multiply((double)(-this.width)/2));
		Location rightBottomBlockLoc = bottomBlockLoc.clone();
		rightBottomBlockLoc.add(rightDir.clone().multiply((double)(this.width)/2));
		
		// Get blocks
		
		// Get pillar locations using linear interpolation.
		double t = 0;
	    double N = diagonal_distance(leftBottomBlockLoc.clone(), rightBottomBlockLoc.clone());
	    for (int x = 0; x < this.width; x++) {
	        Location pillarLoc = lerp_point(leftBottomBlockLoc.clone(), rightBottomBlockLoc.clone(), t);
	        pillarLoc.setY(bottomBlockLoc.getY());
	        
	        pillarLoc.add(rightDir.clone().multiply(0.5));

	        t += 1.0 / N;
	        
	        // Get every block in pillar, starting with the top block and going down.
	        for (int y = 0; y < this.height; y++) {
				Location blockLoc = pillarLoc.clone();
				
				blockLoc.add(0, -y, 0);
				
				// Stop raising this pillar if a non-earthbendable block is found.
				if (!this.isEarthbendable(blockLoc.getBlock())) {
					break;
				}
				
				blockLocations.add(blockLoc.getBlock().getLocation());
			}
	    }
		
		// Replace blocks under ground with TempBlocks
		for (int i = 0; i < blockLocations.size(); i++) {
			Location bLoc = blockLocations.get(i);
			BlockData blockData = bLoc.getBlock().getBlockData();
			
			//Location targetLoc = bLoc.clone().add(upDir.clone().multiply(height));
			Location targetLoc = bLoc.clone();
			
			// Revert any old TempBlocks in the way
			if (TempBlock.isTempBlock(targetLoc.getBlock())) {
				TempBlock.get(targetLoc.getBlock()).revertBlock();
			}
			
			// Place new TempBlock
			TempBlock tempBlock = new TempBlock(targetLoc.getBlock(), blockData);
			
			tempBlocks.add(tempBlock);
			blockLocations.set(i, targetLoc);
		}
		
		// Get block positions below wall
		for (int i = 0; i < blockLocations.size(); i++) {
			Location bLoc = blockLocations.get(i);
			Location groundBlockLoc = bLoc.clone().add(new Vector(0, -1, 0));
			
			if (groundBlockLoc.getY() == (int)(bottomBlockLoc.getY() - this.height)) {
				this.groundBlockLocations.add(groundBlockLoc);
			}
		}
		
		return true;
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			breakWall(true);
			remove();
			return;
		}
		
		if (isCanceled) {
			breakWall(false);
			remove();
			return;
		}
		
		
		if (wallIsBroken(this.minBrokenBlocksToBreak)) {
			if (!isPushing) {
				this.cooldown = this.longCooldown;
				this.bPlayer.addCooldown(this);
			}
			breakWall(true);
			remove();
			return;
		}
		
		if (isRaising) {
			if (System.currentTimeMillis() - this.raiseTime >= this.raiseInterval) {	
				if (this.blocksRaised >= this.height) {
					isRaising = false;
				}
				else {
					this.raiseTime = System.currentTimeMillis();
					pushBlocks(new Vector(0, 1, 0), 1, true);
					this.blocksRaised += 1;
				}
			}
		}
		
		if (isLowering) {
			if (this.blocksLowered == 0) {
				this.cooldown = this.shortCooldown;
				this.bPlayer.addCooldown(this);
			}
			
			if (System.currentTimeMillis() - this.lowerTime >= this.lowerInterval) {	
				this.lowerTime = System.currentTimeMillis();
				
				pushBlocks(new Vector(0, -1, 0), 1, true);
				this.blocksLowered += 1;
				
				if (this.blocksLowered >= this.height) {					
					breakWall(false);
					remove();
					return;
				}
			}
		}
		
		if (isPushing && this.blocksRaised >= this.height) {	
			if (!tryToClimb()) {
				breakWall(true);
				remove();
				return;
			}
			
			if (!pathIsClear()) {
				breakWall(true);
				remove();
				return;
			}
			
			checkForCollision(this.pushDirection, 1);
			
			if (System.currentTimeMillis() - this.pushTime >= this.pushInterval) {
				this.pushTime = System.currentTimeMillis();
				
				if (centerOrigin.distance(centerLocation) > range) {
					breakWall(true);
					remove();
					return;
				}
				
				pushBlocks(this.pushDirection, 1, true);
				
				updateTrail();
			}	
			if (!groundBlocks()) {
				breakWall(true);
				remove();
				return;
			}
		}
	}
	
	public void startPushing() {
		if (!isPushing) {
			if (this.blocksLowered < this.height) {
				this.cooldown = this.longCooldown;
				this.bPlayer.addCooldown(this);
				
				for (int i = 0; i < this.blocksLowered; i++) {
					pushBlocks(new Vector(0, 1, 0), blocksLowered, false);
				}
				
				this.pushDirection = player.getLocation().getDirection().clone();
				this.pushDirection.setY(0);
				this.pushDirection.normalize();
				
				isPushing = true;
				isLowering = false;
				isRaising = true;
			}
		}
	}
	
	public void startLowering() {
		if (!this.isPushing) {			
			this.isLowering = true;
			this.isRaising = false;
		}
	}
	
	private boolean wallIsBroken(int minBrokenBlocksToBreak) {
		if (brokenBlocks >= minBrokenBlocksToBreak) {
			return true;
		}
		
		List<Location> blockLocationsToBreak = new ArrayList<Location>();
		for (int i = 0; i < blockLocations.size(); i++) {
			Location bLoc = blockLocations.get(i);
			
			if (bLoc.getBlock().isPassable()) {
				brokenBlocks += 1;
				if (brokenBlocks >= minBrokenBlocksToBreak) {
					return true;
				}
				
				blockLocationsToBreak.add(bLoc);
			}
		}
		
		// Stop updating blocks that are broken
		for (Location bLoc : blockLocationsToBreak) {
			blockLocations.remove(bLoc);
		}
		
		return false;
	}
	
	private boolean tryToClimb() {
		final Vector upDir = new Vector(0, 1, 0);
		for (Location groundBlockLoc : groundBlockLocations) {
			Location bLoc = groundBlockLoc.clone();
			bLoc.add(pushDirection);
			
			for (int i = 0; i < maxClimbHeight; i++) {
				// Get block above last block
				bLoc.add(upDir.clone().multiply(i + 1));
				
				if (blockLocations.contains(bLoc)) {
					continue;
				}
				
				// If we find a gap in the pillar, start checking next pillar over ground block
				if (bLoc.getBlock().isPassable()) {
					break;
				}
				
				
				if (i == maxClimbHeight - 1) {
					if (bLoc.clone().add(upDir).getBlock().isPassable()) {
						// If we're on the last block in the pillar and the block above is air, climb
						pushBlocks(upDir, maxClimbHeight, false);
						return true;
					}
				}	
			}
		}
		
		// If nothing was climbable, don't break wall
		return true;
	}
	
	
	private boolean pathIsClear() {
		List<Location> blockLocationsToBreak = new ArrayList<Location>();
		
		for (int i = 0; i < blockLocations.size(); i++) {				
			Location bLoc = blockLocations.get(i);

			Location targetLoc = bLoc.clone();
			targetLoc.add(pushDirection);
			
			Block targetBlock = targetLoc.getBlock();
	
			// If we hit a block in our own wall, path is still clear
			for (int j = 0; j < blockLocations.size(); j++) {
				if (blockLocations.get(j).getBlock().equals(targetBlock)) {
					return true;
				}
			}
			
			if (!targetBlock.isPassable()) {
				// If we hit a TempBlock from another move, revert it
				if (TempBlock.isTempBlock(targetBlock)) {
					TempBlock.get(targetBlock).revertBlock();
				}
				
				// If we hit a block, remove wall block that hit it later
				brokenBlocks += 1;
				
				blockLocationsToBreak.add(bLoc);
				
			}
		}
		
		// Break blocks that are hit and stop updating them
		for (Location bLoc : blockLocationsToBreak) {
			if (TempBlock.isTempBlock(bLoc.getBlock())) {
				breakBlock(TempBlock.get(bLoc.getBlock()), true);
			}
			
			blockLocations.remove(bLoc);
		}
		
		// If we didn't hit anything, path is clear
		return true;
	}
	
	
	private void pushBlocks(Vector targetDirection, int distance, boolean playSound) {
		this.lastBlockLocations = new ArrayList<Location>(this.blockLocations);
		
		targetDirection = targetDirection.multiply(distance);
		
		HashMap<Location, BlockData> oldTempBlocks = new HashMap<Location, BlockData>();
		
		// Revert all TempBlocks
		for (int i = 0; i < tempBlocks.size(); i++) {
			oldTempBlocks.put(tempBlocks.get(i).getLocation(), tempBlocks.get(i).getBlockData());
			tempBlocks.get(i).revertBlock();
		}
		tempBlocks.clear();
		
		
		// Move wall
		for (int i = 0; i < blockLocations.size(); i++) {
			Location bLoc = blockLocations.get(i);
	
			Location targetLoc = bLoc.clone().add(targetDirection);

			TempBlock tempBlock = new TempBlock(targetLoc.getBlock(), oldTempBlocks.get(bLoc.getBlock().getLocation()));
			
			tempBlocks.add(tempBlock);
			blockLocations.set(i, targetLoc);
		}
		
		// Move ground block locations
		for (int i = 0; i < groundBlockLocations.size(); i++) {
			Location groundBlockLoc = groundBlockLocations.get(i);

			groundBlockLoc.add(targetDirection);
			groundBlockLocations.set(i, groundBlockLoc);

		}
		
		this.centerLocation.add(targetDirection);
		
		if (playSound) {
			playEarthbendingSound(this.centerLocation);
		}
	}
	
	private boolean groundBlocks() {
		for (int i = 0; i < this.maxDecendHeight; i++) {
			// Check if any block is on the ground
			for (int j = 0; j < this.groundBlockLocations.size(); j++) {
				Location groundBlockLoc = this.groundBlockLocations.get(j);
				
				if (!groundBlockLoc.getBlock().isPassable()) {
					// If a block is on ground, stop decending
					return true;
				}
			}
			
			// If no block is on the ground, move wall down
			pushBlocks(new Vector(0, -1, 0), 1, false);
		}
		
		// If we've already moved down as far as we can, check if the wall has landed
		for (int i = 0; i < this.groundBlockLocations.size(); i++) {
			Location groundBlockLoc = this.groundBlockLocations.get(i);
			
			if (!groundBlockLoc.getBlock().isPassable()) {
				// If a block has landed on ground, grounding the blocks was successful
				return true;
			}
		}
		
		// If no block is on the ground after moving down the wall, grounding the blocks was not successful
		return false;
	}
	
	private void checkForCollision(Vector targetDirection, int blocksForward) {
		Location colliderCenter = this.centerLocation.getBlock().getLocation();
		List<Location> colliderLocations = new ArrayList<Location>(blockLocations);
		
		List<Entity> entitiesHit = new ArrayList<Entity>();
		
		for (int i = 0; i < blocksForward + 1; i++) {
			// Get all entities in the bounding sphere of the collider
			final List<Entity> entities = GeneralMethods.getEntitiesAroundPoint(colliderCenter, this.boundingSphereRadius);
			
			for (Entity entity : entities) {
				if (entitiesHit.contains(entity)) {
					continue;
				}
				
				Location entityLoc = entity.getLocation();
				
				// Check distance to all entities in bounding sphere for every block
				for (Location cLoc : colliderLocations) {
					if (cLoc.distance(entityLoc) <= this.blockCollisionRadius) {
						DamageHandler.damageEntity(entity, this.damage, this);
						final Vector travelVec = this.pushDirection.clone();
						entity.setVelocity(travelVec.multiply(this.knockback).setY(this.knockup));
						
						entitiesHit.add(entity);
						
						break;
					}
				}
			}
			
			// Move collider forward
			colliderCenter = colliderCenter.clone().add(targetDirection).getBlock().getLocation();
			for (int j = 0; j < colliderLocations.size(); j++) {
				colliderLocations.set(j, colliderLocations.get(j).getBlock().getLocation().add(targetDirection).getBlock().getLocation());
			}
		}
	}
	
	private void updateTrail() {
		// Remove blocks like fire, tall grass, signs etc that the wall passes through.
		for (Location behindLoc : lastBlockLocations) {
			Block behindBlock = behindLoc.getBlock();
		
			if (behindBlock.isPassable() && !behindBlock.isLiquid()) {
				behindBlock.setBlockData(Bukkit.createBlockData("minecraft:air"));
			}
		}
		
		
		// Display particles
		for (int i = 0; i < groundBlockLocations.size(); i++) {
			Location frontGroundBlockLoc = groundBlockLocations.get(i).clone().add(this.pushDirection.clone().multiply(0.75));
			Location backGroundBlockLoc = groundBlockLocations.get(i).clone().add(this.pushDirection.clone().multiply(-1));
			
			BlockData frontParticleData;
			BlockData backParticleData;
			BlockData wallParticleData = groundBlockLocations.get(i).clone().add(new Vector(0, 1, 0)).getBlock().getBlockData();
			
			// Particles in front of wall
			
			// Use block under wall's materials as particles
			if (this.isEarthbendable(frontGroundBlockLoc.getBlock())) {
				frontParticleData = frontGroundBlockLoc.getBlock().getBlockData();
			}
			else {
				// If block under the wall is not earthbendable, use wall block data as particles
				frontParticleData = wallParticleData;
			}
			
			
			if (frontParticleData != null) {
				for (int j = 0; j < Math.ceil((double)this.height/2); j++) {
					// Spawn particles
					Location particleLoc = frontGroundBlockLoc.clone().add(new Vector(0, 1 + j, 0));
					ParticleEffect.BLOCK_CRACK.display(particleLoc, 50, 0.6, 0.6, 0.6, frontParticleData);
				}
			}
			

	
			
			// Particles behind wall
			
			// Use block under wall's materials as particles
			if (this.isEarthbendable(backGroundBlockLoc.getBlock())) {
				backParticleData = backGroundBlockLoc.getBlock().getBlockData();
			}
			else {
				// If block under the wall is not earthbendable, use wall block data as particles
				backParticleData = backGroundBlockLoc.clone().add(new Vector(0, 1, 0)).getBlock().getBlockData();
			}
			
			if (backParticleData != null) {
				// Spawn particles
				Location particleLoc = backGroundBlockLoc.clone().add(new Vector(0, 1, 0));
				ParticleEffect.BLOCK_CRACK.display(particleLoc, 50, 0.6, 0.6, 0.6, backParticleData);
			}
		}
	}
	
	private void breakBlock(TempBlock tempBlock, boolean spawnParticles) {
		Location bLoc = tempBlock.getLocation();
		
		tempBlock.revertBlock();
		
		if (spawnParticles) {
			BlockData blockData = tempBlock.getBlockData();
			ParticleEffect.BLOCK_CRACK.display(bLoc, 75, 0.5, 0.5, 0.5, blockData);
		}
	}
	
	private void breakWall(boolean spawnParticles) {
		for (TempBlock tempBlock : this.tempBlocks) {
			if (TempBlock.isTempBlock(tempBlock.getBlock())) {
				breakBlock(tempBlock, spawnParticles);
			}
		}
	}
	
	private static double diagonal_distance(Location p0, Location p1) {
	    double dx = p1.getX() - p0.getX();
	    double dz = p1.getZ() - p0.getZ();
	    return Math.max(Math.abs(dx), Math.abs(dz));
	}

	private static Location lerp_point(Location p0, Location p1, double t) {
		Location point = new Location(p0.getWorld(), lerp(p0.getX(), p1.getX(), t), 0, lerp(p0.getZ(), p1.getZ(), t));
	    return point;
	}
	
	
	private static double lerp(double start, double end, double t) {
	    return start + t * (end - start);
	}
	
	
	@Override
	public long getCooldown() {
		return this.cooldown;
	}
	

	

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "EarthPush";
	}
	
	@Override
	public String getDescription() {
		return "EarthPush is a powerful earthbending technique that can be used both offensively and defensively. "
				+ "It's main use is to raise a wall or pillar from the ground to block incomming attacks. "
				+ "The wall/pillar can then be pushed along the ground, knocking away and dealing damage to everything in it's path. "
				+ "EarthPush can also be used for mobility by launching it towards yourself. "
				+ "Be careful though, it deals a lot of damage!";
	}
	
	@Override
	public String getInstructions() {
		return "Hold sneak to raise a wall/pillar of earth from the ground. Release sneak to drop it down, or left click to launch it towards your opponents!";
	}

	@Override
	public String getAuthor() {
		return "Hoax1";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public void load() {
		listener = new EarthPushListener();
		ProjectKorra.plugin.getServer().getPluginManager().registerEvents(listener, ProjectKorra.plugin);
		ProjectKorra.log.info("Successfully enabled " + getName() + " by " + getAuthor());
		
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.LongCooldown", 6000.0);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.ShortCooldown", 500.0);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.Damage", 4.0);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.Width", 3);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.Height", 3);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.MinBrokenBlocksToBreak", 3);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.Knockback", 2.5);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.Knockup", 0.75);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.Speed", 17.5);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.RaiseSpeed", 20.0);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.LowerSpeed", 20.0);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.Range", 60.0);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.MaxClimbHeight", 1);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.MaxDecendHeight", 2);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.DistanceFromPlayer", 2.0);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.BlockCollisionRadius", 0.6);
		
		ConfigManager.defaultConfig.save();
	}

	@Override
	public void stop() {
		ProjectKorra.log.info("Successfully disabled " + getName() + " by " + getAuthor());
		PlayerToggleSneakEvent.getHandlerList().unregister(listener);
		super.remove();
	}

}