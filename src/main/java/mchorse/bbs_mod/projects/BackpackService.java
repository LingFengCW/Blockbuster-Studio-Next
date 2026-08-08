package mchorse.bbs_mod.projects;

import mchorse.bbs_mod.data.types.MapType;

import java.util.ArrayList;
import java.util.List;

/**
 * The single place that moves assets between a work and the global
 * {@link Backpack}.
 *
 * Anything that can sit in the asset bin - a scene (with its whole film
 * payload), a reusable sequence, or a form category - can be put into the
 * backpack and taken back out into any other work. The UI layers (the HTML
 * editor bridge and the native asset bin) both go through here so the two
 * never drift apart.
 */
public class BackpackService
{
    /* ------------------------------------------------------------------ */
    /* Work -> backpack                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Put one asset of the current work into the backpack.
     *
     * @param type {@link Backpack#TYPE_SCENE}, {@link Backpack#TYPE_SEQUENCE}
     *             or {@link Backpack#TYPE_FORM}
     * @param id   scene / sequence id; ignored for form categories
     * @return human readable errors; empty means success
     */
    public static List<String> put(String type, String id)
    {
        List<String> errors = new ArrayList<>();
        BBSProject project = ProjectManager.get() == null ? null : ProjectManager.get().getCurrent();

        if (project == null)
        {
            errors.add("No work is open.");

            return errors;
        }

        if (Backpack.TYPE_SCENE.equals(type))
        {
            SceneManager scenes = SceneManager.get();
            Scene scene = scenes == null ? null : scenes.getById(id);

            if (scene == null)
            {
                errors.add("Scene not found: " + id);

                return errors;
            }

            MapType document = scenes.exportSceneData(scene);

            return Backpack.exportDocument(project, scene.name, Backpack.TYPE_SCENE, document);
        }

        if (Backpack.TYPE_SEQUENCE.equals(type))
        {
            SequenceManager sequences = SequenceManager.get();
            Sequence sequence = sequences == null ? null : sequences.getById(id);

            if (sequence == null)
            {
                errors.add("Sequence not found: " + id);

                return errors;
            }

            MapType document = sequences.exportSequenceData(sequence);

            /* A sequence only references ids, so pull the scenes it points
             * at along with it - otherwise the sequence lands in another
             * work as a list of dead links. */
            bundleReferencedScenes(project, sequence, errors);

            errors.addAll(Backpack.exportDocument(project, sequence.name, Backpack.TYPE_SEQUENCE, document));

            return errors;
        }

        if (Backpack.TYPE_FORM.equals(type))
        {
            return Backpack.exportCategories(project);
        }

        errors.add("Unknown asset type: " + type);

        return errors;
    }

    /**
     * Export every scene a sequence references (recursively through nested
     * sequences) so the backpack item is self-contained.
     */
    private static void bundleReferencedScenes(BBSProject project, Sequence sequence, List<String> errors)
    {
        SceneManager scenes = SceneManager.get();
        SequenceManager sequences = SequenceManager.get();

        if (scenes == null || sequences == null)
        {
            return;
        }

        List<String> pending = new ArrayList<>();
        List<String> visited = new ArrayList<>();

        pending.add(sequence.id);

        while (!pending.isEmpty())
        {
            String currentId = pending.remove(0);

            if (visited.contains(currentId))
            {
                continue;
            }

            visited.add(currentId);

            Sequence current = sequences.getById(currentId);

            if (current == null)
            {
                continue;
            }

            for (Sequence.SequenceRef ref : current.refs)
            {
                if (ref.isSequence())
                {
                    pending.add(ref.id);
                }
                else if (Sequence.SequenceRef.SCENE.equals(ref.type))
                {
                    Scene scene = scenes.getById(ref.id);

                    if (scene != null)
                    {
                        errors.addAll(Backpack.exportDocument(
                            project, scene.name, Backpack.TYPE_SCENE, scenes.exportSceneData(scene)));
                    }
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Backpack -> work                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Take one backpack item into the current work. Scenes and sequences
     * are added as new assets (fresh ids), form categories are appended -
     * nothing already in the work is overwritten.
     *
     * @return human readable errors; empty means success
     */
    public static List<String> take(String itemName)
    {
        List<String> errors = new ArrayList<>();
        BBSProject project = ProjectManager.get() == null ? null : ProjectManager.get().getCurrent();

        if (project == null)
        {
            errors.add("No work is open.");

            return errors;
        }

        String type = Backpack.typeOf(itemName);

        if (type.isEmpty())
        {
            errors.add("Backpack item '" + itemName + "' no longer exists.");

            return errors;
        }

        if (Backpack.TYPE_FORM.equals(type))
        {
            return Backpack.importCategory(project, itemName);
        }

        /* Assets first, so the imported document resolves its models. */
        errors.addAll(Backpack.restoreAssets(project, itemName));

        MapType document = Backpack.readDocument(itemName);

        if (document == null)
        {
            errors.add("Backpack item '" + itemName + "' has no document.");

            return errors;
        }

        if (Backpack.TYPE_SCENE.equals(type))
        {
            SceneManager scenes = SceneManager.get();

            if (scenes == null || scenes.importSceneData(document, itemName) == null)
            {
                errors.add("Failed to import the scene '" + itemName + "'.");
            }
        }
        else if (Backpack.TYPE_SEQUENCE.equals(type))
        {
            SequenceManager sequences = SequenceManager.get();

            if (sequences == null || sequences.importSequenceData(document, itemName) == null)
            {
                errors.add("Failed to import the sequence '" + itemName + "'.");
            }
        }
        else
        {
            errors.add("Unknown backpack item type: " + type);
        }

        return errors;
    }

    /** Remove one item from the backpack. */
    public static void remove(String itemName)
    {
        Backpack.deleteItem(itemName);
    }
}
