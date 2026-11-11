package dashketch.mods.simpleExcavator.client.gui;

import dashketch.mods.simpleExcavator.client.manager.ExcavatorManager;
import dashketch.mods.simpleExcavator.client.shape.IShapeRule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.Click;

import java.util.ArrayList;
import java.util.List;

public class ShapePreviewScreen extends Screen {
    private float rotationX = 30.0f;
    private float rotationY = 45.0f;
    private float zoom = 1.0f;
    private double lastMouseX, lastMouseY;
    private boolean isRotating = false;
    private String selectedEdgeInfo = "Hover over edges to highlight | Left-click to inspect edge length";

    private final List<BlockPos> previewBlocks = new ArrayList<>();
    private final ProjectedEdge[] currentEdges = new ProjectedEdge[12];
    private ProjectedEdge hoveredEdge = null;

    public static class ProjectedEdge {
        int x0, y0, x1, y1;
        String axis;
        int length;

        public ProjectedEdge(int x0, int y0, int x1, int y1, String axis, int length) {
            this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1;
            this.axis = axis; this.length = length;
        }

        public double distanceTo(double mx, double my) {
            double l2 = (double)(x1 - x0) * (x1 - x0) + (double)(y1 - y0) * (y1 - y0);
            if (l2 == 0) return Math.hypot(mx - x0, my - y0);
            double t = ((mx - x0) * (x1 - x0) + (my - y0) * (y1 - y0)) / l2;
            t = Math.max(0, Math.min(1, t));
            double projX = x0 + t * (x1 - x0);
            double projY = y0 + t * (y1 - y0);
            return Math.hypot(mx - projX, my - projY);
        }
    }

    public ShapePreviewScreen(Text title) {
        super(title);
        loadPreviewBlocks();
    }

    private void loadPreviewBlocks() {
        previewBlocks.clear();
        BlockPos c1 = ExcavatorManager.getInstance().getCorner1();
        BlockPos c2 = ExcavatorManager.getInstance().getCorner2();
        IShapeRule rule = ExcavatorManager.getInstance().getCurrentShapeRule();

        if (c1 == null || c2 == null || rule == null) return;

        int minX = Math.min(c1.getX(), c2.getX());
        int maxX = Math.max(c1.getX(), c2.getX());
        int minY = Math.min(c1.getY(), c2.getY());
        int maxY = Math.max(c1.getY(), c2.getY());
        int minZ = Math.min(c1.getZ(), c2.getZ());
        int maxZ = Math.max(c1.getZ(), c2.getZ());

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (rule.contains(x, y, z, minX, minY, minZ, maxX, maxY, maxZ)) {
                        previewBlocks.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
    }

    private void updateCurrentEdges() {
        BlockPos c1 = ExcavatorManager.getInstance().getCorner1();
        BlockPos c2 = ExcavatorManager.getInstance().getCorner2();
        if (c1 == null || c2 == null) return;

        // Perfectly aligned with block outer boundaries
        float minX = Math.min(c1.getX(), c2.getX());
        float maxX = Math.max(c1.getX(), c2.getX()) + 1.0f;
        float minY = Math.min(c1.getY(), c2.getY());
        float maxY = Math.max(c1.getY(), c2.getY()) + 1.0f;
        float minZ = Math.min(c1.getZ(), c2.getZ());
        float maxZ = Math.max(c1.getZ(), c2.getZ()) + 1.0f;

        int lenX = (int)(maxX - minX);
        int lenY = (int)(maxY - minY);
        int lenZ = (int)(maxZ - minZ);

        float centerX = (minX + maxX) / 2.0f;
        float centerY = (minY + maxY) / 2.0f;
        float centerZ = (minZ + maxZ) / 2.0f;

        float radX = (float) Math.toRadians(rotationX);
        float radY = (float) Math.toRadians(rotationY);
        float cosX = (float) Math.cos(radX), sinX = (float) Math.sin(radX);
        float cosY = (float) Math.cos(radY), sinY = (float) Math.sin(radY);

        float screenCenterX = this.width / 2.0f;
        float screenCenterY = this.height / 2.0f;

        float maxDim = Math.max(Math.max(lenX, lenY), lenZ);
        float scale = Math.min(120.0f / Math.max(1.0f, maxDim), 40.0f) * zoom;

        float[][] corners = {
                {minX, minY, minZ}, {maxX, minY, minZ}, {maxX, minY, maxZ}, {minX, minY, maxZ},
                {minX, maxY, minZ}, {maxX, maxY, minZ}, {maxX, maxY, maxZ}, {minX, maxY, maxZ}
        };

        float[][] proj = new float[8][2];
        for (int i = 0; i < 8; i++) {
            float px = corners[i][0] - centerX;
            float py = corners[i][1] - centerY;
            float pz = corners[i][2] - centerZ;

            float xz1 = px * cosY + pz * sinY;
            float zz1 = -px * sinY + pz * cosY;
            float yz1 = py * cosX - zz1 * sinX;

            proj[i][0] = screenCenterX + xz1 * scale;
            proj[i][1] = screenCenterY + yz1 * scale;
        }

        Object[][] edgeData = {
                {0, 1, "X", lenX}, {1, 2, "Z", lenZ}, {2, 3, "X", lenX}, {3, 0, "Z", lenZ},
                {4, 5, "X", lenX}, {5, 6, "Z", lenZ}, {6, 7, "X", lenX}, {7, 4, "Z", lenZ},
                {0, 4, "Y", lenY}, {1, 5, "Y", lenY}, {2, 6, "Y", lenY}, {3, 7, "Y", lenY}
        };

        for (int i = 0; i < 12; i++) {
            int p0 = (int) edgeData[i][0];
            int p1 = (int) edgeData[i][1];
            String axis = (String) edgeData[i][2];
            int length = (int) edgeData[i][3];

            int x0 = (int) proj[p0][0], y0 = (int) proj[p0][1];
            int x1 = (int) proj[p1][0], y1 = (int) proj[p1][1];

            currentEdges[i] = new ProjectedEdge(x0, y0, x1, y1, axis, length);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            isRotating = true;
            lastMouseX = click.x();
            lastMouseY = click.y();
            return true;
        } else if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            updateCurrentEdges();
            ProjectedEdge clickedEdge = findClosestEdge(click.x(), click.y());
            // Increased hit radius to 20 pixels for effortless clicking
            if (clickedEdge != null && clickedEdge.distanceTo(click.x(), click.y()) < 20.0) {
                selectedEdgeInfo = String.format("Selected Edge (%s-axis) Length: %d blocks", clickedEdge.axis, clickedEdge.length);
            } else {
                BlockPos c1 = ExcavatorManager.getInstance().getCorner1();
                BlockPos c2 = ExcavatorManager.getInstance().getCorner2();
                if (c1 != null && c2 != null) {
                    int dx = Math.abs(c1.getX() - c2.getX()) + 1;
                    int dy = Math.abs(c1.getY() - c2.getY()) + 1;
                    int dz = Math.abs(c1.getZ() - c2.getZ()) + 1;
                    selectedEdgeInfo = String.format("Box Dimensions -> X: %d | Y: %d | Z: %d", dx, dy, dz);
                }
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
            rotationY -= (float) (click.x() - lastMouseX) * 0.8f;
            rotationX -= (float) (click.y() - lastMouseY) * 0.8f;
            lastMouseX = click.x();
            lastMouseY = click.y();
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        zoom = Math.clamp((float) (zoom + verticalAmount * 0.15f), 0.3f, 4.0f);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xEE101010);
        super.render(context, mouseX, mouseY, delta);

        BlockPos c1 = ExcavatorManager.getInstance().getCorner1();
        BlockPos c2 = ExcavatorManager.getInstance().getCorner2();
        IShapeRule rule = ExcavatorManager.getInstance().getCurrentShapeRule();

        if (c1 != null && c2 != null && rule != null) {
            renderShapePreview3D(context, c1, c2, mouseX, mouseY);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Corners not set yet! Open config (U) to set them."), this.width / 2, this.height / 2, 0xFF5555);
        }

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Hold Right-Click & Drag to Rotate | Scroll to Zoom"), this.width / 2, 30, 0xAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(selectedEdgeInfo), this.width / 2, this.height - 25, 0x55FF55);
    }

    private ProjectedEdge findClosestEdge(double mx, double my) {
        double minDist = Double.MAX_VALUE;
        ProjectedEdge best = null;
        for (ProjectedEdge e : currentEdges) {
            if (e == null) continue;
            double dist = e.distanceTo(mx, my);
            if (dist < minDist) {
                minDist = dist;
                best = e;
            }
        }
        return best;
    }

    private void renderShapePreview3D(DrawContext context, BlockPos c1, BlockPos c2, int mouseX, int mouseY) {
        float minX = Math.min(c1.getX(), c2.getX());
        float maxX = Math.max(c1.getX(), c2.getX()) + 1.0f;
        float minY = Math.min(c1.getY(), c2.getY());
        float maxY = Math.max(c1.getY(), c2.getY()) + 1.0f;
        float minZ = Math.min(c1.getZ(), c2.getZ());
        float maxZ = Math.max(c1.getZ(), c2.getZ()) + 1.0f;

        float centerX = (minX + maxX) / 2.0f;
        float centerY = (minY + maxY) / 2.0f;
        float centerZ = (minZ + maxZ) / 2.0f;

        float radX = (float) Math.toRadians(rotationX);
        float radY = (float) Math.toRadians(rotationY);
        float cosX = (float) Math.cos(radX), sinX = (float) Math.sin(radX);
        float cosY = (float) Math.cos(radY), sinY = (float) Math.sin(radY);

        float screenCenterX = this.width / 2.0f;
        float screenCenterY = this.height / 2.0f;

        int lenX = (int)(maxX - minX);
        int lenY = (int)(maxY - minY);
        int lenZ = (int)(maxZ - minZ);
        float maxDim = Math.max(Math.max(lenX, lenY), lenZ);
        float scale = Math.min(120.0f / Math.max(1.0f, maxDim), 40.0f) * zoom;

        // 1. Render actual shape blocks centered correctly
        for (BlockPos pos : previewBlocks) {
            float bx = pos.getX() - centerX + 0.5f;
            float by = pos.getY() - centerY + 0.5f;
            float bz = pos.getZ() - centerZ + 0.5f;

            float xz = bx * cosY + bz * sinY;
            float zz = -bx * sinY + bz * cosY;
            float yz = by * cosX - zz * sinX;

            int screenX = (int)(screenCenterX + xz * scale);
            int screenY = (int)(screenCenterY + yz * scale);

            if (screenX >= 0 && screenX < this.width && screenY >= 0 && screenY < this.height) {
                context.fill(screenX - 1, screenY - 1, screenX + 1, screenY + 1, 0x5500FFFF);
            }
        }

        // 2. Update and render Bounding Box Edges with hover detection
        updateCurrentEdges();

        hoveredEdge = findClosestEdge(mouseX, mouseY);

        for (ProjectedEdge e : currentEdges) {
            if (e == null) continue;
            int color = (e == hoveredEdge && e.distanceTo(mouseX, mouseY) < 20.0) ? 0xFFFFFF00 : 0xFF00FFFF;
            drawLine(context, e.x0, e.y0, e.x1, e.y1, color);
        }

        String shapeName = "Shape: " + ExcavatorManager.getInstance().getCurrentShapeRule().getName();
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(shapeName), (int)screenCenterX, (int)screenCenterY - 12, 0xFFD700);
    }

    private void drawLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) {
            context.fill(x0, y0, x0 + 1, y0 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (dx * i) / steps;
            int y = y0 + (dy * i) / steps;
            if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
                context.fill(x, y, x + 1, y + 1, color);
            }
        }
    }
}