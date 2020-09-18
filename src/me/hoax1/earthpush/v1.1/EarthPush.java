package me.hoax1.earthpush;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap; 

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

import net.md_5.bungee.api.ChatColor;

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
	private Location colliderCenter;
	
	private int minLocalRaiseDepth;
	private int maxLocalRaiseHeight;
	
	private double distanceFromPlayer;
	private Vector direction;
	private Location bottomBlockLocation;
	private Location centerLocation;
	private List<Location> blockLocations;
	private List<Location> groundBlockLocations;
	private List<Location> upperBlockLocations;

	private List<TempBlock> tempBlocks;
	
	private double knockback;
	private double knockup;
	
	private double raisingSpeed;
	private double pushingSpeed;
	
	private long raiseTime;
	private long raiseInterval;
	
	private long pushTime;
	private long pushInterval;
	
	public boolean isRaising;
	public boolean isLowering;
	public boolean isPushing;
	
	private int blocksRaised;
	
	private Vector pushDirection;
	private Vector raiseDirection;
	private Vector backDirection;
	
	private boolean replaceGround;
	private HashMap<Material, Material> replaceGroundMap;
	private double particleMultiplier;
	

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
		
		this.range = ConfigManager.getConfig().getInt("ExtraAbilities.Hoax1.Earth.EarthPush.Range");
		
		this.maxClimbHeight = ConfigManager.getConfig().getInt("ExtraAbilities.Hoax1.Earth.EarthPush.MaxClimbHeight");
		this.maxDecendHeight = ConfigManager.getConfig().getInt("ExtraAbilities.Hoax1.Earth.EarthPush.MaxDecendHeight");
		
		this.distanceFromPlayer = ConfigManager.getConfig().getDouble("ExtraAbilities.Hoax1.Earth.EarthPush.DistanceFromPlayer");
		
		this.blockCollisionRadius = ConfigManager.getConfig().getDouble("ExtraAbilities.Hoax1.Earth.EarthPush.BlockCollisionRadius");
		
		this.replaceGround = ConfigManager.getConfig().getBoolean("ExtraAbilities.Hoax1.Earth.EarthPush.ReplaceGround.Enabled");
		
		if (replaceGround) {
			List<String> pairs = ConfigManager.getConfig().getStringList("ExtraAbilities.Hoax1.Earth.EarthPush.ReplaceGround.Map");
			this.replaceGroundMap = new HashMap<Material, Material>();
			for (String pair : pairs) {
				String materials[] = pair.split("=");
				
				if (materials.length != 2) {
					player.sendMessage(ChatColor.RED + "Warning: Syntax error in config file under "
							+ "\"ExtraAbilities.Hoax1.Earth.EarthPush.ReplaceGround.Map\"."
							+ ChatColor.GRAY + "\nThe syntax for entering block pairs is:"
							+ "\n" + ChatColor.ITALIC + "- FIRST_BLOCK=SECOND_BLOCK" + ChatColor.GRAY
							+ "\nwhere " + ChatColor.ITALIC + "FIRST_BLOCK" + ChatColor.GRAY + " is the block to be replaced "
							+ "and " + ChatColor.ITALIC + "SECOND_BLOCK" + ChatColor.GRAY + " is the block that "
							+ "replaces the first block. To add a new block pair, simply make a new line under "
							+ ChatColor.ITALIC + "ReplaceGround: " + ChatColor.GRAY + "and write it using the right syntax."
							+ "\nExample:" + ChatColor.ITALIC + "\nMap:\n- GRASS_BLOCK=DIRT\n- ICE=AIR");
					continue;
				}
				
				Material mat1 = null;
				Material mat2 = null;
	
				mat1 = Material.getMaterial(materials[0]);
				mat2 = Material.getMaterial(materials[1]);
				
				if (mat1 == null) {
					player.sendMessage(ChatColor.RED + "Warning: Error in config file under "
							+ "\"ExtraAbilities.Hoax1.Earth.EarthPush.ReplaceGround.Map\".\n"
							+ ChatColor.ITALIC + "\"" + materials[0] + "\"" + ChatColor.RED + " is not a valid block!");
				}
				
				if (mat2 == null) {
					player.sendMessage(ChatColor.RED + "Warning: Error in config file under "
							+ "\"ExtraAbilities.Hoax1.Earth.EarthPush.ReplaceGround.Map\".\n"
							+ ChatColor.ITALIC + "\"" + materials[1] + "\"" + ChatColor.RED + " is not a valid block!");
				}
 				
				if (mat1 == null || mat2 == null) {
					continue;
				}
				
				
				if (this.replaceGroundMap.containsKey(mat1)) {
					player.sendMessage(ChatColor.RED + "Warning: Error in config file under "
							+ "\"ExtraAbilities.Hoax1.Earth.EarthPush.ReplaceGround.Map\". "
							+ "You can't use the same block to be replaced by two different blocks!");
					continue;
				}
				
				try {
					this.replaceGroundMap.put(mat1, mat2);
				}
				catch (Exception e) {
					player.sendMessage(ChatColor.RED + "Warning: Syntax error in config file under "
							+ "\"ExtraAbilities.Hoax1.Earth.EarthPush.ReplaceGround.Map\"."
							+ ChatColor.GRAY + "\nThe syntax for entering block pairs is:"
							+ "\n" + ChatColor.ITALIC + "- FIRST_BLOCK=SECOND_BLOCK" + ChatColor.GRAY
							+ "\nwhere " + ChatColor.ITALIC + "FIRST_BLOCK" + ChatColor.GRAY + " is the block to be replaced "
							+ "and " + ChatColor.ITALIC + "SECOND_BLOCK" + ChatColor.GRAY + " is the block that "
							+ "replaces the first block. To add a new block pair, simply make a new line under "
							+ ChatColor.ITALIC + "ReplaceGround: " + ChatColor.GRAY + "and write it using the right syntax."
							+ "\nExample:" + ChatColor.ITALIC + "\nReplaceGround:\n- GRASS_BLOCK=DIRT\n- ICE=AIR");
					continue;
				}
			}
		}
		
		
		this.particleMultiplier = ConfigManager.getConfig().getDouble("ExtraAbilities.Hoax1.Earth.EarthPush.ParticleMultiplier");
		
		this.brokenBlocks = 0;
		
		this.minLocalRaiseDepth = -5;
		this.maxLocalRaiseHeight = 4;
		
		this.initialBlock = player.getLocation().add(0, -height, 0).getBlock();
		
		this.blockOrigin = initialBlock.getLocation().clone();
		
		blockOrigin.clone();
		
		this.direction = player.getLocation().getDirection();
		this.direction.setY(0);
		this.direction.normalize();
		
		this.blockLocations = new ArrayList<Location>();
		this.groundBlockLocations = new ArrayList<Location>();
		this.upperBlockLocations = new ArrayList<Location>();
		this.tempBlocks = new ArrayList<TempBlock>();
		
		this.isRaising = true;
		this.isLowering = false;
		this.isPushing = false;
		
		this.raiseInterval = (long)(1000.0 / this.raisingSpeed);
		this.pushInterval = (long)(1000.0 / this.pushingSpeed);
		
		this.blocksRaised = 0;
		
		this.raiseDirection = new Vector(0, 1, 0);
		this.backDirection = new Vector();
	}
	
	private boolean prepare() {
		// Try to find a starting position for the bottom center block.
		Location bottomBlockLoc = null;
			
		final Location feetLoc = player.getLocation().add(this.direction.clone().multiply(this.distanceFromPlayer));
		for(int i = 0; i < (this.maxLocalRaiseHeight - this.minLocalRaiseDepth) + 1; i++) {
			int localYPos = this.minLocalRaiseDepth + i;
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
		this.centerLocation.add(0.5, 0.5, 0.5);
		
		this.colliderCenter = bottomBlockLoc.getBlock().getLocation();
		this.colliderCenter.add(0, ((double)(this.height)/2) - 0.5, 0);
		this.colliderCenter.add(0.5, 0.5, 0.5);
		
		
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
				
				// Stop raising this pillar if a solid non-earthbendable block is found.
				if (!this.isEarthbendable(blockLoc.getBlock()) && !blockLoc.getBlock().isPassable()) {
					break;
				}
				
				if (this.isEarthbendable(blockLoc.getBlock())) {
					blockLocations.add(blockLoc.getBlock().getLocation().add(0.5, 0.5, 0.5));
				}
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
			
			if (blockData.equals(Bukkit.createBlockData("minecraft:sand"))) {
				blockData = Bukkit.createBlockData("minecraft:sandstone");
			} else if (blockData.equals(Bukkit.createBlockData("minecraft:red_sand"))) {
				blockData = Bukkit.createBlockData("minecraft:red_sandstone");
			} else if (blockData.equals(Bukkit.createBlockData("minecraft:gravel"))) {
				blockData = Bukkit.createBlockData("minecraft:stone");
			} else if (blockData.getAsString().endsWith("concrete_powder")) {
				blockData = Bukkit.createBlockData(blockData.getAsString().replace("_powder", ""));
			}
			
			// Place new TempBlock
			TempBlock tempBlock = new TempBlock(targetLoc.getBlock(), blockData);
			
			tempBlocks.add(tempBlock);
			blockLocations.set(i, targetLoc);
		}
		
		// Get block positions below and above wall
		for (int i = 0; i < blockLocations.size(); i++) {
			Location bLoc = blockLocations.get(i);
			//Location groundBlockLoc = bLoc.clone().add(new Vector(0, -1, 0));
			
			if ((int)bLoc.getY() - 1 == (int)(bottomBlockLoc.getY() - this.height)) {
				this.groundBlockLocations.add(bLoc.clone().add(0, -1, 0));
			}
			
			Block targetBlock = bLoc.getBlock().getRelative(BlockFace.UP);
			if (!targetBlock.isEmpty() && targetBlock.isPassable()) {
				this.upperBlockLocations.add(bLoc.clone().add(0, 1, 0));
				TempBlock tempBlock = new TempBlock(targetBlock, targetBlock.getBlockData());
				tempBlocks.add(tempBlock);
			}
		}
		
		this.bottomBlockLocation = bottomBlockLoc.clone().add(new Vector(0, -this.height + 1, 0));
		
		return true;
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			breakWall(true);
			remove();
			return;
		}
		
		if (!isLowering && !isPushing && !player.isSneaking()) {
			this.isLowering = true;
			
			this.isRaising = true;
			this.raiseDirection.multiply(-1);
			
			this.cooldown = this.shortCooldown;
			this.bPlayer.addCooldown(this);
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
				this.raiseTime = System.currentTimeMillis();
				boolean leaveAir = false;
				if (this.raiseDirection.getY() == 1) leaveAir = true; 
				pushBlocks(this.raiseDirection, 1, true, true, leaveAir);
				
				this.blocksRaised += this.raiseDirection.getY();
				
				if (this.blocksRaised >= this.height) {
					isRaising = false;
				}
				
				if (this.blocksRaised <= 0) {
					breakWall(false);
					remove();
					return;
				}
			}
		}
		
		if (isPushing && this.blocksRaised >= this.height) {	
			if (!tryToClimb()) {
				remove();
				return;
			}
			
			if (!pathIsClear()) {
				remove();
				return;
			}
			
			checkForCollision(this.pushDirection, 1);
			
			if (System.currentTimeMillis() - this.pushTime >= this.pushInterval) {
				this.pushTime = System.currentTimeMillis();
				
				if (centerLocation.distanceSquared(player.getLocation()) > this.range * this.range) {
					breakWall(true);
					remove();
					return;
				}
				
				pushBlocks(this.pushDirection, 1, true, false, false);
				updateTrail(true);
			}	
			if (!groundBlocks()) {
				breakWall(true);
				remove();
				return;
				
			}
		}
	}
	
	public void startPushing() {
		if (!isPushing && !isLowering) {
			isPushing = true;
			
			this.cooldown = this.longCooldown;
			this.bPlayer.addCooldown(this);
			
			this.pushDirection = player.getLocation().getDirection().clone();
			this.pushDirection.setY(0);
			this.pushDirection.normalize();

			isRaising = true;
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
				
				// Remove ground block
				if ((int)bLoc.getBlockY() == (int)bottomBlockLocation.getBlockY()) {
					Location groundBlockLoc = bLoc.clone().add(new Vector(0, -1, 0));
					groundBlockLocations.remove(groundBlockLoc);
				}
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
						pushBlocks(upDir, maxClimbHeight, false, true, false);
						updateTrail(false);
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
	
	
	private void pushBlocks(Vector targetDirection, int distance, boolean playSound, boolean throwPlayer, boolean leaveAir) {
		Vector targetVector = targetDirection.multiply(distance);
		
		HashMap<Location, BlockData> oldTempBlocks = new HashMap<Location, BlockData>();
		
		// Revert all TempBlocks
		for (int i = 0; i < tempBlocks.size(); i++) {
			final Location bLoc = tempBlocks.get(i).getLocation();
			oldTempBlocks.put(bLoc, tempBlocks.get(i).getBlockData());
			tempBlocks.get(i).revertBlock();
			
			if (leaveAir) {
				new TempBlock(bLoc.getBlock(), Bukkit.createBlockData("minecraft:air"));
			}
		}
		tempBlocks.clear();
		
		this.bottomBlockLocation.add(targetVector);
		this.centerLocation.add(targetVector);
		this.colliderCenter.add(targetVector);
		
		
		// Move wall
		for (int i = 0; i < blockLocations.size(); i++) {
			Location bLoc = blockLocations.get(i);
	
			Location targetLoc = bLoc.clone().add(targetVector);

			TempBlock tempBlock = new TempBlock(targetLoc.getBlock(), oldTempBlocks.get(bLoc.getBlock().getLocation()));
			
			tempBlocks.add(tempBlock);
			
			blockLocations.set(i, targetLoc);
			
			// Move entities out of the way (code stolen from EarthAbility.moveEarth)
			if (throwPlayer) {
				Vector throwForce = targetDirection.clone().multiply(distance * 0.75);
				for (final Entity entity : GeneralMethods.getEntitiesAroundPoint(bLoc, 1.75)) {
					if (entity instanceof LivingEntity) {
						final LivingEntity lentity = (LivingEntity) entity;
						if (lentity.getEyeLocation().getBlockX() == bLoc.getBlockX() && lentity.getEyeLocation().getBlockZ() == bLoc.getBlockZ()) {
							if (!(entity instanceof FallingBlock)) {
								entity.setVelocity(throwForce);
							}
						}
					} else {
						if (entity.getLocation().getBlockX() == bLoc.getBlockX() && entity.getLocation().getBlockZ() == bLoc.getBlockZ()) {
							if (!(entity instanceof FallingBlock)) {
								entity.setVelocity(throwForce);
							}
						}
					}
				}
			}
		}
		
		// Move blocks above wall
		for (int i = 0; i < upperBlockLocations.size(); i++) {
			Location bLoc = upperBlockLocations.get(i);
	
			Location targetLoc = bLoc.clone().add(targetVector);

			TempBlock tempBlock = new TempBlock(targetLoc.getBlock(), oldTempBlocks.get(bLoc.getBlock().getLocation()));
			
			tempBlocks.add(tempBlock);
			
			upperBlockLocations.set(i, targetLoc);
		}
		
		// Move ground block locations
		for (int i = 0; i < groundBlockLocations.size(); i++) {
			Location groundBlockLoc = groundBlockLocations.get(i);

			groundBlockLoc.add(targetDirection);
			groundBlockLocations.set(i, groundBlockLoc);
		}
		
		if (playSound) {
			playEarthbendingSound(this.centerLocation);
		}
		
		this.backDirection = targetDirection.clone().multiply(-1);
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
			pushBlocks(new Vector(0, -1, 0), 1, false, false, false);
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
		List<Location> colliderLocations = new ArrayList<Location>();
		for (Location bLoc : blockLocations) {
			Location targetLoc = bLoc.getBlock().getLocation().add(0.5, 0.5, 0.5);
			targetLoc.add(targetDirection.clone().multiply(0.5));
			colliderLocations.add(targetLoc);
		}
		
		List<Entity> entitiesHit = new ArrayList<Entity>();
		for (Location cLoc : colliderLocations) {	
			final List<Entity> entities = GeneralMethods.getEntitiesAroundPoint(cLoc, this.blockCollisionRadius);
			for (Entity entity: entities) {
				if (!entitiesHit.contains(entity)) {
					Vector vel = this.pushDirection.clone().multiply(this.knockback);
					vel.setY(this.knockup);
					entity.setVelocity(vel);
					
					DamageHandler.damageEntity(entity, this.damage, this);
					
					entitiesHit.add(entity);
				}
			}
		}
	}
	
	private void updateTrail(boolean displayParticles) {
		if (this.particleMultiplier <= 0) {
			displayParticles = false;
		}
		
		ArrayList<Location> checkedLocations = new ArrayList<Location>();
		for (Location bLoc : blockLocations) {
			BlockIterator blocksToCheck = new BlockIterator(bLoc.getWorld(), bLoc.toVector(), this.backDirection, 0, 1);
            while(blocksToCheck.hasNext()) {
                final Block blockToCheck = blocksToCheck.next();
                if (checkedLocations.contains(blockToCheck.getLocation())) {
                	continue;
                }
                checkedLocations.add(blockToCheck.getLocation());
                
    			if (!blockToCheck.isEmpty() && blockToCheck.isPassable() && !blockToCheck.isLiquid()) {
    				new TempBlock(blockToCheck, Bukkit.createBlockData("minecraft:air"));
    			}
    			
    			// Replace Ground
    			if (this.replaceGround) {
    				final Block downBlock = blockToCheck.getRelative(BlockFace.DOWN);
    				
    				Material mat = replaceGroundMap.get(downBlock.getBlockData().getMaterial());
    				if (mat != null) {
    					downBlock.setBlockData(Bukkit.createBlockData(mat));
    				}
    			}
            }
		}
		
		if (displayParticles) {
			for (int i = 0; i < groundBlockLocations.size(); i++)  {				
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
						ParticleEffect.BLOCK_CRACK.display(particleLoc, (int)(25 * this.particleMultiplier), 0.6, 0.6, 0.6, frontParticleData);
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
					ParticleEffect.BLOCK_CRACK.display(particleLoc, (int)(25 * this.particleMultiplier), 0.6, 0.6, 0.6, backParticleData);
				}
			}
		}
	}
	
	private void breakBlock(TempBlock tempBlock, boolean displayParticles) {
		if (this.particleMultiplier <= 0) {
			displayParticles = false;
		}
		
		Location pLoc = tempBlock.getLocation();
		pLoc.add(0.5, 0.5, 0.5);
		
		tempBlock.revertBlock();
		
		if (displayParticles) {
			BlockData blockData = tempBlock.getBlockData();
			ParticleEffect.BLOCK_CRACK.display(pLoc, (int)(75 * this.particleMultiplier), 0.5, 0.5, 0.5, blockData);
		}
	}
	
	private void breakWall(boolean displayParticles) {
		for (TempBlock tempBlock : this.tempBlocks) {
			if (TempBlock.isTempBlock(tempBlock.getBlock())) {
				breakBlock(tempBlock, displayParticles);
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
		return this.centerLocation;
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
		return "Hold sneak to raise a wall/pillar of earth from the ground. "
				+ "Release sneak to drop it down, or left click to launch it towards your opponents!";
	}

	@Override
	public String getAuthor() {
		return "Hoax1";
	}

	@Override
	public String getVersion() {
		return "1.1";
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
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.Range", 25.0);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.MaxClimbHeight", 1);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.MaxDecendHeight", 2);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.DistanceFromPlayer", 2.0);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.BlockCollisionRadius", 0.6);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.ReplaceGround.Enabled", true);
		ArrayList<String> list = new ArrayList<String>();
		list.add(Material.GRASS_BLOCK.toString() + "=" + Material.DIRT.toString());
		list.add(Material.MYCELIUM.toString() + "=" + Material.DIRT.toString());
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.ReplaceGround.Map", list);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hoax1.Earth.EarthPush.ParticleMultiplier", 1.0);
		
		ConfigManager.defaultConfig.save();
	}

	@Override
	public void stop() {
		ProjectKorra.log.info("Successfully disabled " + getName() + " by " + getAuthor());
		PlayerToggleSneakEvent.getHandlerList().unregister(listener);
		super.remove();
	}

}