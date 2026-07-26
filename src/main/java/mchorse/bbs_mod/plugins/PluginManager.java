package mchorse.bbs_mod.plugins;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.BBSMod;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Discovers and runs user script plugins.
 *
 * <p>Plugins live in {@code <gameDir>/bbsnplugin/}. This path is resolved at runtime
 * from Fabric's game directory (never hardcoded). Two layouts are supported:</p>
 *
 * <ul>
 *   <li>A zip file: {@code bbsnplugin/my_plugin.zip} containing {@code plugin.json}
 *       and the entry script at its root.</li>
 *   <li>An unpacked folder: {@code bbsnplugin/my_plugin/} containing the same files.</li>
 * </ul>
 *
 * <p>This is the second plugin mechanism. The first one — Fabric mods that declare a
 * {@code bbs-addon} entrypoint — keeps working exactly as before; this is loaded in
 * addition to it.</p>
 */
public class PluginManager
{
    public static final String FOLDER_NAME = "bbsnplugin";
    public static final String MANIFEST = "plugin.json";

    private final List<ScriptPlugin> plugins = new ArrayList<>();

    public List<ScriptPlugin> getPlugins()
    {
        return this.plugins;
    }

    /**
     * Scans the plugins folder and runs every valid plugin found. Any failure in a
     * single plugin is logged and skipped so it can never break the rest of the mod.
     */
    public void load()
    {
        File folder = BBSMod.getGamePath(FOLDER_NAME);

        try
        {
            folder.mkdirs();
            this.ensureReadme(folder);
        }
        catch (Exception e)
        {
            BBSMod.LOGGER.warn("Failed to prepare plugins folder '{}': {}", folder, e.getMessage());

            return;
        }

        File[] entries = folder.listFiles();

        if (entries == null)
        {
            return;
        }

        for (File entry : entries)
        {
            try
            {
                if (entry.isDirectory())
                {
                    if (new File(entry, MANIFEST).isFile())
                    {
                        this.loadFolderPlugin(entry);
                    }
                }
                else if (entry.isFile() && entry.getName().toLowerCase().endsWith(".zip"))
                {
                    this.loadZipPlugin(entry);
                }
            }
            catch (Exception e)
            {
                BBSMod.LOGGER.error("Failed to load plugin from '{}': {}", entry.getName(), e.getMessage(), e);
            }
        }

        BBSMod.LOGGER.info("Loaded {} script plugin(s) from '{}'.", this.plugins.size(), folder);
    }

    private void loadFolderPlugin(File root) throws Exception
    {
        File manifestFile = new File(root, MANIFEST);

        String manifestJson = new String(Files.readAllBytes(manifestFile.toPath()), StandardCharsets.UTF_8);
        PluginManifest manifest = PluginManifest.fromJson(JsonParser.parseString(manifestJson).getAsJsonObject());

        if (!manifest.isValid())
        {
            BBSMod.LOGGER.warn("Skipping plugin '{}': invalid {} (needs 'id' and 'main').", root.getName(), MANIFEST);

            return;
        }

        File mainFile = new File(root, manifest.main);

        if (!mainFile.isFile())
        {
            BBSMod.LOGGER.warn("Skipping plugin '{}': entry script '{}' not found.", manifest.id, manifest.main);

            return;
        }

        String source = new String(Files.readAllBytes(mainFile.toPath()), StandardCharsets.UTF_8);

        this.runPlugin(new ScriptPlugin(manifest, root, source, manifest.id + "/" + manifest.main));
    }

    private void loadZipPlugin(File zip) throws Exception
    {
        /* Extract the whole zip to a temp folder so the plugin has a real directory it
           can read its own bundled files from, then load it as a folder plugin. */
        Path temp = Files.createTempDirectory("bbsnplugin-");

        temp.toFile().deleteOnExit();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip)))
        {
            ZipEntry zipEntry;

            while ((zipEntry = zis.getNextEntry()) != null)
            {
                Path resolved = temp.resolve(zipEntry.getName()).normalize();

                /* Zip-slip protection. */
                if (!resolved.startsWith(temp))
                {
                    continue;
                }

                if (zipEntry.isDirectory())
                {
                    Files.createDirectories(resolved);
                }
                else
                {
                    Files.createDirectories(resolved.getParent());
                    Files.copy(zis, resolved, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
            }
        }

        File root = temp.toFile();

        if (!new File(root, MANIFEST).isFile())
        {
            BBSMod.LOGGER.warn("Skipping plugin zip '{}': no {} at its root.", zip.getName(), MANIFEST);

            return;
        }

        this.loadFolderPlugin(root);
    }

    private void runPlugin(ScriptPlugin plugin)
    {
        BBSMod.LOGGER.info("Running script plugin '{}' v{} ({}).", plugin.manifest.name, plugin.manifest.version, plugin.manifest.id);

        if (plugin.run())
        {
            this.plugins.add(plugin);
        }
    }

    private void ensureReadme(File folder) throws Exception
    {
        File readme = new File(folder, "README.txt");

        if (readme.exists())
        {
            return;
        }

        String text = ""
            + "BBS Next - script plugins folder\n"
            + "================================\n\n"
            + "Drop a plugin here as either:\n"
            + "  - a zip file:   bbsnplugin/my_plugin.zip\n"
            + "  - or a folder:  bbsnplugin/my_plugin/\n\n"
            + "Each plugin must contain a plugin.json at its root, for example:\n\n"
            + "  {\n"
            + "    \"id\": \"my_plugin\",\n"
            + "    \"name\": \"My Plugin\",\n"
            + "    \"version\": \"1.0.0\",\n"
            + "    \"author\": \"You\",\n"
            + "    \"main\": \"main.js\"\n"
            + "  }\n\n"
            + "The 'main' file is a JavaScript file, run at game startup. A 'bbs' global\n"
            + "is available, plus full Java interop via Packages.*, for example:\n\n"
            + "  bbs.log(\"Hello from \" + bbs.getName());\n\n"
            + "  var RegisterSourcePacksEvent =\n"
            + "      Packages.mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;\n\n"
            + "  bbs.subscribe(RegisterSourcePacksEvent, function (event) {\n"
            + "      bbs.log(\"source packs registering\");\n"
            + "  });\n";

        Files.write(readme.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }
}
