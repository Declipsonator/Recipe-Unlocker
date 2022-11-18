package me.declipsonator.recipeunlocker;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RecipeUnlocker implements ModInitializer {
    public static final Logger LOG = LogManager.getLogger();
    public static final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public void onInitialize() {
        LOG.info("Initialized Recipe Unlocker.");

    }
}
