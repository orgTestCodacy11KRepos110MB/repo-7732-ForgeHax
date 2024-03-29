package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.*;
import static com.matt.forgehax.util.entity.EntityUtils.*;
import static net.minecraft.util.math.RayTraceResult.Type;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceFluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Created by Babbaj on 9/4/2017. TODO: check all 4 collision box corners */
@RegisterMod
public class AutoBucketFallMod extends ToggleMod {
  public AutoBucketFallMod() {
    super(Category.PLAYER, "AutoBucket", false, "Automatically place bucket to reset fall damage");
  }

  public final Setting<Double> preHeight =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("PreHeight")
          .description("how far below to check before preparing")
          .defaultTo(10D)
          .build();
  public final Setting<Double> settingFallHeight =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("height")
          .description("minimum fall distance to work")
          .defaultTo(15D)
          .build();

  private ItemStack WATER_BUCKET = new ItemStack(Items.WATER_BUCKET);

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null
        || getLocalPlayer().fallDistance < settingFallHeight.getAsDouble()
        || !getLocalPlayer().inventory.hasItemStack(WATER_BUCKET)
        || isInWater(getLocalPlayer())
        || isAboveWater(getLocalPlayer())) return;

    Vec3d playerPos = getLocalPlayer().getPositionVector();
    Vec3d rayTraceBucket = new Vec3d(playerPos.x, playerPos.y - 5, playerPos.z);
    Vec3d rayTracePre =
        new Vec3d(
            playerPos.x,
            playerPos.y - preHeight.getAsDouble(),
            playerPos.z); // find the ground before the player is ready to water bucket

    RayTraceResult result = MC.world.rayTraceBlocks(playerPos, rayTraceBucket, RayTraceFluidMode.ALWAYS);
    RayTraceResult resultPre = MC.world.rayTraceBlocks(playerPos, rayTracePre, RayTraceFluidMode.ALWAYS);

    if (resultPre != null
        && resultPre.type.equals(Type.BLOCK)
        && !(getWorld().getBlockState(resultPre.getBlockPos()).getFluidState().isSource())) { // set the pitch early to not get cucked by ncp
      getLocalPlayer().prevRotationPitch = 90f;
      getLocalPlayer().rotationPitch = 90f;

      int bucketSlot = findBucketHotbar();
      if (bucketSlot == -1) bucketSlot = findBucketInv();
      if (bucketSlot > 8) {
        swap(
            bucketSlot,
            getLocalPlayer().inventory.currentItem); // move bucket from inventory to hotbar
      } else {
        MC.player.inventory.currentItem = bucketSlot;
      }
    }

    if (result != null
        && result.type.equals(Type.BLOCK)
        && !(getWorld().getBlockState(result.getBlockPos()).getFluidState().isSource())) {
      getNetworkManager()
          .sendPacket(
              new CPacketPlayer.Rotation(
                  getLocalPlayer().rotationYaw,
                  90,
                  getLocalPlayer().onGround)); // probably unnecessary but doing it anyways
      getLocalPlayer().prevRotationPitch = 90f;
      getLocalPlayer().rotationPitch = 90f;

      // printMessage("Attempted to place water bucket");
      MC.playerController.processRightClick(getLocalPlayer(), getWorld(), EnumHand.MAIN_HAND);
    }
  }

  private int findBucketInv() {
    return getLocalPlayer().inventory.getSlotFor(WATER_BUCKET); // find bucket in entire inventory
  }

  private int findBucketHotbar() {
    for (int i = 0; i < 9; i++) // iterate through hotbar slots
    if (getLocalPlayer().inventory.getStackInSlot(i).getItem() == Items.WATER_BUCKET) return i;
    return -1;
  }

  private void swap(final int slot, final int hotbarNum) {
    MC.playerController.windowClick(
        getLocalPlayer().inventoryContainer.windowId,
        slot,
        hotbarNum,
        ClickType.SWAP,
        getLocalPlayer());
  }
}
