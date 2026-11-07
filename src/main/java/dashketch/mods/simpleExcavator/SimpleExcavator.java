package dashketch.mods.simpleExcavator;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleExcavator implements ModInitializer {
    public static final String MOD_ID = "simple_excavator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Simple Excavator mod initialized.");
    }
}