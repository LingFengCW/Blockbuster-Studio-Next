/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.Identifier
 */
package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FriendsButton
extends SpriteIconButton.CenteredIcon {
    private static final Component TOOLTIP = Component.translatable((String)"gui.friends.open.tooltip");
    private static final Component MESSAGE = Component.translatable((String)"gui.friends.open");
    private static final Button.CreateNarration NARRATION = supplier -> Component.translatable((String)"gui.friends.open.narration");
    private static final Identifier[] NOTIFICATION_ICONS = new Identifier[]{Identifier.withDefaultNamespace((String)"notification/1"), Identifier.withDefaultNamespace((String)"notification/2"), Identifier.withDefaultNamespace((String)"notification/3"), Identifier.withDefaultNamespace((String)"notification/4"), Identifier.withDefaultNamespace((String)"notification/5"), Identifier.withDefaultNamespace((String)"notification/more")};
    private static final int SPRITE_SIZE = 15;
    private int incomingRequestCount;

    public FriendsButton(int width, Button.OnPress onPress, boolean friendsAvailable) {
        super(width, 20, MESSAGE, 15, 15, 0, 0, new WidgetSprites(Identifier.withDefaultNamespace((String)"friends/friends")), onPress, (Component)(friendsAvailable ? TOOLTIP : null), NARRATION, false);
        this.active = friendsAvailable;
        this.refreshIncomingRequestCount();
    }

    public void refreshIncomingRequestCount() {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerSocialManager playerSocialManager = minecraft.getPlayerSocialManager();
        if (!playerSocialManager.isFriendListEnabled()) {
            this.incomingRequestCount = 0;
            return;
        }
        this.incomingRequestCount = playerSocialManager.getIncomingRequests().size();
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        if (this.isActive() && this.incomingRequestCount > 0) {
            int iconIndex = Math.min(this.incomingRequestCount, 6) - 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NOTIFICATION_ICONS[iconIndex], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8, this.alpha);
        }
    }
}

