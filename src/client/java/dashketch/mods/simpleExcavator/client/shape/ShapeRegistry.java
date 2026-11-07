package dashketch.mods.simpleExcavator.client.shape;

import java.util.ArrayList;
import java.util.List;

public class ShapeRegistry {
    private static final List<IShapeRule> SHAPES = new ArrayList<>();

    static {
        // Register default shape rules here.
        // To add a new shape, create its class implementing IShapeRule and register it here
        register(new RectangleRule());
        register(new SphereRule());
        register(new VerticalCylinderRule());
        register(new HorizontalZCylinderRule());
        register(new HorizontalXCylinderRule());
    }

    public static void register(IShapeRule shape) {
        SHAPES.add(shape);
    }

    public static List<IShapeRule> getShapes() {
        return SHAPES;
    }

    public static IShapeRule getShape(int index) {
        if (SHAPES.isEmpty()) return null;
        return SHAPES.get(Math.floorMod(index, SHAPES.size()));
    }
}