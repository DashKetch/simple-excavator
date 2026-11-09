package dashketch.mods.simpleExcavator.client.gui;

import dashketch.mods.simpleExcavator.client.manager.ExcavatorManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.Click;

public class ShapePreviewScreen extends Screen {
    private float rotationX = 30.0f;
    private float rotationY = 45.0f;
    private float zoom = 1.0f;
    private double lastMouseX, lastMouseY;
    private boolean isRotating = false;

    public ShapePreviewScreen(Text title) {
        super(title);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            isRotating = true;
            lastMouseX = click.x();
            lastMouseY = click.y();
            return true;
        } else if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            BlockPos c1 = ExcavatorManager.getInstance().getCorner1();
            BlockPos c2 = ExcavatorManager.getInstance().getCorner2();
            if (c1 != null && c2 != null) {
                int dx = Math.abs(c1.getX() - c2.getX()) + 1;
                int dy = Math.abs(c1.getY() - c2.getY()) + 1;
                int dz = Math.abs(c1.getZ() - c2.getZ()) + 1;
                String selectedEdgeInfo = String.format("Selected Edge Length -> X:%d, Y:%d, Z:%d", dx, dy, dz);
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            isRotating = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (isRotating) {
            rotationY += (float) (click.x() - lastMouseX) * 0.8f;
            rotationX -= (float) (click.y() - lastMouseY) * 0.8f;
            lastMouseX = click.x();
            lastMouseY = click.y();
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        zoom = Math.clamp((float) (zoom + verticalAmount * 0.1f), 0.4f, 3.0f);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}