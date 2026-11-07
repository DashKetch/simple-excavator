package dashketch.mods.simpleExcavator.client.manager;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExcavatorManager {
    private static final ExcavatorManager INSTANCE = new ExcavatorManager();

    /**
     * Added to the player's block interaction range (4.5) when deciding if a block is "in reach".
     * Negative = stay safely inside what the server will accept (it allows ~1 block of slack on top of 4.5).
     */
    private static final double REACH_MARGIN = -0.5;
    /** How many times a block may be rolled back / stall before we stop trying it. */
    private static final int MAX_ATTEMPTS = 4;
    /** Safety valve on full re-scans of the shape. */
    private static final int MAX_PASSES = 30;
    /**
     * Stop entirely after this many rollbacks in a row with no confirmed break in between.
     * That pattern means "the server will not let us mine here" (protected area, anticheat),
     * and grinding on only racks up more rejected breaks.
     */
    private static final int MAX_CONSECUTIVE_ROLLBACKS = 8;
    /** Ticks we will walk towards a block before giving up on it. */
    private static final int MAX_WALK_TICKS = 200;
    /** Extra ticks (on top of the expected break time) before a break counts as stalled. */
    private static final int STALL_EXTRA_TICKS = 60;

    public enum ShapeType { RECTANGLE, SPHERE, VERTICAL_CYLINDER, HORIZONTAL_CYLINDER }

    /** A block that *looks* broken on the client but has not been confirmed by the server yet. */
    private record PendingBreak(BlockPos pos, int tick) {}

    private ShapeType currentShape = ShapeType.RECTANGLE;
    private BlockPos corner1 = null;
    private BlockPos corner2 = null;
    private boolean isRunning = false;

    private final List<BlockPos> targetQueue = new ArrayList<>();
    private int currentIndex = 0;

    // Blocks the server rolled back; they jump the queue and get mined again.
    private final Deque<BlockPos> retryQueue = new ArrayDeque<>();
    // Blocks we broke client-side and are waiting on the server to confirm.
    private final List<PendingBreak> pending = new ArrayList<>();
    private final Map<BlockPos, Integer> attempts = new HashMap<>();
    private final Set<BlockPos> gaveUp = new HashSet<>();

    private BlockPos currentBreakingTarget = null;
    private BlockPos aimedTarget = null;
    private int breakTicks = 0;
    private int walkTicks = 0;
    private int tickCounter = 0;
    private int pass = 0;
    private int rollbackCount = 0;
    private int consecutiveRollbacks = 0;

    public static ExcavatorManager getInstance() {
        return INSTANCE;
    }

    public void setShape(ShapeType shape) {
        this.currentShape = shape;
        clearProgress();
        recalculateQueue();
    }

    public void setCorner1(BlockPos pos) {
        this.corner1 = pos;
        clearProgress();
        recalculateQueue();
    }

    public void setCorner2(BlockPos pos) {
        this.corner2 = pos;
        clearProgress();
        recalculateQueue();
    }

    public BlockPos getCorner1() { return corner1; }
    public BlockPos getCorner2() { return corner2; }
    public ShapeType getCurrentShape() { return currentShape; }

    public void toggleExcavation(MinecraftClient client) {
        if (corner1 == null || corner2 == null) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c[SimpleExcavator] Please set both Corner 1 and Corner 2 in the GUI first (Press U)!"), true);
            }
            return;
        }
        isRunning = !isRunning;
        if (isRunning) {
            clearProgress();
            recalculateQueue();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§a[SimpleExcavator] Fully Synced Excavation Started! (" + targetQueue.size() + " blocks)"), true);
            }
        } else {
            stopExcavation(client);
        }
    }

    private void clearProgress() {
        currentIndex = 0;
        currentBreakingTarget = null;
        aimedTarget = null;
        breakTicks = 0;
        walkTicks = 0;
        pass = 0;
        rollbackCount = 0;
        consecutiveRollbacks = 0;
        pending.clear();
        retryQueue.clear();
        attempts.clear();
        gaveUp.clear();
    }

    private void clearCurrentTarget() {
        currentBreakingTarget = null;
        breakTicks = 0;
    }

    /** Shared cleanup: stop walking and tell the server we are no longer mining anything. */
    private void halt(MinecraftClient client) {
        isRunning = false;
        if (client.interactionManager != null && client.player != null && client.world != null) {
            client.interactionManager.cancelBlockBreaking();
        }
        clearCurrentTarget();
        if (client.options != null) {
            client.options.forwardKey.setPressed(false);
        }
    }

    private void stopExcavation(MinecraftClient client) {
        halt(client);
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§e[SimpleExcavator] Excavation Paused."), true);
        }
    }

    private void finishExcavation(MinecraftClient client) {
        int unmined = countMineable(client, false);
        halt(client);
        if (client.player != null) {
            String msg = unmined == 0
                    ? "§a[SimpleExcavator] Excavation completed!"
                    : "§e[SimpleExcavator] Finished, but " + unmined + " block(s) could not be mined.";
            if (rollbackCount > 0) {
                msg += " §7(" + rollbackCount + " server rollbacks retried)";
            }
            client.player.sendMessage(Text.literal(msg), true);
        }
    }

    public void recalculateQueue() {
        targetQueue.clear();
        currentIndex = 0;
        if (corner1 == null || corner2 == null) return;

        int minX = Math.min(corner1.getX(), corner2.getX());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;

        double rx = Math.max(0.5, (maxX - minX) / 2.0);
        double ry = Math.max(0.5, (maxY - minY) / 2.0);
        double rz = Math.max(0.5, (maxZ - minZ) / 2.0);

        // Iterate top-down for safety
        for (int y = maxY; y >= minY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (matchShape(x, y, z, centerX, centerY, centerZ, rx, ry, rz)) {
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

    private boolean matchShape(int x, int y, int z, double cx, double cy, double cz, double rx, double ry, double rz) {
        return switch (currentShape) {
            case RECTANGLE -> true;
            case SPHERE -> {
                double dx = (x - cx) / rx;
                double dy = (y - cy) / ry;
                double dz = (z - cz) / rz;
                yield (dx * dx + dy * dy + dz * dz) <= 1.0001;
            }
            case VERTICAL_CYLINDER -> {
                double dx = (x - cx) / rx;
                double dz = (z - cz) / rz;
                yield (dx * dx + dz * dz) <= 1.0001;
            }
            case HORIZONTAL_CYLINDER -> {
                double dx = (x - cx) / rx;
                double dy = (y - cy) / ry;
                yield (dx * dx + dy * dy) <= 1.0001;
            }
        };
    }

    /** Something we can actually dig: not air, not a bare fluid block, not unbreakable (bedrock, barriers...). */
    private boolean isMineable(MinecraftClient client, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (state.getBlock() instanceof FluidBlock) return false;
        return state.getHardness(client.world, pos) >= 0.0f;
    }

    private int countMineable(MinecraftClient client, boolean skipGaveUp) {
        int count = 0;
        for (BlockPos pos : targetQueue) {
            if (skipGaveUp && gaveUp.contains(pos)) continue;
            if (isMineable(client, pos, client.world.getBlockState(pos))) count++;
        }
        return count;
    }

    /** Re-scan the whole shape. Returns true if there is still something left to mine. */
    private boolean beginNextPass(MinecraftClient client) {
        if (pass >= MAX_PASSES) return false;
        pass++;
        recalculateQueue();
        return countMineable(client, true) > 0;
    }

    /** How many ticks to wait before trusting that the server accepted a break (scaled by ping). */
    private int confirmDelayTicks(MinecraftClient client) {
        int latencyMs = 0;
        if (client.getNetworkHandler() != null && client.player != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (entry != null) latencyMs = entry.getLatency();
        }
        return Math.max(4, latencyMs / 50 + 4);
    }

    private void recordFailure(BlockPos pos) {
        int n = attempts.merge(pos, 1, Integer::sum);
        if (n >= MAX_ATTEMPTS) {
            gaveUp.add(pos);
        }
    }

    /**
     * The client "predicts" a break instantly, so the block looks like air for a moment. If the server
     * disagrees it sends the block back. Check every predicted break after a round trip and re-queue
     * anything that came back.
     */
    private void verifyPendingBreaks(MinecraftClient client) {
        int delay = confirmDelayTicks(client);
        Iterator<PendingBreak> it = pending.iterator();
        while (it.hasNext()) {
            PendingBreak pb = it.next();
            if (tickCounter - pb.tick() < delay) continue;
            it.remove();

            if (client.world.getBlockState(pb.pos()).isAir()) {
                consecutiveRollbacks = 0; // server agreed
                continue;
            }

            rollbackCount++;
            consecutiveRollbacks++;
            recordFailure(pb.pos());
            if (!gaveUp.contains(pb.pos())) {
                retryQueue.addLast(pb.pos());
            }

            if (consecutiveRollbacks >= MAX_CONSECUTIVE_ROLLBACKS) {
                abortForRollbacks(client);
                return;
            }
        }
    }

    private void abortForRollbacks(MinecraftClient client) {
        halt(client);
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§c[SimpleExcavator] Server keeps rolling back breaks here (protected area or anticheat?). Stopped."), true);
        }
    }

    private void advance(boolean fromRetry) {
        if (fromRetry) {
            retryQueue.pollFirst();
        } else {
            currentIndex++;
        }
    }

    public void tick(MinecraftClient client) {
        if (!isRunning || client.player == null || client.world == null || client.interactionManager == null) return;

        tickCounter++;
        verifyPendingBreaks(client);
        if (!isRunning) return; // verification may have aborted the run

        while (true) {
            // Rolled-back blocks go first, then the normal queue.
            boolean fromRetry = !retryQueue.isEmpty();
            BlockPos target;
            if (fromRetry) {
                target = retryQueue.peekFirst();
            } else if (currentIndex < targetQueue.size()) {
                target = targetQueue.get(currentIndex);
            } else {
                // Queue drained. Don't finish until the server has confirmed everything,
                // then re-scan the shape for anything that came back (or fell in).
                if (!pending.isEmpty()) return;
                if (beginNextPass(client)) continue;
                finishExcavation(client);
                return;
            }

            BlockState state = client.world.getBlockState(target);

            // 1. Looks like air. If it was the block we were breaking, this is only a client-side
            //    prediction: don't trust it yet, let verifyPendingBreaks() confirm it with the server.
            if (state.isAir()) {
                if (target.equals(currentBreakingTarget)) {
                    pending.add(new PendingBreak(target, tickCounter));
                }
                clearCurrentTarget();
                advance(fromRetry);
                continue;
            }

            // 2. Fluids, bedrock, blocks we gave up on.
            if (gaveUp.contains(target) || !isMineable(client, target, state)) {
                if (target.equals(currentBreakingTarget)) clearCurrentTarget();
                advance(fromRetry);
                continue;
            }

            // 3. Out of reach (measured from the eyes, like the server does): walk closer.
            if (!client.player.canInteractWithBlockAt(target, REACH_MARGIN)) {
                if (++walkTicks > MAX_WALK_TICKS) {
                    walkTicks = 0;
                    gaveUp.add(target);
                    advance(fromRetry);
                    continue;
                }
                lookAt(client, target);
                client.options.forwardKey.setPressed(true);
                return;
            }

            // 4. In reach: stop walking and aim.
            walkTicks = 0;
            client.options.forwardKey.setPressed(false);
            lookAt(client, target);

            // The rotation we just set only reaches the server with the *next* movement packet.
            // Give it one tick so the server sees us looking at the block before the dig packets.
            if (!target.equals(aimedTarget)) {
                aimedTarget = target;
                return;
            }

            Direction face = faceTowards(client, target);

            if (!target.equals(currentBreakingTarget)) {
                currentBreakingTarget = target;
                breakTicks = 0;
                client.interactionManager.attackBlock(target, face);
                // Insta-mine: attackBlock already removed it client-side. Handled next tick (step 1).
                if (client.world.getBlockState(target).isAir()) return;
            }

            client.interactionManager.updateBlockBreakingProgress(target, face);
            client.player.swingHand(Hand.MAIN_HAND);
            breakTicks++;

            // Stall detection scaled to how long this block should actually take
            // (a fixed 40 ticks would constantly cancel slow blocks like obsidian).
            float delta = state.calcBlockBreakingDelta(client.player, client.world, target);
            int expectedTicks = delta > 0 ? (int) Math.ceil(1.0f / delta) : 0;
            if (breakTicks > expectedTicks + STALL_EXTRA_TICKS) {
                client.interactionManager.cancelBlockBreaking(); // sends ABORT so the next attack is a real fresh start
                recordFailure(target);
                clearCurrentTarget();
            }
            return;
        }
    }

    private void lookAt(MinecraftClient client, BlockPos target) {
        Vec3d targetVec = Vec3d.ofCenter(target);
        double dx = targetVec.x - client.player.getX();
        double dy = targetVec.y - client.player.getEyeY();
        double dz = targetVec.z - client.player.getZ();
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(Math.atan2(dy, distXZ) * (180.0 / Math.PI));

        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
    }

    /** The face of the block that points at the player's eyes (what a real click would hit). */
    private Direction faceTowards(MinecraftClient client, BlockPos pos) {
        Vec3d c = Vec3d.ofCenter(pos);
        double dx = client.player.getX() - c.x;
        double dy = client.player.getEyeY() - c.y;
        double dz = client.player.getZ() - c.z;
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);

        if (ay >= ax && ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
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

        for (int i = currentIndex; i < targetQueue.size(); i++) {
            BlockPos pos = targetQueue.get(i);
            drawBoxOutline(matrix, buffer, new Box(pos), 0.0f, 1.0f, 1.0f, 0.5f);
        }

        matrices.pop();
    }

    private void drawBoxOutline(Matrix4f matrix, VertexConsumer buffer, Box box, float red, float green, float blue, float alpha) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        drawLine(matrix, buffer, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        drawLine(matrix, buffer, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
        drawLine(matrix, buffer, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        drawLine(matrix, buffer, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha);

        drawLine(matrix, buffer, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        drawLine(matrix, buffer, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        drawLine(matrix, buffer, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        drawLine(matrix, buffer, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);

        drawLine(matrix, buffer, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        drawLine(matrix, buffer, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        drawLine(matrix, buffer, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        drawLine(matrix, buffer, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
    }

    private void drawLine(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(1.0f);
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(1.0f);
    }
}