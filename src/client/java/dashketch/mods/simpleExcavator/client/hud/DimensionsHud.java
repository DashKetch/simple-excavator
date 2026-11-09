package dashketch.mods.simpleExcavator.client.hud;

import dashketch.mods.simpleExcavator.client.manager.ExcavatorManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

public class DimensionsHud {
    private static boolean visible = true;

    public static void toggleHud() {
        visible = !visible;
    }

    public static void render(DrawContext context) {
        if (!visible) return;

        BlockPos c1 = ExcavatorManager.getInstance().getCorner1();
        BlockPos c2 = ExcavatorManager.getInstance().getCorner2();
        if (c1 == null || c2 == null) return;

        int length = Math.abs(c1.getX() - c2.getX()) + 1;
        int height = Math.abs(c1.getY() - c2.getY()) + 1;
        int width = Math.abs(c1.getZ() - c2.getZ()) + 1;

        String dimText = String.format("Dimensions: %d L × %d W × %d H", length, width, height);
        String shapeText = "Shape: " + ExcavatorManager.getInstance().getCurrentShapeRule().getName();

        MinecraftClient client = MinecraftClient.getInstance();
        context.drawTextWithShadow(client.textRenderer, dimText, 10, 10, 0x00FF00);
        context.drawTextWithShadow(client.textRenderer, shapeText, 10, 22, 0x00FFFF);
    }
}