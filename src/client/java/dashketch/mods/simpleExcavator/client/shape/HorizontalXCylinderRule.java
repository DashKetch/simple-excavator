package dashketch.mods.simpleExcavator.client.shape;

public class HorizontalXCylinderRule implements IShapeRule {
    @Override
    public String getName() {
        return "H-Cylinder (X)";
    }

    @Override
    public boolean contains(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        double cz = (minZ + maxZ + 1.0) / 2.0;
        double cy = (minY + maxY + 1.0) / 2.0;

        double rz = Math.max(0.5, (maxZ - minZ + 1.0) / 2.0);
        double ry = Math.max(0.5, (maxY - minY + 1.0) / 2.0);

        double dz = (z + 0.5 - cz) / rz;
        double dy = (y + 0.5 - cy) / ry;

        return (dz * dz + dy * dy) <= 1.0001;
    }
}