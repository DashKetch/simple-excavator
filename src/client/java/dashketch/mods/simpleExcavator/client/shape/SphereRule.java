package dashketch.mods.simpleExcavator.client.shape;

public class SphereRule implements IShapeRule {
    @Override
    public String getName() {
        return "Sphere";
    }

    @Override
    public boolean contains(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        double cx = (minX + maxX + 1.0) / 2.0;
        double cy = (minY + maxY + 1.0) / 2.0;
        double cz = (minZ + maxZ + 1.0) / 2.0;

        double rx = (maxX - minX + 1.0) / 2.0;
        double ry = (maxY - minY + 1.0) / 2.0;
        double rz = (maxZ - minZ + 1.0) / 2.0;

        double dx = (x + 0.5 - cx) / rx;
        double dy = (y + 0.5 - cy) / ry;
        double dz = (z + 0.5 - cz) / rz;

        return (dx * dx + dy * dy + dz * dz) <= 1.0001;
    }
}