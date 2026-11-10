
package dashketch.mods.simpleExcavator.client.hud;

import dashketch.mods.simpleExcavator.client.manager.ExcavatorManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class DimensionsHud {

    private static boolean visible = false;

    public static void toggleHud() {
        visible = !visible;

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal(
                            "§e[SimpleExcavator] Dimensions HUD "
                                    + (visible ? "Enabled" : "Disabled")
                    ),
                    true
            );
        }
    }

    public static void render(DrawContext context) {

        if (!visible) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.textRenderer == null) {
            return;
        }

        ExcavatorManager manager = ExcavatorManager.getInstance();

        BlockPos c1 = manager.getCorner1();
        BlockPos c2 = manager.getCorner2();

        // Display a message if corners haven't been selected.
        if (c1 == null || c2 == null) {

            context.drawTextWithShadow(
                    client.textRenderer,
                    Text.literal("Dimensions HUD: Corners not set (Use U)"),
                    10,
                    10,
                    0xFFFF5555
            );

            return;
        }

        // Calculate dimensions.
        int length = Math.abs(c1.getX() - c2.getX()) + 1;
        int height = Math.abs(c1.getY() - c2.getY()) + 1;
        int width = Math.abs(c1.getZ() - c2.getZ()) + 1;

        // Prepare HUD text.
        String dimText = String.format(
                "Dimensions: %d L × %d W × %d H",
                length,
                width,
                height
        );

        String shapeText = "Shape: "
                + manager.getCurrentShapeRule().getName();

        // Render dimensions.
        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(dimText),
                10,
                10,
                0xFF00FF00
        );

        // Render shape.
        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(shapeText),
                10,
                22,
                0xFF00FFFF
        );
    }
}
