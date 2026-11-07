package dashketch.mods.simpleExcavator.client;

import dashketch.mods.simpleExcavator.client.gui.ExcavatorScreen;
import dashketch.mods.simpleExcavator.client.manager.ExcavatorManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class SimpleExcavatorClient implements ClientModInitializer {
    private static KeyBinding openGuiKey;
    private static KeyBinding toggleExcavationKey;

    private static final KeyBinding.Category simpleExcavatorCategory = KeyBinding.Category.create(Identifier.of("category.simple_excavator"));

    @Override
    public void onInitializeClient() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_excavator.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                simpleExcavatorCategory
        ));

        toggleExcavationKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_excavator.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                simpleExcavatorCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (openGuiKey.wasPressed()) {
                //noinspection NoTranslation
                client.setScreen(new ExcavatorScreen(Text.translatable("screen.simple_excavator.config")));
            }

            while (toggleExcavationKey.wasPressed()) {
                ExcavatorManager.getInstance().toggleExcavation(client);
            }

            ExcavatorManager.getInstance().tick(client);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> ExcavatorManager.getInstance().renderHighlight(context));
    }
}