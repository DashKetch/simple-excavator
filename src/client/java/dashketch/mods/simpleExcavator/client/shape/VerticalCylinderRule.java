package dashketch.mods.simpleExcavator.client.shape;

public class VerticalCylinderRule implements IShapeRule {
    @Override
    public String getName() {
        return "Vertical Cylinder";
    }

    @Override
    public boolean contains(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        double cx = (minX + maxX + 1.0) / 2.0;
        double cz = (minZ + maxZ + 1.0) / 2.0;

        double rx = (maxX - minX + 1.0) / 2.0;
        double rz = (maxZ - minZ + 1.0) / 2.0;

        double dx = (x + 0.5 - cx) / rx;
        double dz = (z + 0.5 - cz) / rz;

        return (dx * dx + dz * dz) <= 1.0001;
    }
}