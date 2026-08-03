/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.network.chat.ClickEvent
 *  net.minecraft.network.chat.ClickEvent$ShowDialog
 *  net.minecraft.server.dialog.ActionButton
 *  net.minecraft.server.dialog.CommonButtonData
 *  net.minecraft.server.dialog.Dialog
 *  net.minecraft.server.dialog.DialogListDialog
 *  net.minecraft.server.dialog.action.StaticAction
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.dialog;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.ButtonListDialogScreen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import net.minecraft.server.dialog.action.StaticAction;
import org.jspecify.annotations.Nullable;

public class DialogListDialogScreen
extends ButtonListDialogScreen<DialogListDialog> {
    public DialogListDialogScreen(@Nullable Screen previousScreen, DialogListDialog dialog, DialogConnectionAccess connectionAccess) {
        super(previousScreen, dialog, connectionAccess);
    }

    @Override
    protected Stream<ActionButton> createListActions(DialogListDialog data, DialogConnectionAccess connectionAccess) {
        return data.dialogs().stream().map(subDialog -> DialogListDialogScreen.createDialogClickAction(data, (Holder<Dialog>)subDialog));
    }

    private static ActionButton createDialogClickAction(DialogListDialog data, Holder<Dialog> subDialog) {
        return new ActionButton(new CommonButtonData(((Dialog)subDialog.value()).common().computeExternalTitle(), data.buttonWidth()), Optional.of(new StaticAction((ClickEvent)new ClickEvent.ShowDialog(subDialog))));
    }
}

