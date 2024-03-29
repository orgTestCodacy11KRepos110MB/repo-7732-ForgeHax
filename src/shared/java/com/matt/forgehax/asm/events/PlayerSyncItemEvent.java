package com.matt.forgehax.asm.events;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraftforge.eventbus.api.Event;

public class PlayerSyncItemEvent extends Event {
  private final PlayerControllerMP playerController;

  public PlayerSyncItemEvent(PlayerControllerMP playerController) {
    this.playerController = playerController;
  }

  public PlayerControllerMP getPlayerController() {
    return playerController;
  }
}
