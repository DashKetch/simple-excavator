package dashketch.mods.simpleExcavator.client.manager;

import dashketch.mods.simpleExcavator.client.shape.IShapeRule;
import dashketch.mods.simpleExcavator.client.shape.ShapeRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExcavatorManager {
    private static final ExcavatorManager INSTANCE = new ExcavatorManager();

    private IShapeRule currentShapeRule = ShapeRegistry.getShape(0);
    private int shapeIndex = 0;
    private BlockPos corner1 = null;
    private BlockPos corner2 = null;
    private boolean isRunning = false;
    private final List<BlockPos> targetQueue = new ArrayList<>();
    private int currentIndex = 0;
    private BlockPos currentBreakingTarget = null;
    private int breakTicks = 0;

    public static ExcavatorManager getInstance() {
        return INSTANCE;
    }

    public void nextShape() {
        shapeIndex++;
        currentShapeRule = ShapeRegistry.getShape(shapeIndex);
        recalculateQueue();
    }

    public IShapeRule getCurrentShapeRule() {
        return currentShapeRule;
    }

    public void setCorner1(BlockPos pos) {
        this.corner1 = pos;
        recalculateQueue();
    }

    public void setCorner2(BlockPos pos) {
        this.corner2 = pos;
        recalculateQueue();
    }

    public BlockPos getCorner1() { return corner1; }
    public BlockPos getCorner2() { return corner2; }

    public void toggleExcavation(MinecraftClient client) {
        if (corner1 == null || corner2 == null) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c[SimpleExcavator] Please set both Corner 1 and Corner 2 in the GUI first (Press U)!"), true);
            }
            return;
        }
        isRunning = !isRunning;
        if (isRunning) {
            recalculateQueue();
            currentIndex = 0;
            currentBreakingTarget = null;
            breakTicks = 0;
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§a[SimpleExcavator] Excavation Started! (" + targetQueue.size() + " blocks)"), true);
            }
        } else {
            stopExcavation(client);
        }
    }

    private void stopExcavation(MinecraftClient client) {
        isRunning = false;
        currentBreakingTarget = null;
        breakTicks = 0;
        if (client.options != null) {
            client.options.forwardKey.setPressed(false);
            client.options.jumpKey.setPressed(false);
        }
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§e[SimpleExcavator] Excavation Paused."), true);
        }
    }

    public void recalculateQueue() {
        targetQueue.clear();
        if (corner1 == null || corner2 == null || currentShapeRule == null) return;

        int minX = Math.min(corner1.getX(), corner2.getX());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        // Iterate top-down for safety
        for (int y = maxY; y >= minY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (currentShapeRule.contains(x, y, z, minX, minY, minZ, maxX, maxY, maxZ)) {
                        targetQueue.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

        // Sort by distance to player for efficient pathing
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Vec3d playerPos = client.player.getEntityPos();
            targetQueue.sort(Comparator.comparingDouble(bp -> bp.getSquaredDistance(playerPos)));
        }
    }

    public void tick(MinecraftClient client) {
        if (!isRunning || client.player == null || client.world == null || client.interactionManager == null) return;

        while (currentIndex < targetQueue.size()) {
            BlockPos target = targetQueue.get(currentIndex);

            if (client.world.isAir(target)) {
                if (target.equals(currentBreakingTarget)) {
                    currentBreakingTarget = null;
                    breakTicks = 0;
                }
                currentIndex++;
                continue;
            }

            double distSq = client.player.getBlockPos().getSquaredDistance(target);
            if (distSq > 20.0) {
                Vec3d targetVec = Vec3d.ofCenter(target);
                double dx = targetVec.x - client.player.getX();
                double dy = targetVec.y - client.player.getEyeY();
                double dz = targetVec.z - client.player.getZ();
                double distXZ = Math.sqrt(dx * dx + dz * dz);

                float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
                float pitch = (float) -(Math.atan2(dy, distXZ) * (180.0 / Math.PI));

                client.player.setYaw(yaw);
                client.player.setPitch(pitch);

                // Walk forward toward target
                client.options.forwardKey.setPressed(true);

                // Auto-jump if hitting a block/step obstacle in front of the player
                client.options.jumpKey.setPressed(client.player.horizontalCollision);
                return;
            }

            // In range: release movement keys and mine
            client.options.forwardKey.setPressed(false);
            client.options.jumpKey.setPressed(false);

            Vec3d targetVec = Vec3d.ofCenter(target);
            double dx = targetVec.x - client.player.getX();
            double dy = targetVec.y - client.player.getEyeY();
            double dz = targetVec.z - client.player.getZ();
            double distXZ = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            float pitch = (float) -(Math.atan2(dy, distXZ) * (180.0 / Math.PI));

            client.player.setYaw(yaw);
            client.player.setPitch(pitch);

            if (!target.equals(currentBreakingTarget)) {
                currentBreakingTarget = target;
                breakTicks = 0;
                client.interactionManager.attackBlock(target, Direction.UP);
            }

            client.interactionManager.updateBlockBreakingProgress(target, Direction.UP);
            client.player.swingHand(client.player.getActiveHand());
            breakTicks++;

            if (breakTicks > 40) {
                client.interactionManager.attackBlock(target, Direction.UP);
                breakTicks = 0;
            }
            return;
        }

        stopExcavation(client);
        client.player.sendMessage(Text.literal("§a[SimpleExcavator] Excavation completed!"), true);
    }

    public void renderHighlight(WorldRenderContext context) {
        if (corner1 == null || corner2 == null || targetQueue.isEmpty()) return;

        MatrixStack matrices = context.matrices();
        Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumerProvider.Immediate vertexConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayers.lines());

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int renderedCount = 0;
        int maxRender = 300;

        for (int i = currentIndex; i < targetQueue.size() && renderedCount < maxRender; i++) {
            BlockPos pos = targetQueue.get(i);

            if (pos.getSquaredDistance(cameraPos.x, cameraPos.y, cameraPos.z) <= 576.0) {
                drawBoxOutline(matrix, buffer, new Box(pos));
                renderedCount++;
            }
        }

        matrices.pop();
    }

    private void drawBoxOutline(Matrix4f matrix, VertexConsumer buffer, Box box) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        drawLine(matrix, buffer, minX, minY, minZ, maxX, minY, minZ);
        drawLine(matrix, buffer, maxX, minY, minZ, maxX, minY, maxZ);
        drawLine(matrix, buffer, maxX, minY, maxZ, minX, minY, maxZ);
        drawLine(matrix, buffer, minX, minY, maxZ, minX, minY, minZ);

        drawLine(matrix, buffer, minX, maxY, minZ, maxX, maxY, minZ);
        drawLine(matrix, buffer, maxX, maxY, minZ, maxX, maxY, maxZ);
        drawLine(matrix, buffer, maxX, maxY, maxZ, minX, maxY, maxZ);
        drawLine(matrix, buffer, minX, maxY, maxZ, minX, maxY, minZ);

        drawLine(matrix, buffer, minX, minY, minZ, minX, maxY, minZ);
        drawLine(matrix, buffer, maxX, minY, minZ, maxX, maxY, minZ);
        drawLine(matrix, buffer, maxX, minY, maxZ, maxX, maxY, maxZ);
        drawLine(matrix, buffer, minX, minY, maxZ, minX, maxY, maxZ);
    }

    private void drawLine(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2) {
        buffer.vertex(matrix, x1, y1, z1).color(0.0f, 1.0f, 1.0f, 0.5f).normal(0, 1, 0).lineWidth(1.0f);
        buffer.vertex(matrix, x2, y2, z2).color(0.0f, 1.0f, 1.0f, 0.5f).normal(0, 1, 0).lineWidth(1.0f);
    }
}