package toni.sodiumoptionsapi.util;

import net.minecraft.resources.ResourceLocation;
import toni.sodiumoptionsapi.api.OptionIdentifier;

public interface IOptionGroupIdAccessor {
    OptionIdentifier<Void> sodiumOptionsAPI$getId();
    void sodiumOptionsAPI$setId(OptionIdentifier<Void> id);
    void sodiumOptionsAPI$setId(ResourceLocation id);
}
