package com.matt.forgehax.asm.events;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created by Babbaj on 8/4/2017.
 *
 * Called every time the player updates cancel to stop getting pushed out of blocks
 */
@Cancelable
public class PushOutOfBlocksEvent extends Event {}
