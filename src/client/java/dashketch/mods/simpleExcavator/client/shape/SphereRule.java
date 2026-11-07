package dashketch.mods.simpleExcavator.client.shape;

public class SphereRule implements IShapeRule {
    @Override
    public String getName() {
        return "Sphere";
    }

    @Override
    public boolean contains(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;
        double cz = (minZ + maxZ) / 2.0;

        double rx = Math.max(0.5, (maxX - minX) / 2.0);
        double ry = Math.max(0.5, (maxY - minY) / 2.0);
        double rz = Math.max(0.5, (maxZ - minZ) / 2.0);

        double dx = (x + 0.5 - cx) / rx;
        double dy = (y + 0.5 - cy) / ry;
        double dz = (z + 0.5 - cz) / rz;

        return (dx * dx + dy * dy + dz * dz) <= 1.0;
    }
}