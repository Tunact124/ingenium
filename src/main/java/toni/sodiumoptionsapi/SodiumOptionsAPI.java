package toni.sodiumoptionsapi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class SodiumOptionsAPI implements ModInitializer, ClientModInitializer {
	public static final String MODNAME = "Sodium Options API";
	public static final String ID = "sodiumoptionsapi";
	public static final Logger LOGGER = LogManager.getLogger(MODNAME);

	public SodiumOptionsAPI() {
	}

	@Override
	public void onInitialize() {
	}

	@Override
	public void onInitializeClient() {
	}
}
