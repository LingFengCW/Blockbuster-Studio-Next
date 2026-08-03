/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.Identifier
 *  net.minecraft.util.ARGB
 *  net.minecraft.world.level.GameType
 */
package net.minecraft.client.gui.spectator.categories;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.spectator.PlayerMenuItem;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuCategory;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.GameType;

public class TeleportToPlayerMenuCategory
implements SpectatorMenuCategory,
SpectatorMenuItem {
    private static final Identifier TELEPORT_TO_PLAYER_SPRITE = Identifier.withDefaultNamespace((String)"spectator/teleport_to_player");
    private static final Comparator<PlayerInfo> PROFILE_ORDER = Comparator.comparing(p -> p.getProfile().id());
    private static final Component TELEPORT_TEXT = Component.translatable((String)"spectatorMenu.teleport");
    private static final Component TELEPORT_PROMPT = Component.translatable((String)"spectatorMenu.teleport.prompt");
    private final List<SpectatorMenuItem> items;

    public TeleportToPlayerMenuCategory() {
        this(Minecraft.getInstance().getConnection().getListedOnlinePlayers());
    }

    public TeleportToPlayerMenuCategory(Collection<PlayerInfo> profiles) {
        this.items = profiles.stream().filter(p -> p.getGameMode() != GameType.SPECTATOR).sorted(PROFILE_ORDER).map(PlayerMenuItem::new).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<SpectatorMenuItem> getItems() {
        return this.items;
    }

    @Override
    public Component getPrompt() {
        return TELEPORT_PROMPT;
    }

    @Override
    public void selectItem(SpectatorMenu menu) {
        menu.selectCategory(this);
    }

    @Override
    public Component getName() {
        return TELEPORT_TEXT;
    }

    @Override
    public void extractIcon(GuiGraphicsExtractor graphics, float brightness, float alpha) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TELEPORT_TO_PLAYER_SPRITE, 0, 0, 16, 16, ARGB.colorFromFloat((float)alpha, (float)brightness, (float)brightness, (float)brightness));
    }

    @Override
    public boolean isEnabled() {
        return !this.items.isEmpty();
    }
}

