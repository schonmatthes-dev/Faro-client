package com.faro.client;

import com.faro.client.gui.ClickGui;
import com.faro.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class FaroClient implements ClientModInitializer {
    public static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.faroclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) {
                if (mc.screen == null && mc.level != null) mc.setScreen(new ClickGui());
            }
            ModuleManager.tick(mc);
        });
    }
}
