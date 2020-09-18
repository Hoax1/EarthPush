package me.hoax1.earthpush;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;

import com.projectkorra.projectkorra.BendingPlayer;


public class EarthPushListener implements Listener {
	
	private EarthPush earthPush;
	
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

		if (event.isCancelled() || bPlayer == null) {
			return;
		}

		if (bPlayer.getBoundAbilityName().equalsIgnoreCase(null)) {
			return;
		}
		
		if (player.isSneaking()) {
			return;
		}

		if (bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthPush")) {
			this.earthPush = new EarthPush(player);
		}
	}
	
	@EventHandler
	public void onSwing(PlayerAnimationEvent event) {
		Player player = event.getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		
		if (event.isCancelled() || bPlayer == null) {
			return;
		}

		if (bPlayer.getBoundAbilityName().equalsIgnoreCase(null)) {
			return;
		}
		
		if (earthPush == null) {
			return;
		}
		
		if (bPlayer.getBoundAbilityName().equalsIgnoreCase("EarthPush")) {
			earthPush.startPushing();
		}
	}
}
