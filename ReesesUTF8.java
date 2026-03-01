/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.jellysquid.mods.sodium.client.SodiumClientMod
 *  me.jellysquid.mods.sodium.client.data.fingerprint.HashedFingerprint
 *  me.jellysquid.mods.sodium.client.gui.SodiumGameOptions
 *  me.jellysquid.mods.sodium.client.gui.options.Option
 *  me.jellysquid.mods.sodium.client.gui.options.OptionFlag
 *  me.jellysquid.mods.sodium.client.gui.options.OptionPage
 *  me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage
 *  me.jellysquid.mods.sodium.client.gui.prompt.ScreenPrompt
 *  me.jellysquid.mods.sodium.client.gui.prompt.ScreenPrompt$Action
 *  me.jellysquid.mods.sodium.client.gui.prompt.ScreenPromptable
 *  me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget
 *  me.jellysquid.mods.sodium.client.util.Dim2i
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_156
 *  net.minecraft.class_2561
 *  net.minecraft.class_2583
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 *  net.minecraft.class_446
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 *  org.jetbrains.annotations.Nullable
 */
package me.flashyreese.mods.reeses_sodium_options.client.gui;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.AbstractFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.BasicFrame;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.components.SearchTextFieldComponent;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.Tab;
import me.flashyreese.mods.reeses_sodium_options.client.gui.frame.tab.TabFrame;
import me.flashyreese.mods.reeses_sodium_options.compat.IrisCompat;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.data.fingerprint.HashedFingerprint;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import me.jellysquid.mods.sodium.client.gui.prompt.ScreenPrompt;
import me.jellysquid.mods.sodium.client.gui.prompt.ScreenPromptable;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_437;
import net.minecraft.class_446;
import net.minecraft.class_5250;
import net.minecraft.class_5348;
import org.jetbrains.annotations.Nullable;

public class SodiumVideoOptionsScreen
extends class_437
implements ScreenPromptable {
    private static final AtomicReference<class_2561> tabFrameSelectedTab = new AtomicReference<Object>(null);
    private static final AtomicReference<Integer> tabFrameScrollBarOffset = new AtomicReference<Integer>(0);
    private static final AtomicReference<Integer> optionPageScrollBarOffset = new AtomicReference<Integer>(0);
    private static final AtomicReference<String> lastSearch = new AtomicReference<String>("");
    private static final AtomicReference<Integer> lastSearchIndex = new AtomicReference<Integer>(0);
    private final class_437 prevScreen;
    private final List<OptionPage> pages = new ArrayList<OptionPage>();
    private AbstractFrame frame;
    private FlatButtonWidget applyButton;
    private FlatButtonWidget closeButton;
    private FlatButtonWidget undoButton;
    private FlatButtonWidget donateButton;
    private FlatButtonWidget hideDonateButton;
    private boolean hasPendingChanges;
    private SearchTextFieldComponent searchTextField;
    @Nullable
    private ScreenPrompt prompt;
    private static final List<class_5348> DONATION_PROMPT_MESSAGE = List.of(class_5348.method_29433((class_5348[])new class_5348[]{class_2561.method_43470((String)"Hello!")}), class_5348.method_29433((class_5348[])new class_5348[]{class_2561.method_43470((String)"It seems that you've been enjoying "), class_2561.method_43470((String)"Sodium").method_10862(class_2583.field_24360.method_36139(2616210)), class_2561.method_43470((String)", the free and open-source optimization mod for Minecraft.")}), class_5348.method_29433((class_5348[])new class_5348[]{class_2561.method_43470((String)"Mods like these are complex. They require "), class_2561.method_43470((String)"thousands of hours").method_10862(class_2583.field_24360.method_36139(16739840)), class_2561.method_43470((String)" of development, debugging, and tuning to create the experience that players have come to expect.")}), class_5348.method_29433((class_5348[])new class_5348[]{class_2561.method_43470((String)"If you'd like to show your token of appreciation, and support the development of our mod in the process, then consider "), class_2561.method_43470((String)"buying us a coffee").method_10862(class_2583.field_24360.method_36139(15550926)), class_2561.method_43470((String)".")}), class_5348.method_29433((class_5348[])new class_5348[]{class_2561.method_43470((String)"And thanks again for using our mod! We hope it helps you (and your computer.)")}));

    public SodiumVideoOptionsScreen(class_437 prev, List<OptionPage> pages) {
        super((class_2561)class_2561.method_43470((String)"Reese's Sodium Menu"));
        this.prevScreen = prev;
        this.pages.addAll(pages);
        this.checkPromptTimers();
    }

    private void checkPromptTimers() {
        Instant threshold;
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return;
        }
        SodiumGameOptions options = SodiumClientMod.options();
        if (options.notifications.hasSeenDonationPrompt) {
            return;
        }
        HashedFingerprint fingerprint = null;
        try {
            fingerprint = HashedFingerprint.loadFromDisk();
        }
        catch (Throwable t) {
            SodiumClientMod.logger().error("Failed to read the fingerprint from disk", t);
        }
        if (fingerprint == null) {
            return;
        }
        Instant now = Instant.now();
        if (now.isAfter(threshold = Instant.ofEpochSecond(fingerprint.timestamp()).plus(3L, ChronoUnit.DAYS))) {
            this.openDonationPrompt(options);
        }
    }

    private void openDonationPrompt(SodiumGameOptions options) {
        ScreenPrompt prompt = new ScreenPrompt((ScreenPromptable)this, DONATION_PROMPT_MESSAGE, 320, 190, new ScreenPrompt.Action((class_2561)class_2561.method_43470((String)"Buy us a coffee"), this::openDonationPage));
        prompt.method_25365(true);
        options.notifications.hasSeenDonationPrompt = true;
        try {
            SodiumGameOptions.writeToDisk((SodiumGameOptions)options);
        }
        catch (IOException e) {
            SodiumClientMod.logger().error("Failed to update config file", (Throwable)e);
        }
    }

    public void rebuildUI() {
        this.method_41843();
    }

    protected void method_25426() {
        this.frame = this.parentFrameBuilder().build();
        this.method_37063((class_364)this.frame);
        this.searchTextField.method_25365(!lastSearch.get().trim().isEmpty());
        if (this.searchTextField.method_25370()) {
            this.method_25395((class_364)this.searchTextField);
        } else {
            this.method_25395((class_364)this.frame);
        }
    }

    protected BasicFrame.Builder parentFrameBuilder() {
        int newWidth = this.field_22789;
        if ((double)((float)this.field_22789 / (float)this.field_22790) > 1.77777777778) {
            newWidth = (int)((double)this.field_22790 * 1.77777777778);
        }
        Dim2i basicFrameDim = new Dim2i((this.field_22789 - newWidth) / 2, 0, newWidth, this.field_22790);
        Dim2i tabFrameDim = new Dim2i(basicFrameDim.x() + basicFrameDim.width() / 20 / 2, basicFrameDim.y() + basicFrameDim.height() / 4 / 2, basicFrameDim.width() - basicFrameDim.width() / 20, basicFrameDim.height() / 4 * 3);
        Dim2i undoButtonDim = new Dim2i(tabFrameDim.getLimitX() - 203, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i applyButtonDim = new Dim2i(tabFrameDim.getLimitX() - 134, tabFrameDim.getLimitY() + 5, 65, 20);
        Dim2i closeButtonDim = new Dim2i(tabFrameDim.getLimitX() - 65, tabFrameDim.getLimitY() + 5, 65, 20);
        class_5250 donationText = class_2561.method_43471((String)"sodium.options.buttons.donate");
        int donationTextWidth = this.field_22787.field_1772.method_27525((class_5348)donationText);
        Dim2i donateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 32 - donationTextWidth, tabFrameDim.y() - 26, 10 + donationTextWidth, 20);
        Dim2i hideDonateButtonDim = new Dim2i(tabFrameDim.getLimitX() - 20, tabFrameDim.y() - 26, 20, 20);
        this.undoButton = new FlatButtonWidget(undoButtonDim, (class_2561)class_2561.method_43471((String)"sodium.options.buttons.undo"), this::undoChanges);
        this.applyButton = new FlatButtonWidget(applyButtonDim, (class_2561)class_2561.method_43471((String)"sodium.options.buttons.apply"), this::applyChanges);
        this.closeButton = new FlatButtonWidget(closeButtonDim, (class_2561)class_2561.method_43471((String)"gui.done"), this::method_25419);
        this.donateButton = new FlatButtonWidget(donateButtonDim, (class_2561)donationText, this::openDonationPage);
        this.hideDonateButton = new FlatButtonWidget(hideDonateButtonDim, (class_2561)class_2561.method_43470((String)"x"), this::hideDonationButton);
        if (SodiumClientMod.options().notifications.hasClearedDonationButton) {
            this.setDonationButtonVisibility(false);
        }
        Dim2i searchTextFieldDim = SodiumClientMod.options().notifications.hasClearedDonationButton ? new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width(), 20) : new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width() - (tabFrameDim.getLimitX() - donateButtonDim.x()) - 2, 20);
        BasicFrame.Builder basicFrameBuilder = this.parentBasicFrameBuilder(basicFrameDim, tabFrameDim);
        if (IrisCompat.isIrisPresent()) {
            int size = this.field_22787.field_1772.method_27525((class_5348)class_2561.method_43471((String)IrisCompat.getIrisShaderPacksScreenLanguageKey()));
            Dim2i shaderPackButtonDim = !SodiumClientMod.options().notifications.hasClearedDonationButton ? new Dim2i(donateButtonDim.x() - 12 - size, tabFrameDim.y() - 26, 10 + size, 20) : new Dim2i(tabFrameDim.getLimitX() - size - 10, tabFrameDim.y() - 26, 10 + size, 20);
            searchTextFieldDim = new Dim2i(tabFrameDim.x(), tabFrameDim.y() - 26, tabFrameDim.width() - (tabFrameDim.getLimitX() - shaderPackButtonDim.x()) - 2, 20);
            FlatButtonWidget shaderPackButton = new FlatButtonWidget(shaderPackButtonDim, (class_2561)class_2561.method_43471((String)IrisCompat.getIrisShaderPacksScreenLanguageKey()), () -> this.field_22787.method_1507(IrisCompat.getIrisShaderPacksScreen(this)));
            basicFrameBuilder.addChild(dim -> shaderPackButton);
        }
        this.searchTextField = new SearchTextFieldComponent(searchTextFieldDim, this.pages, tabFrameSelectedTab, tabFrameScrollBarOffset, optionPageScrollBarOffset, tabFrameDim.height(), this, lastSearch, lastSearchIndex);
        basicFrameBuilder.addChild(dim -> this.searchTextField);
        return basicFrameBuilder;
    }

    public BasicFrame.Builder parentBasicFrameBuilder(Dim2i parentBasicFrameDim, Dim2i tabFrameDim) {
        return BasicFrame.createBuilder().setDimension(parentBasicFrameDim).shouldRenderOutline(false).addChild(dim -> this.donateButton).addChild(dim -> this.hideDonateButton).addChild(parentDim -> TabFrame.createBuilder().setDimension(tabFrameDim).shouldRenderOutline(false).setTabSectionScrollBarOffset(tabFrameScrollBarOffset).setTabSectionSelectedTab(tabFrameSelectedTab).addTabs(tabs -> this.pages.stream().filter(page -> !page.getGroups().isEmpty()).forEach(page -> tabs.add(Tab.createBuilder().from((OptionPage)page, optionPageScrollBarOffset)))).onSetTab(() -> optionPageScrollBarOffset.set(0)).build()).addChild(dim -> this.undoButton).addChild(dim -> this.applyButton).addChild(dim -> this.closeButton);
    }

    public void method_25394(class_332 drawContext, int mouseX, int mouseY, float delta) {
        super.method_25420(drawContext);
        this.updateControls();
        this.frame.method_25394(drawContext, this.prompt != null ? -1 : mouseX, this.prompt != null ? -1 : mouseY, delta);
        if (this.prompt != null) {
            this.prompt.method_25394(drawContext, mouseX, mouseY, delta);
        }
    }

    private void updateControls() {
        boolean hasChanges = this.getAllOptions().anyMatch(Option::hasChanged);
        for (OptionPage page : this.pages) {
            for (Option option : page.getOptions()) {
                if (!option.hasChanged()) continue;
                hasChanges = true;
            }
        }
        this.applyButton.setEnabled(hasChanges);
        this.undoButton.setVisible(hasChanges);
        this.closeButton.setEnabled(!hasChanges);
        this.hasPendingChanges = hasChanges;
    }

    private void setDonationButtonVisibility(boolean value) {
        this.donateButton.setVisible(value);
        this.hideDonateButton.setVisible(value);
    }

    private void hideDonationButton() {
        SodiumGameOptions options = SodiumClientMod.options();
        options.notifications.hasClearedDonationButton = true;
        try {
            SodiumGameOptions.writeToDisk((SodiumGameOptions)options);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to save configuration", e);
        }
        this.setDonationButtonVisibility(false);
        this.rebuildUI();
    }

    private void openDonationPage() {
        class_156.method_668().method_670("https://caffeinemc.net/donate");
    }

    private Stream<Option<?>> getAllOptions() {
        return this.pages.stream().flatMap(s -> s.getOptions().stream());
    }

    private void applyChanges() {
        HashSet dirtyStorages = new HashSet();
        EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);
        this.getAllOptions().forEach(option -> {
            if (!option.hasChanged()) {
                return;
            }
            option.applyChanges();
            flags.addAll(option.getFlags());
            dirtyStorages.add(option.getStorage());
        });
        class_310 client = class_310.method_1551();
        if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD)) {
            client.field_1769.method_3279();
        }
        if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD)) {
            client.method_24041(((Integer)client.field_1690.method_42563().method_41753()).intValue());
            client.method_1513();
        }
        for (OptionStorage storage : dirtyStorages) {
            storage.save();
        }
    }

    private void undoChanges() {
        this.getAllOptions().forEach(Option::reset);
    }

    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (this.prompt != null) {
            return this.prompt.method_25402(mouseX, mouseY, button);
        }
        return super.method_25402(mouseX, mouseY, button);
    }

    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (this.prompt != null) {
            return this.prompt.method_25404(keyCode, scanCode, modifiers);
        }
        if (!(keyCode != 80 || (modifiers & 1) == 0 || this.searchTextField != null && this.searchTextField.method_25370())) {
            class_310.method_1551().method_1507((class_437)new class_446(this.prevScreen, class_310.method_1551().field_1690));
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }

    public boolean method_25422() {
        return !this.hasPendingChanges;
    }

    public void method_25419() {
        lastSearch.set("");
        lastSearchIndex.set(0);
        this.field_22787.method_1507(this.prevScreen);
    }

    public void setPrompt(@Nullable ScreenPrompt prompt) {
        this.prompt = prompt;
    }

    @Nullable
    public ScreenPrompt getPrompt() {
        return this.prompt;
    }

    public Dim2i getDimensions() {
        return new Dim2i(0, 0, this.field_22789, this.field_22790);
    }
}
