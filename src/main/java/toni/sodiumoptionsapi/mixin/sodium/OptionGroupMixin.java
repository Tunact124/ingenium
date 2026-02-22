package toni.sodiumoptionsapi.mixin.sodium;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import toni.sodiumoptionsapi.api.OptionIdentifier;
import toni.sodiumoptionsapi.util.IOptionGroupIdAccessor;

import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;

@Mixin(OptionGroup.class)
public class OptionGroupMixin implements IOptionGroupIdAccessor {

    @Unique
    private static final OptionIdentifier<Void> sodiumOptionsAPI$DEFAULT_ID = OptionIdentifier.create("sodium", "empty");

    @Unique
    public OptionIdentifier<Void> sodiumOptionsAPI$id;

    public OptionIdentifier<Void> sodiumOptionsAPI$getId() {
        return this.sodiumOptionsAPI$id;
    }

    @Override
    public void sodiumOptionsAPI$setId(OptionIdentifier<Void> id) {
        sodiumOptionsAPI$id = id;
    }

    @Override
    public void sodiumOptionsAPI$setId(ResourceLocation id) {
        sodiumOptionsAPI$id = OptionIdentifier.create(id);
    }
}
