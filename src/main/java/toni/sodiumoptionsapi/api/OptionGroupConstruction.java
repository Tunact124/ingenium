package toni.sodiumoptionsapi.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.List;

import me.jellysquid.mods.sodium.client.gui.options.Option;

public interface OptionGroupConstruction {
    Event<OptionGroupConstruction> EVENT = EventFactory.createArrayBacked(OptionGroupConstruction.class, (listeners) -> (id, options) -> {
        for (OptionGroupConstruction event : listeners) {
            event.onGroupConstruction(id, options);
        }
    });

    void onGroupConstruction(OptionIdentifier<Void> id, List<Option<?>> options);
}
