package dashketch.mods.simpleExcavator.client;

import dashketch.mods.simpleExcavator.client.gui.ExcavatorScreen;
import dashketch.mods.simpleExcavator.client.gui.ShapePreviewScreen;
import dashketch.mods.simpleExcavator.client.hud.DimensionsHud;
import dashketch.mods.simpleExcavator.client.manager.ExcavatorManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class SimpleExcavatorClient implements ClientModInitializer {
    private static KeyBinding openGUIKey;
    private static KeyBinding toggleExcavationKey;
    private static KeyBinding toggleDimensionsHUDKey;
    private static KeyBinding openPreviewGUIKey;
    private static KeyBinding rotatePreviewKey;
    private static KeyBinding selectEdgeKey;

    private static final KeyBinding.Category simpleExcavatorCategory = KeyBinding.Category.create(Identifier.of("simple_excavator", "main"));

    @Override
    public void onInitializeClient() {
        openGUIKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_excavator.open_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, simpleExcavatorCategory
        ));

        toggleExcavationKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_excavator.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, simpleExcavatorCategory
        ));

        toggleDimensionsHUDKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_excavator.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, simpleExcavatorCategory
        ));

        openPreviewGUIKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_excavator.open_preview", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_I, simpleExcavatorCategory
        ));

        rotatePreviewKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_excavator.rotate_preview", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, simpleExcavatorCategory
        ));

        selectEdgeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_excavator.select_edge", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_LEFT, simpleExcavatorCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (openGUIKey.wasPressed()) {
                client.setScreen(new ExcavatorScreen(Text.translatable("screen.simple_excavator.config")));
            }

            while (openPreviewGUIKey.wasPressed()) {
                client.setScreen(new ShapePreviewScreen(Text.translatable("screen.simple_excavator.preview")));
            }

            while (toggleDimensionsHUDKey.wasPressed()) {
                DimensionsHud.toggleHud();
            }

            while (toggleExcavationKey.wasPressed()) {
                ExcavatorManager.getInstance().toggleExcavation(client);
            }

            ExcavatorManager.getInstance().tick(client);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> ExcavatorManager.getInstance().renderHighlight(context));
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> DimensionsHud.render(drawContext));
    }
}