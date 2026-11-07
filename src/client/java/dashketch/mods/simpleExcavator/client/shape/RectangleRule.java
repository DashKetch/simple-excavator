package dashketch.mods.simpleExcavator.client.shape;

public class RectangleRule implements IShapeRule {
    @Override
    public String getName() {
        return "Rectangle";
    }

    @Override
    public boolean contains(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return true;
    }
}