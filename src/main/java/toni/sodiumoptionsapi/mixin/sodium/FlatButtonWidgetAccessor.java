package toni.sodiumoptionsapi.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;

@Mixin(FlatButtonWidget.class)
public interface FlatButtonWidgetAccessor {
    @Accessor("dim")
    Dim2i getDim();
}
