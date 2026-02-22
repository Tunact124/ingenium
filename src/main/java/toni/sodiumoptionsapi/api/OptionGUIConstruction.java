package toni.sodiumoptionsapi.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.List;

import me.jellysquid.mods.sodium.client.gui.options.OptionPage;

public interface OptionGUIConstruction {
    Event<OptionGUIConstruction> EVENT = EventFactory.createArrayBacked(OptionGUIConstruction.class, (listeners) -> (pages) -> {
        for (OptionGUIConstruction event : listeners) {
            event.onGroupConstruction(pages);
        }
    });

    void onGroupConstruction(List<OptionPage> pages);
}
