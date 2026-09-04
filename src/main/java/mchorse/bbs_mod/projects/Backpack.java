package mchorse.bbs_mod.projects;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.StringType;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * The Backpack is a cross-project shared asset library. Any work in a
 * project (form categories and the assets they reference) can be exported
 * into the backpack and then imported into any other project, enabling
 * fast migration of assets between isolated workspaces.
 *
 * Layout of one backpack item:
 *   <gameDir>/bbs/backpack/<itemName>/form_category.json   - exported form category
 *   <gameDir>/bbs/backpack/<itemName>/meta.json            - origin project id (collision handling)
 *   <gameDir>/bbs/backpack/<itemName>/assets/<relPath>     - every asset the category references
 *
 * Unlike the original implementation this also packages the actual asset
 * files (models, textures, audio, particles, ...) that the exported forms
 * point to, so an import into another project yields working forms instead
 * of dead references.
 */
public class Backpack
{
    public static final String CATEGORY_FILE = "form_category.json";
    public static final String META_FILE = "meta.json";
    public static final String ASSETS_DIR = "assets";

    /** Generic document file used by scene / sequence backpack items. */
    public static final String DOCUMENT_FILE = "document.json";

    /* Item types. "form" is the legacy form-category item, the other two
     * hold a whole scene or a reusable sequence. */
    public static final String TYPE_FORM = "form";
    public static final String TYPE_SCENE = "scene";
    public static final String TYPE_SEQUENCE = "sequence";
    public static final String TYPE_REPLAY = "replay";
    public static final String TYPE_CAMERA = "camera";
    public static final String TYPE_CAMERAGROUP = "cameragroup";

    /**
     * One backpack item: the folder name (unique key), its type and the
     * human readable label stored at export time.
     */
    public static class Entry
    {
        public final String name;
        public final String type;
        public final String label;

        public Entry(String name, String type, String label)
        {
            this.name = name;
            this.type = type;
            this.label = label == null || label.isEmpty() ? name : label;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Backpack.class.getName());

    /* Asset roots that a form may reference, and the file extensions we try
     * when a reference is stored as a bare name (e.g. a model called "foo"
     * resolves to models/foo.json). */
    private static final String[] MODEL_EXTS = {".json", ".bobj", ".geo.json", ".bbs.json", ".animation.json"};
    private static final String[] AUDIO_EXTS = {".wav", ".ogg", ".mp3", ".json"};

    public static Path getRoot()
    {
        return FabricLoader.getInstance().getGameDir().resolve("bbs").resolve("backpack");
    }

    /**
     * List every backpack item (directory that holds either a form category
     * or a generic scene / sequence document).
     */
    public static List<String> getItems()
    {
        List<String> items = new ArrayList<>();

        for (Entry entry : getEntries())
        {
            items.add(entry.name);
        }

        return items;
    }

    /**
     * List every backpack item together with its type and label, so a UI can
     * tell a scene apart from a sequence or a form category.
     */
    public static List<Entry> getEntries()
    {
        List<Entry> entries = new ArrayList<>();
        Path root = getRoot();

        if (!Files.isDirectory(root))
        {
            return entries;
        }

        try (Stream<Path> dirs = Files.list(root))
        {
            dirs.filter(Files::isDirectory)
                .filter(dir -> Files.exists(dir.resolve(CATEGORY_FILE)) || Files.exists(dir.resolve(DOCUMENT_FILE)))
                .sorted(Comparator.comparing(dir -> dir.getFileName().toString()))
                .forEach(dir ->
                {
                    String name = dir.getFileName().toString();
                    MapType meta = readMeta(dir);
                    String type = meta.getString("type", "");

                    if (type.isEmpty())
                    {
                        /* Items written before typed documents existed are
                         * always form categories. */
                        type = TYPE_FORM;
                    }

                    entries.add(new Entry(name, type, meta.getString("label", name)));
                });
        }
        catch (IOException e)
        {
            LOGGER.warning("Failed to list backpack items: " + e.getMessage());
        }

        return entries;
    }

    /** Type of one backpack item ({@link #TYPE_FORM} when unknown). */
    public static String typeOf(String itemName)
    {
        Path dir = getRoot().resolve(itemName);

        if (!Files.isDirectory(dir))
        {
            return "";
        }

        String type = readMeta(dir).getString("type", "");

        return type.isEmpty() ? TYPE_FORM : type;
    }

    /* ------------------------------------------------------------------ */
    /* Generic documents (scene / sequence)                                */
    /* ------------------------------------------------------------------ */

    /**
     * Store any exported document (a scene with its film payload, or a
     * sequence with its reference list) as a backpack item, bundling every
     * project asset the document references.
     *
     * @return human readable errors; empty means success.
     */
    public static List<String> exportDocument(BBSProject project, String label, String type, MapType document)
    {
        List<String> errors = new ArrayList<>();

        if (project == null || document == null)
        {
            errors.add("Nothing to put into the backpack.");

            return errors;
        }

        String safe = sanitize(label);

        try
        {
            Path itemDir = resolveItemDir(safe, project.id);

            Files.createDirectories(itemDir);
            Files.write(itemDir.resolve(DOCUMENT_FILE), DataStorageUtils.writeToBytes(document));

            MapType meta = new MapType();

            meta.putString("projectId", project.id);
            meta.putString("type", type);
            meta.putString("label", label);

            Files.write(itemDir.resolve(META_FILE), DataStorageUtils.writeToBytes(meta));

            /* Bundle every asset the document points at. */
            Path settings = project.getDirectory().resolve("settings");
            Set<String> strings = new HashSet<>();
            Set<String> assets = new HashSet<>();

            collectStrings(document, strings);

            for (String s : strings)
            {
                resolveAsset(s, settings, assets);
            }

            for (String rel : assets)
            {
                Path source = settings.resolve(rel);

                if (Files.isRegularFile(source))
                {
                    Path dest = itemDir.resolve(ASSETS_DIR).resolve(rel);

                    Files.createDirectories(dest.getParent());
                    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        catch (IOException e)
        {
            errors.add("Failed to put '" + label + "' into the backpack: " + e.getMessage());
            LOGGER.warning(errors.get(errors.size() - 1));
        }

        return errors;
    }

    /** Read back the document of a scene / sequence backpack item. */
    public static MapType readDocument(String itemName)
    {
        Path file = getRoot().resolve(itemName).resolve(DOCUMENT_FILE);

        if (!Files.isRegularFile(file))
        {
            return null;
        }

        try
        {
            BaseType data = DataStorageUtils.readFromBytes(Files.readAllBytes(file));

            return data instanceof MapType map ? map : null;
        }
        catch (IOException e)
        {
            LOGGER.warning("Failed to read backpack document " + file + ": " + e.getMessage());

            return null;
        }
    }

    /**
     * Copy the assets bundled with a backpack item back into a project, so
     * the imported scene / sequence resolves its models and textures.
     */
    public static List<String> restoreAssets(BBSProject project, String itemName)
    {
        List<String> errors = new ArrayList<>();

        if (project == null)
        {
            return errors;
        }

        Path assetsSrc = getRoot().resolve(itemName).resolve(ASSETS_DIR);

        if (!Files.isDirectory(assetsSrc))
        {
            return errors;
        }

        Path settings = project.getDirectory().resolve("settings");

        try
        {
            Files.createDirectories(settings);
        }
        catch (IOException e)
        {
            errors.add("Failed to create the settings directory: " + e.getMessage());

            return errors;
        }

        copyRecursively(assetsSrc, settings, errors);

        return errors;
    }

    /** Filesystem-safe item name. */
    public static String sanitize(String name)
    {
        String safe = name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();

        return safe.isEmpty() ? "item" : safe;
    }

    /**
     * Export every user form category of the project into the backpack,
     * together with all assets those forms reference.
     *
     * @return a list of human-readable error messages; empty means success.
     */
    public static List<String> exportCategories(BBSProject project)
    {
        List<String> errors = new ArrayList<>();
        Path formsDir = project.getDirectory().resolve("settings").resolve("forms");

        if (!Files.isDirectory(formsDir))
        {
            return errors;
        }

        try (Stream<Path> files = Files.list(formsDir))
        {
            files.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .forEach(file ->
                {
                    String name = file.getFileName().toString().replace(".json", "");

                    try
                    {
                        Path itemDir = resolveItemDir(name, project.id);
                        Path target = itemDir.resolve(CATEGORY_FILE);

                        Files.createDirectories(itemDir);
                        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                        writeMeta(itemDir, project.id);

                        /* Bundle every asset referenced by this category. */
                        Path settings = project.getDirectory().resolve("settings");
                        Set<String> assets = collectAssetPaths(file, settings);

                        for (String rel : assets)
                        {
                            Path source = settings.resolve(rel);

                            if (Files.isRegularFile(source))
                            {
                                Path dest = itemDir.resolve(ASSETS_DIR).resolve(rel);

                                Files.createDirectories(dest.getParent());
                                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        errors.add("Failed to export '" + name + "': " + e.getMessage());
                        LOGGER.warning(errors.get(errors.size() - 1));
                    }
                });
        }
        catch (IOException e)
        {
            errors.add("Failed to read forms directory: " + e.getMessage());
            LOGGER.warning(errors.get(errors.size() - 1));
        }

        return errors;
    }

    /**
     * Import one backpack item into the project. Its assets are copied back
     * into the project's settings and the form category is appended as the
     * next forms/<index>.json file so nothing in the project is overwritten.
     *
     * @return a list of human-readable error messages; empty means success.
     */
    public static List<String> importCategory(BBSProject project, String itemName)
    {
        List<String> errors = new ArrayList<>();
        Path itemDir = getRoot().resolve(itemName);

        if (!Files.isDirectory(itemDir))
        {
            errors.add("Backpack item '" + itemName + "' no longer exists.");
            return errors;
        }

        Path formsDir = project.getDirectory().resolve("settings").resolve("forms");
        Path assetsSrc = itemDir.resolve(ASSETS_DIR);

        try
        {
            /* Copy bundled assets into the project's settings. */
            if (Files.isDirectory(assetsSrc))
            {
                Path settings = project.getDirectory().resolve("settings");

                Files.createDirectories(settings);
                copyRecursively(assetsSrc, settings, errors);
            }

            /* Append the form category as the next free index. */
            Path source = itemDir.resolve(CATEGORY_FILE);

            if (Files.isRegularFile(source))
            {
                Files.createDirectories(formsDir);

                int index = 0;

                while (Files.exists(formsDir.resolve(index + ".json")))
                {
                    index++;
                }

                Files.copy(source, formsDir.resolve(index + ".json"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e)
        {
            errors.add("Failed to import '" + itemName + "': " + e.getMessage());
            LOGGER.warning(errors.get(errors.size() - 1));
        }

        return errors;
    }

    /**
     * Delete one backpack item.
     */
    public static void deleteItem(String itemName)
    {
        Path itemDir = getRoot().resolve(itemName);

        try
        {
            if (Files.isDirectory(itemDir))
            {
                try (Stream<Path> walk = Files.walk(itemDir))
                {
                    walk.sorted(Comparator.reverseOrder()).forEach(path ->
                    {
                        try
                        {
                            Files.delete(path);
                        }
                        catch (IOException e)
                        {
                            LOGGER.warning("Failed to delete " + path + ": " + e.getMessage());
                        }
                    });
                }
            }
        }
        catch (IOException e)
        {
            LOGGER.warning("Failed to delete backpack item '" + itemName + "': " + e.getMessage());
        }
    }

    /* ------------------------------------------------------------------ */
    /* Item directory resolution (collision handling)                     */
    /* ------------------------------------------------------------------ */

    /**
     * Resolve the directory an exported item should live in.
     *
     * - If no item with that name exists yet, use it directly.
     * - If an item exists and was exported from the same project, overwrite
     *   it (a re-export of the same work).
     * - Otherwise the name collides with a different project's item, so we
     *   append a numeric suffix instead of silently overwriting it.
     */
    private static Path resolveItemDir(String name, String projectId)
    {
        Path dir = getRoot().resolve(name);

        if (!Files.exists(dir))
        {
            return dir;
        }

        String existing = readProjectId(dir);

        if (existing.equals(projectId))
        {
            return dir;
        }

        int suffix = 2;

        while (true)
        {
            Path candidate = getRoot().resolve(name + " (" + suffix + ")");

            if (!Files.exists(candidate))
            {
                return candidate;
            }

            suffix++;
        }
    }

    private static void writeMeta(Path itemDir, String projectId)
    {
        try
        {
            MapType data = new MapType();
            data.putString("projectId", projectId);

            Files.createDirectories(itemDir);
            Files.write(itemDir.resolve(META_FILE), DataStorageUtils.writeToBytes(data));
        }
        catch (IOException e)
        {
            LOGGER.warning("Failed to write backpack meta for " + itemDir + ": " + e.getMessage());
        }
    }

    private static String readProjectId(Path itemDir)
    {
        return readMeta(itemDir).getString("projectId", "");
    }

    /** Meta of one item; an empty map when it is missing or unreadable. */
    private static MapType readMeta(Path itemDir)
    {
        Path meta = itemDir.resolve(META_FILE);

        if (!Files.isRegularFile(meta))
        {
            return new MapType();
        }

        try
        {
            BaseType data = DataStorageUtils.readFromBytes(Files.readAllBytes(meta));

            if (data instanceof MapType map)
            {
                return map;
            }
        }
        catch (IOException e)
        {
            LOGGER.warning("Failed to read backpack meta " + meta + ": " + e.getMessage());
        }

        return new MapType();
    }

    /* ------------------------------------------------------------------ */
    /* Asset reference collection                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Walk a form category file and return the settings-relative paths of
     * every asset it references (models, textures, audio, particles, ...).
     */
    private static Set<String> collectAssetPaths(Path formFile, Path settings)
    {
        Set<String> refs = new HashSet<>();
        Set<String> strings = new HashSet<>();

        try
        {
            byte[] bytes = Files.readAllBytes(formFile);
            BaseType data = DataStorageUtils.readFromBytes(bytes);

            collectStrings(data, strings);
        }
        catch (IOException e)
        {
            LOGGER.warning("Failed to read form category " + formFile + ": " + e.getMessage());
            return refs;
        }

        for (String s : strings)
        {
            resolveAsset(s, settings, refs);
        }

        return refs;
    }

    private static void resolveAsset(String raw, Path settings, Set<String> out)
    {
        String ref = raw;

        if (ref.startsWith("assets:"))
        {
            ref = ref.substring("assets:".length());
        }

        if (ref.isEmpty())
        {
            return;
        }

        /* A reference may already be a settings-relative path, e.g. the
         * "assets:textures/foo.png" links stored by ValueLink. */
        Path direct = settings.resolve(ref);

        if (Files.isRegularFile(direct))
        {
            out.add(ref);
        }

        /* Bare names (e.g. a model called "foo" or "sub/foo") resolve under
         * their asset root with one of the known extensions. */
        for (String ext : MODEL_EXTS)
        {
            Path p = settings.resolve("models/" + ref + ext);

            if (Files.isRegularFile(p))
            {
                out.add("models/" + ref + ext);
            }
        }

        Path particle = settings.resolve("particles/" + ref + ".json");

        if (Files.isRegularFile(particle))
        {
            out.add("particles/" + ref + ".json");
        }

        for (String ext : AUDIO_EXTS)
        {
            Path p = settings.resolve("audio/" + ref + ext);

            if (Files.isRegularFile(p))
            {
                out.add("audio/" + ref + ext);
            }
        }
    }

    private static void collectStrings(BaseType data, Set<String> out)
    {
        if (data == null)
        {
            return;
        }

        if (data.isMap())
        {
            for (Map.Entry<String, BaseType> entry : (MapType) data)
            {
                collectStrings(entry.getValue(), out);
            }
        }
        else if (data.isList())
        {
            ListType list = (ListType) data;

            for (int i = 0; i < list.size(); i++)
            {
                collectStrings(list.get(i), out);
            }
        }
        else if (data.isString())
        {
            out.add(((StringType) data).value);
        }
    }

    /* ------------------------------------------------------------------ */
    /* File utilities                                                      */
    /* ------------------------------------------------------------------ */

    private static void copyRecursively(Path src, Path dst, List<String> errors)
    {
        try (Stream<Path> walk = Files.walk(src))
        {
            walk.filter(Files::isRegularFile).forEach(source ->
            {
                try
                {
                    Path relative = src.relativize(source);
                    Path target = dst.resolve(relative);

                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
                catch (IOException e)
                {
                    errors.add("Failed to copy asset " + source + ": " + e.getMessage());
                    LOGGER.warning(errors.get(errors.size() - 1));
                }
            });
        }
        catch (IOException e)
        {
            errors.add("Failed to read backpack assets: " + e.getMessage());
            LOGGER.warning(errors.get(errors.size() - 1));
        }
    }
}
