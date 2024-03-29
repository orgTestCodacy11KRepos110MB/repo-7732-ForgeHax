package com.matt.forgehax.mods.services;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.github.lunatrius.core.client.renderer.unique.GeometryTessellator;
import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/** Created on 6/14/2017 by fr1kin */
@RegisterMod
public class RenderEventService extends ServiceMod {
  private static final GeometryTessellator TESSELLATOR = new GeometryTessellator();

  public RenderEventService() {
    super("RenderEventService");
  }

  @SubscribeEvent
  public void onRenderWorld(RenderWorldLastEvent event) {
    GlStateManager.pushMatrix();
    GlStateManager.disableTexture2D();
    GlStateManager.enableBlend();
    GlStateManager.disableAlphaTest();
    GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
    GlStateManager.shadeModel(GL11.GL_SMOOTH);
    GlStateManager.disableDepthTest();

    GlStateManager.lineWidth(1.f);

    Vec3d renderPos = EntityUtils.getInterpolatedPos(getLocalPlayer(), event.getPartialTicks());

    RenderEvent e = new RenderEvent(TESSELLATOR, renderPos, event.getPartialTicks());
    e.resetTranslation();
    MinecraftForge.EVENT_BUS.post(e);

    GlStateManager.lineWidth(1.f);

    GlStateManager.shadeModel(GL11.GL_FLAT);
    GlStateManager.disableBlend();
    GlStateManager.enableAlphaTest();
    GlStateManager.enableTexture2D();
    GlStateManager.enableDepthTest();
    GlStateManager.enableCull();
    GlStateManager.popMatrix();
  }

  @SubscribeEvent(priority = EventPriority.LOW)
  public void onRenderGameOverlayEvent(final RenderGameOverlayEvent.Text event) {
    if (event.getType().equals(RenderGameOverlayEvent.ElementType.TEXT)) {
      MinecraftForge.EVENT_BUS.post(new Render2DEvent(event.getPartialTicks()));
      GlStateManager.color4f(1.f, 1.f, 1.f, 1.f); // reset color
    }
  }
}
