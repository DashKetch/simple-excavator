package dashketch.mods.simpleExcavator.client.shape;

public interface IShapeRule {
    String getName();
    boolean contains(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ);
}