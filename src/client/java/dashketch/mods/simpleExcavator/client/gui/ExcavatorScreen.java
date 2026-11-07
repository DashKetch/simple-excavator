package dashketch.mods.simpleExcavator.client.gui;

import dashketch.mods.simpleExcavator.client.manager.ExcavatorManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class ExcavatorScreen extends Screen {

    public ExcavatorScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Shape cycle button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Shape: " + ExcavatorManager.getInstance().getCurrentShape().name()), button -> {
                    ExcavatorManager.ShapeType next = switch (ExcavatorManager.getInstance().getCurrentShape()) {
                        case RECTANGLE -> ExcavatorManager.ShapeType.SPHERE;
                        case SPHERE -> ExcavatorManager.ShapeType.VERTICAL_CYLINDER;
                        case VERTICAL_CYLINDER -> ExcavatorManager.ShapeType.HORIZONTAL_CYLINDER;
                        case HORIZONTAL_CYLINDER -> ExcavatorManager.ShapeType.RECTANGLE;
                    };
                    ExcavatorManager.getInstance().setShape(next);
                    button.setMessage(Text.literal("Shape: " + next.name()));
                }
        ).dimensions(centerX - 100, centerY - 60, 200, 20).build());

        // Set Corner 1 to player position
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Set Corner 1 (Here)"), button -> {
                    if (this.client != null && this.client.player != null) {
                        BlockPos pos = this.client.player.getBlockPos();
                        ExcavatorManager.getInstance().setCorner1(pos);
                    }
                }
        ).dimensions(centerX - 100, centerY - 30, 200, 20).build());

        // Set Corner 2 to player position
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Set Corner 2 (Here)"), button -> {
                    if (this.client != null && this.client.player != null) {
                        BlockPos pos = this.client.player.getBlockPos();
                        ExcavatorManager.getInstance().setCorner2(pos);
                    }
                }
        ).dimensions(centerX - 100, centerY, 200, 20).build());

        // Close / Done button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"), button -> this.close()
        ).dimensions(centerX - 100, centerY + 30, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        // Display current corner coordinates
        BlockPos c1 = ExcavatorManager.getInstance().getCorner1();
        BlockPos c2 = ExcavatorManager.getInstance().getCorner2();

        String c1Str = c1 != null ? String.format("Corner 1: (%d, %d, %d)", c1.getX(), c1.getY(), c1.getZ()) : "Corner 1: Not Set";
        String c2Str = c2 != null ? String.format("Corner 2: (%d, %d, %d)", c2.getX(), c2.getY(), c2.getZ()) : "Corner 2: Not Set";

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(c1Str), this.width / 2, 50, 0xAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(c2Str), this.width / 2, 65, 0xAAAAAA);
    }
}