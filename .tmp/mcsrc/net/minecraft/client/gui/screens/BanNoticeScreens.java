/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.minecraft.BanDetails
 *  it.unimi.dsi.fastutil.booleans.BooleanConsumer
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.ComponentUtils
 *  net.minecraft.network.chat.Style
 *  net.minecraft.util.CommonLinks
 *  net.minecraft.util.Util
 *  org.apache.commons.lang3.StringUtils
 */
package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.multiplayer.chat.report.BanReason;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;

public class BanNoticeScreens {
    private static final Component TEMPORARY_BAN_TITLE = Component.translatable((String)"gui.banned.title.temporary").withStyle(ChatFormatting.BOLD);
    private static final Component PERMANENT_BAN_TITLE = Component.translatable((String)"gui.banned.title.permanent").withStyle(ChatFormatting.BOLD);
    public static final Component NAME_BAN_TITLE = Component.translatable((String)"gui.banned.name.title").withStyle(ChatFormatting.BOLD);
    private static final Component SKIN_BAN_TITLE = Component.translatable((String)"gui.banned.skin.title").withStyle(ChatFormatting.BOLD);
    private static final Component SKIN_BAN_DESCRIPTION = Component.translatable((String)"gui.banned.skin.description", (Object[])new Object[]{Component.translationArg((URI)CommonLinks.SUSPENSION_HELP)});

    public static ConfirmLinkScreen create(BooleanConsumer callback, BanDetails multiplayerBanned) {
        return new ConfirmLinkScreen(callback, BanNoticeScreens.getBannedTitle(multiplayerBanned), BanNoticeScreens.getBannedScreenText(multiplayerBanned), CommonLinks.SUSPENSION_HELP, CommonComponents.GUI_ACKNOWLEDGE, true);
    }

    public static ConfirmLinkScreen createSkinBan(Runnable onClose) {
        URI uri = CommonLinks.SUSPENSION_HELP;
        return new ConfirmLinkScreen(result -> {
            if (result) {
                Util.getPlatform().openUri(uri);
            }
            onClose.run();
        }, SKIN_BAN_TITLE, SKIN_BAN_DESCRIPTION, uri, CommonComponents.GUI_ACKNOWLEDGE, true);
    }

    public static ConfirmLinkScreen createNameBan(String name, Runnable onClose) {
        URI uri = CommonLinks.SUSPENSION_HELP;
        return new ConfirmLinkScreen(result -> {
            if (result) {
                Util.getPlatform().openUri(uri);
            }
            onClose.run();
        }, NAME_BAN_TITLE, (Component)Component.translatable((String)"gui.banned.name.description", (Object[])new Object[]{Component.literal((String)name).withStyle(ChatFormatting.YELLOW), Component.translationArg((URI)CommonLinks.SUSPENSION_HELP)}), uri, CommonComponents.GUI_ACKNOWLEDGE, true);
    }

    private static Component getBannedTitle(BanDetails multiplayerBanned) {
        return BanNoticeScreens.isTemporaryBan(multiplayerBanned) ? TEMPORARY_BAN_TITLE : PERMANENT_BAN_TITLE;
    }

    private static Component getBannedScreenText(BanDetails multiplayerBanned) {
        return Component.translatable((String)"gui.banned.description", (Object[])new Object[]{BanNoticeScreens.getBanReasonText(multiplayerBanned), BanNoticeScreens.getBanStatusText(multiplayerBanned), Component.translationArg((URI)CommonLinks.SUSPENSION_HELP)});
    }

    private static Component getBanReasonText(BanDetails multiplayerBanned) {
        String reasonString = multiplayerBanned.reason();
        String reasonMessage = multiplayerBanned.reasonMessage();
        if (StringUtils.isNumeric((CharSequence)reasonString)) {
            int reasonId = Integer.parseInt(reasonString);
            BanReason reason = BanReason.byId(reasonId);
            Object reasonText = reason != null ? ComponentUtils.mergeStyles((Component)reason.title(), (Style)Style.EMPTY.withBold(Boolean.valueOf(true))) : (reasonMessage != null ? Component.translatable((String)"gui.banned.description.reason_id_message", (Object[])new Object[]{reasonId, reasonMessage}).withStyle(ChatFormatting.BOLD) : Component.translatable((String)"gui.banned.description.reason_id", (Object[])new Object[]{reasonId}).withStyle(ChatFormatting.BOLD));
            return Component.translatable((String)"gui.banned.description.reason", (Object[])new Object[]{reasonText});
        }
        return Component.translatable((String)"gui.banned.description.unknownreason");
    }

    private static Component getBanStatusText(BanDetails multiplayerBanned) {
        if (BanNoticeScreens.isTemporaryBan(multiplayerBanned)) {
            Component banDurationText = BanNoticeScreens.getBanDurationText(multiplayerBanned);
            return Component.translatable((String)"gui.banned.description.temporary", (Object[])new Object[]{Component.translatable((String)"gui.banned.description.temporary.duration", (Object[])new Object[]{banDurationText}).withStyle(ChatFormatting.BOLD)});
        }
        return Component.translatable((String)"gui.banned.description.permanent").withStyle(ChatFormatting.BOLD);
    }

    private static Component getBanDurationText(BanDetails multiplayerBanned) {
        Duration banDuration = Duration.between(Instant.now(), multiplayerBanned.expires());
        long durationHours = banDuration.toHours();
        if (durationHours > 72L) {
            return CommonComponents.days((long)banDuration.toDays());
        }
        if (durationHours < 1L) {
            return CommonComponents.minutes((long)banDuration.toMinutes());
        }
        return CommonComponents.hours((long)banDuration.toHours());
    }

    private static boolean isTemporaryBan(BanDetails multiplayerBanned) {
        return multiplayerBanned.expires() != null;
    }
}

