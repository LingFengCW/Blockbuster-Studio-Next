package mchorse.bbs_mod.plugins;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lingfeng.bbsnext.film.replays.ActionGroup;
import lingfeng.bbsnext.film.replays.ActionGroupLibrary;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.events.EventBus;
import mchorse.bbs_mod.projects.ProjectManager;
import mchorse.bbs_mod.utils.clips.Clip;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The {@code bbs} global object that gets injected into every script plugin's scope.
 *
 * <p>Scripts get full Java interop via Rhino's {@code Packages.*}, so this API is
 * mostly a set of convenience helpers on top of the mod's {@link EventBus} plus
 * some contextual info about the running plugin.</p>
 *
 * <p>Usage inside a plugin script:</p>
 * <pre>
 * bbs.log("Hello from " + bbs.getName());
 *
 * var RegisterSourcePacksEvent = Packages.mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;
 *
 * bbs.subscribe(RegisterSourcePacksEvent, function (event) {
 *     bbs.log("source packs are being registered");
 * });
 * </pre>
 */
public class PluginAPI
{
    private static final Gson GSON = new Gson();
    /** Set from the client module at startup so plugins can surface a toast
     *  without this main-side class depending on client UI code. */
    private static Consumer<String> toastHandler;

    private final PluginManifest manifest;
    private final File folder;
    private Scriptable scope;
    private JsonObject configCache;

    public PluginAPI(PluginManifest manifest, File folder)
    {
        this.manifest = manifest;
        this.folder = folder;
    }

    /** Client-side bridge: lets the main module raise a user-facing toast. */
    public static void setToastHandler(Consumer<String> handler)
    {
        toastHandler = handler;
    }

    /**
     * Package-private: called by {@link ScriptPlugin} after the standard scope is
     * created so callbacks can be invoked against the right scope later.
     */
    void setScope(Scriptable scope)
    {
        this.scope = scope;
    }

    /* ---- Contextual info exposed to scripts ---- */

    public String getId()
    {
        return this.manifest.id;
    }

    public String getName()
    {
        return this.manifest.name;
    }

    public String getVersion()
    {
        return this.manifest.version;
    }

    /**
     * The folder this plugin was loaded from (for zip plugins this is the extracted
     * temporary folder). Scripts can use it to load their own bundled files.
     */
    public File getDir()
    {
        return this.folder;
    }

    /**
     * The game directory ({@code .minecraft}), resolved at runtime. Never hardcoded.
     */
    public File getGameDir()
    {
        return BBSMod.getGameFolder();
    }

    /** Direct access to the mod's event bus, for advanced use. */
    public EventBus getEvents()
    {
        return BBSMod.events;
    }

    /* ---- Manifest accessors ---- */

    public String getAuthor()
    {
        return this.manifest.author;
    }

    public String getDescription()
    {
        return this.manifest.description;
    }

    public String getIcon()
    {
        return this.manifest.icon;
    }

    /* ---- High-level mod access (the stuff plugins actually want to touch) ---- */

    /** The project manager: list/create/select projects, read the current one. */
    public ProjectManager getProjectManager()
    {
        return ProjectManager.get();
    }

    /** All generic action groups currently in the shared library. */
    public List<ActionGroup> getActionGroups()
    {
        List<ActionGroup> out = new ArrayList<>();

        for (Clip clip : ActionGroupLibrary.get().get())
        {
            if (clip instanceof ActionGroup group)
            {
                out.add(group);
            }
        }

        return out;
    }

    /** Look up a single generic action group by its stable id. */
    public ActionGroup findActionGroup(String id)
    {
        return ActionGroupLibrary.find(id);
    }

    /** Persist a generic action group into the shared library. */
    public void addActionGroup(Object group)
    {
        if (group instanceof ActionGroup actionGroup)
        {
            ActionGroupLibrary.addGroup(actionGroup);
        }
        else
        {
            this.warn("addActionGroup() expected an ActionGroup, got " + (group == null ? "null" : group.getClass().getName()));
        }
    }

    /** Remove a generic action group from the shared library by id. */
    public void removeActionGroup(String id)
    {
        ActionGroupLibrary.removeGroup(id);
    }

    /* ---- Per-plugin persistent config ---- */

    private JsonObject config()
    {
        if (this.configCache == null)
        {
            this.configCache = this.loadConfig();
        }

        return this.configCache;
    }

    private JsonObject loadConfig()
    {
        Path path = new File(this.folder, "bbs_plugin_config.json").toPath();

        if (Files.exists(path))
        {
            try
            {
                return JsonParser.parseString(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
            }
            catch (Exception e)
            {
                this.warn("could not read config, starting fresh: " + e.getMessage());
            }
        }

        return new JsonObject();
    }

    private void saveConfig()
    {
        try
        {
            Path path = new File(this.folder, "bbs_plugin_config.json").toPath();
            Files.write(path, GSON.toJson(this.config()).getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            this.error("could not save config: " + e.getMessage());
        }
    }

    /** Read a previously stored config value (or null). */
    public Object getConfig(String key)
    {
        JsonObject cfg = this.config();

        return cfg.has(key) ? GSON.fromJson(cfg.get(key), Object.class) : null;
    }

    /** Store a config value (string/number/boolean) and persist it. */
    public void setConfig(String key, Object value)
    {
        this.config().add(key, GSON.toJsonTree(value));
        this.saveConfig();
    }

    /* ---- Bundled resource IO ---- */

    /** Read a file bundled inside the plugin folder as text. */
    public String readResource(String name)
    {
        try
        {
            Path path = new File(this.folder, name).toPath();

            return Files.readAllBytes(path).length == 0 ? "" : new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            this.warn("readResource('" + name + "') failed: " + e.getMessage());

            return null;
        }
    }

    /** Write text to a file inside the plugin folder. */
    public void writeResource(String name, String content)
    {
        try
        {
            Path path = new File(this.folder, name).toPath();

            Files.write(path, (content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            this.error("writeResource('" + name + "') failed: " + e.getMessage());
        }
    }

    /* ---- Helpers ---- */

    public void log(Object message)
    {
        BBSMod.LOGGER.info("[plugin:{}] {}", this.manifest.id, String.valueOf(message));
    }

    public void warn(Object message)
    {
        BBSMod.LOGGER.warn("[plugin:{}] {}", this.manifest.id, String.valueOf(message));
    }

    public void error(Object message)
    {
        BBSMod.LOGGER.error("[plugin:{}] {}", this.manifest.id, String.valueOf(message));
    }

    /**
     * Surface a short message to the user. If a client toast handler is
     * registered it is used; otherwise the message is logged so it is never
     * silently lost.
     */
    public void toast(Object message)
    {
        String text = String.valueOf(message);

        if (toastHandler != null)
        {
            toastHandler.accept(text);
        }
        else
        {
            this.log(text);
        }
    }

    /**
     * Subscribes a JS function to a BBS event type. The event type can be passed as
     * a Java class reference from the script (e.g.
     * {@code Packages.mchorse.bbs_mod.events.register.RegisterSourcePacksEvent})
     * or as a fully-qualified class name string.
     *
     * <p>The callback is invoked with the event object whenever it is posted. Rhino
     * contexts are entered per-invocation so this is safe across threads.</p>
     */
    public void subscribe(Object type, final Function callback)
    {
        final Class<?> eventClass = this.resolveClass(type);

        if (eventClass == null)
        {
            this.warn("subscribe() could not resolve event type: " + type);

            return;
        }

        if (callback == null)
        {
            this.warn("subscribe() was given a null callback for " + eventClass.getName());

            return;
        }

        final Scriptable pluginScope = this.scope;

        Consumer<Object> handler = (event) ->
        {
            Context cx = Context.enter();

            try
            {
                callback.call(cx, pluginScope, pluginScope, new Object[]{Context.javaToJS(event, pluginScope)});
            }
            catch (Exception e)
            {
                this.error("error in event handler for " + eventClass.getSimpleName() + ": " + e.getMessage());
            }
            finally
            {
                Context.exit();
            }
        };

        BBSMod.events.subscribe((Class<Object>) eventClass, handler);
    }

    private Class<?> resolveClass(Object type)
    {
        if (type instanceof NativeJavaClass)
        {
            return ((NativeJavaClass) type).getClassObject();
        }

        if (type instanceof Class<?>)
        {
            return (Class<?>) type;
        }

        if (type instanceof CharSequence)
        {
            try
            {
                return Class.forName(type.toString());
            }
            catch (ClassNotFoundException e)
            {
                return null;
            }
        }

        return null;
    }
}
