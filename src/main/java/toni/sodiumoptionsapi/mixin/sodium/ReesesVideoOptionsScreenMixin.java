package toni.sodiumoptionsapi.mixin.sodium;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import toni.sodiumoptionsapi.gui.SodiumOptionsTabFrame;
import toni.sodiumoptionsapi.util.IOptionGroupIdAccessor;
import toni.sodiumoptionsapi.util.PlatformUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.BasicFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.compat.IrisCompat;

import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;

@Mixin(SodiumVideoOptionsScreen.class)
public class ReesesVideoOptionsScreenMixin  {

    @Shadow @Final private static AtomicReference<Integer> tabFrameScrollBarOffset;
    @Shadow @Final private static AtomicReference<Integer> optionPageScrollBarOffset;

    @Shadow @Final private List<OptionPage> pages;
    @Shadow @Final private static AtomicReference<Component> tabFrameSelectedTab;

    @Redirect(method = "parentBasicFrameBuilder", at = @At(value = "INVOKE", ordinal = 0, target = "Lme/flashyreese/mods/reeses_sodium_options/client/gui/frame/BasicFrame$Builder;addChild(Ljava/util/function/Function;)Lme/flashyreese/mods/reeses_sodium_options/client/gui/frame/BasicFrame$Builder;"), remap = false)
    private BasicFrame.Builder tabFrameBuilder(BasicFrame.Builder instance, Function<Dim2i, AbstractWidget> function, @Local(ordinal = 1, argsOnly = true) Dim2i tabFrameDim) {
        instance.addChild((parentDim) -> SodiumOptionsTabFrame.createBuilder()
                .setDimension(tabFrameDim)
                .shouldRenderOutline(false)
                .setTabSectionScrollBarOffset(tabFrameScrollBarOffset)
                .setTabSectionSelectedTab(tabFrameSelectedTab)
                .addTabs(tabs -> this.pages
                        .stream()
                        .filter(this::sodiumOptionsAPI$isSodiumTab)
                        .forEach(page -> tabs.put(((IOptionGroupIdAccessor)page).sodiumOptionsAPI$getId().getModId(), Tab.createBuilder().from(page, optionPageScrollBarOffset)))                )
                .addTabs(this::sodiumOptionsAPI$createShaderPackButton)
                .addTabs(tabs -> this.pages
                        .stream()
                        .filter((tab) -> !sodiumOptionsAPI$isSodiumTab(tab))
                        .forEach(page -> tabs.put(((IOptionGroupIdAccessor)page).sodiumOptionsAPI$getId().getModId(), Tab.createBuilder().from(page, optionPageScrollBarOffset)))                )
                .onSetTab(() -> {
                    optionPageScrollBarOffset.set(0);
                })
                .build());

        return instance;
    }

    @Unique
    private boolean sodiumOptionsAPI$isSodiumTab(OptionPage optionPage) {
        if (optionPage.getName().getString().equals("Shader Packs...") || optionPage.getName().getString().equals("Oculus"))
            return false;

        return Objects.equals(((IOptionGroupIdAccessor) optionPage).sodiumOptionsAPI$getId().getModId(), "sodium") || Objects.equals(((IOptionGroupIdAccessor) optionPage).sodiumOptionsAPI$getId().getModId(), "embeddium");
    }

    @Unique
    private void sodiumOptionsAPI$createShaderPackButton(Multimap<String, Tab<?>> tabs) {
        var iris = IrisCompat.isIrisPresent();

        if (iris) {
            String shaderModId = (String) Stream.of("oculus", "iris").filter(PlatformUtil::modPresent).findFirst().orElse("iris");

            var builder = Tab.createBuilder()
                    .setTitle(Component.translatable("options.iris.shaderPackSelection"))
                    .setFrameFunction(this::sodiumOptionsAPI$getFrame);

            tabs.put(shaderModId, builder.build());
        }

    }

    @Unique
    private <T extends me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame> T sodiumOptionsAPI$getFrame(Dim2i dim) {
        return (T) new BasicFrame(dim, false, new ArrayList());
    }
}
