package toni.sodiumoptionsapi.api;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;

import net.minecraft.resources.ResourceLocation;

public interface ExtendedOptionGroup {

    OptionGroup.Builder sodiumOptionsAPI$setId(ResourceLocation id);

    OptionGroup.Builder sodiumOptionsAPI$setId(OptionIdentifier<Void> id);

    static OptionGroup.Builder createBuilder(OptionIdentifier<Void> id) {
        OptionGroup.Builder builder = OptionGroup.createBuilder();
        ((ExtendedOptionGroup) builder).sodiumOptionsAPI$setId(id);
        return builder;
    }

    static OptionGroup.Builder createBuilder(ResourceLocation id) {
        OptionGroup.Builder builder = OptionGroup.createBuilder();
        ((ExtendedOptionGroup) builder).sodiumOptionsAPI$setId(id);
        return builder;
    }
}
