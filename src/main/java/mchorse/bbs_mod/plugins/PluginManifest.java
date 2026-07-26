package mchorse.bbs_mod.plugins;

import com.google.gson.JsonObject;

/**
 * Metadata for a script plugin, parsed from a {@code plugin.json} file that sits
 * at the root of a plugin zip (or plugin folder).
 *
 * <p>Example {@code plugin.json}:</p>
 * <pre>
 * {
 *   "id": "my_plugin",
 *   "name": "My Plugin",
 *   "version": "1.0.0",
 *   "author": "Someone",
 *   "description": "Does something cool",
 *   "main": "main.js"
 * }
 * </pre>
 */
public class PluginManifest
{
    public String id;
    public String name;
    public String version = "1.0.0";
    public String author = "";
    public String description = "";
    /** Entry script, relative to the plugin root. Defaults to {@code main.js}. */
    public String main = "main.js";

    public static PluginManifest fromJson(JsonObject json)
    {
        PluginManifest manifest = new PluginManifest();

        if (json.has("id"))
        {
            manifest.id = json.get("id").getAsString();
        }

        if (json.has("name"))
        {
            manifest.name = json.get("name").getAsString();
        }

        if (json.has("version"))
        {
            manifest.version = json.get("version").getAsString();
        }

        if (json.has("author"))
        {
            manifest.author = json.get("author").getAsString();
        }

        if (json.has("description"))
        {
            manifest.description = json.get("description").getAsString();
        }

        if (json.has("main"))
        {
            manifest.main = json.get("main").getAsString();
        }

        if (manifest.name == null)
        {
            manifest.name = manifest.id;
        }

        return manifest;
    }

    public boolean isValid()
    {
        return this.id != null && !this.id.isEmpty() && this.main != null && !this.main.isEmpty();
    }
}
