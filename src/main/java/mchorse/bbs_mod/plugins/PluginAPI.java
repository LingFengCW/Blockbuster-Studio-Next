package mchorse.bbs_mod.plugins;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.events.EventBus;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.Scriptable;

import java.io.File;
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
    private final PluginManifest manifest;
    private final File folder;
    private Scriptable scope;

    public PluginAPI(PluginManifest manifest, File folder)
    {
        this.manifest = manifest;
        this.folder = folder;
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
