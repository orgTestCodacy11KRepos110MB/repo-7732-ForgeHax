package com.matt.forgehax.util.key;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.glfw.GLFW;

/** Created on 6/10/2017 by fr1kin */
public interface IKeyBind {
  void bind(int keyCode);

  KeyBinding getBind();

  void onKeyPressed();

  void onKeyDown();

  default void unbind() {

    bind(GLFW.GLFW_KEY_UNKNOWN);
  }
}
