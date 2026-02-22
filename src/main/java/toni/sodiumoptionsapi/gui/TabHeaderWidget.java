package toni.sodiumoptionsapi.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import toni.sodiumoptionsapi.SodiumOptionsAPI;
import toni.sodiumoptionsapi.mixin.sodium.FlatButtonWidgetAccessor;
import toni.sodiumoptionsapi.util.ILeftAlignOffsetAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;

import net.fabricmc.loader.api.FabricLoader;

public class TabHeaderWidget extends FlatButtonWidget {
    private static final ResourceLocation FALLBACK_LOCATION = new ResourceLocation("textures/misc/unknown_pack.png");

    private static final Set<String> erroredLogos = new HashSet<>();
    private final ResourceLocation logoTexture;
    private final boolean isTitle;

    public static MutableComponent getLabel(String modId, boolean underline ) {
        return (switch(modId) {
            // TODO handle long mod names better, this is the only one we know of right now
            case "sspb" -> Component.literal("SSPB");
            default -> idComponent(modId);
        }).withStyle(s -> s.withUnderlined(underline));
    }

    static MutableComponent idComponent(String namespace) {
        return Component.literal(getModName(namespace));
    }

    public static String getModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(container -> container.getMetadata().getName()).orElse(modId);
    }

    public TabHeaderWidget(Dim2i dim, String modId, Runnable action) {
        super(dim, getLabel(modId, action == null), action == null ? () -> { } : action);

        isTitle = action == null;

        Optional<Path> logoFile = erroredLogos.contains(modId) ? Optional.empty() : FabricLoader.getInstance().getModContainer(modId).flatMap(c -> c.getMetadata().getIconPath(32).flatMap(c::findPath));

        ResourceLocation texture = null;
        if(logoFile.isPresent()) {
            try(InputStream is = Files.newInputStream(logoFile.get())) {
                if (is != null) {
                    NativeImage logo = NativeImage.read(is);
                    if(logo.getWidth() != logo.getHeight()) {
                        logo.close();
                        throw new IOException("Logo " + logoFile.get() + " for " + modId + " is not square");
                    }
                    texture = new ResourceLocation("sodium", "logo/" + modId);
                    Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(logo));
                }
            } catch(IOException e) {
                erroredLogos.add(modId);
                SodiumOptionsAPI.LOGGER.error("Exception reading logo for " + modId, e);
            }
        }

        this.setStyle(getStyle());
        this.logoTexture = texture;
    }

    public FlatButtonWidget.Style getStyle() {
        FlatButtonWidget.Style style = new FlatButtonWidget.Style();
        style.bgHovered = isTitle ? ColorARGB.pack(0, 0, 0, 140) : -536870912;
        style.bgDefault = ColorARGB.pack(0, 0, 0, 140);
        style.bgDisabled = 1610612736;
        style.textDefault = -1;
        style.textDisabled = -1862270977;
        return style;
    }

    protected boolean isHovered(int mouseX, int mouseY) {
        return false;
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        ((ILeftAlignOffsetAccessor) this).sodiumOptionsAPI$setLeftAlignOffset(20);
        super.render(drawContext, mouseX, mouseY, delta);

        var dim = ((FlatButtonWidgetAccessor)this).getDim();

        ResourceLocation icon = Objects.requireNonNullElse(this.logoTexture, FALLBACK_LOCATION);
        int fontHeight = Minecraft.getInstance().font.lineHeight;
        int imgY = ((FlatButtonWidgetAccessor)this).getDim().getCenterY() - (fontHeight / 2) ;
        drawContext.blit(icon, ((FlatButtonWidgetAccessor)this).getDim().x() + 5, imgY, 0.0f, 0.0f, fontHeight, fontHeight, fontHeight, fontHeight);
    }
}
