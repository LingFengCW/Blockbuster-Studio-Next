package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageBarOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * "Import" dialog: lists every exported document (.scenebbs / .seqbbs) in
 * the export folder. Picking a file hands it to the callback (scene import
 * or sequence import depending on the caller) and closes the dialog.
 */
public class UIImportOverlayPanel extends UIMessageBarOverlayPanel
{
    public UIImportOverlayPanel(Consumer<File> callback)
    {
        super(UIKeys.ASSETS_IMPORT, UIKeys.ASSETS_EXPORT_FOLDER);

        var list = UI.scrollView(5, 5);

        list.h(220);

        File folder = BBSMod.getExportFolder();
        File[] files = folder.isDirectory() ? folder.listFiles((dir, name) ->
            name.endsWith(".scenebbs") || name.endsWith(".seqbbs")) : null;

        if (files != null && files.length > 0)
        {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

            for (File file : files)
            {
                UIButton button = new UIButton(IKey.raw(file.getName()), (b) ->
                {
                    callback.accept(file);
                    this.removeFromParent();
                });

                button.h(20);
                list.add(button);
            }
        }
        else
        {
            var hint = UI.label(UIKeys.ASSETS_EXPORT_EMPTY, 18, Colors.GRAY);

            hint.h(18);
            list.add(hint);
        }

        this.content.add(list);
    }
}
