package dashketch.mods.simpleExcavator.client.shape;

public class HorizontalZCylinderRule implements IShapeRule {
    @Override
    public String getName() {
        return "H-Cylinder (Z)";
    }

    @Override
    public boolean contains(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        double cx = (minX + maxX + 1.0) / 2.0;
        double cy = (minY + maxY + 1.0) / 2.0;

        double rx = Math.max(0.5, (maxX - minX + 1.0) / 2.0);
        double ry = Math.max(0.5, (maxY - minY + 1.0) / 2.0);

        double dx = (x + 0.5 - cx) / rx;
        double dy = (y + 0.5 - cy) / ry;

        return (dx * dx + dy * dy) <= 1.0001;
    }
}