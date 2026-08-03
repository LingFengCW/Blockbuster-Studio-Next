package mchorse.bbs_mod.utils.undo;

/**
 * Tracks whether the active project has unsaved edits.
 *
 * Any edit routed through {@link UndoManager#pushUndo} marks the project
 * dirty; saving the project clears it. The dashboard's title bar reads this
 * flag to show the classic unsaved ("*") marker next to the project name.
 */
public class EditState
{
    private static boolean dirty = false;

    public static boolean isDirty()
    {
        return dirty;
    }

    public static void markDirty()
    {
        dirty = true;
    }

    public static void markSaved()
    {
        dirty = false;
    }
}
