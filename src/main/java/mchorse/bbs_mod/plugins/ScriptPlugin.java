package mchorse.bbs_mod.plugins;

import mchorse.bbs_mod.BBSMod;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;

import java.io.File;

/**
 * A single loaded script plugin. Holds its manifest and the source of its entry
 * script, and knows how to execute it in a fresh Rhino scope with the {@code bbs}
 * global injected.
 */
public class ScriptPlugin
{
    public final PluginManifest manifest;
    /** Folder the plugin was loaded from (extracted temp folder for zip plugins). */
    public final File folder;
    private final String source;
    private final String sourceName;

    public ScriptPlugin(PluginManifest manifest, File folder, String source, String sourceName)
    {
        this.manifest = manifest;
        this.folder = folder;
        this.source = source;
        this.sourceName = sourceName;
    }

    /**
     * Executes the plugin's entry script. Returns true on success.
     */
    public boolean run()
    {
        Context cx = Context.enter();

        try
        {
            /* Use the interpreter (no runtime bytecode generation) so we don't depend
               on the host JVM's class-file version. Safe across Java versions. */
            cx.setOptimizationLevel(-1);
            cx.setLanguageVersion(Context.VERSION_ES6);

            /* ImporterTopLevel enables importPackage()/importClass() and Packages.* so
               scripts get full Java interop against the mod's classes. */
            Scriptable scope = new ImporterTopLevel(cx);

            PluginAPI api = new PluginAPI(this.manifest, this.folder);

            api.setScope(scope);

            Object jsApi = Context.javaToJS(api, scope);

            scope.put("bbs", scope, jsApi);

            cx.evaluateString(scope, this.source, this.sourceName, 1, null);

            return true;
        }
        catch (Exception e)
        {
            BBSMod.LOGGER.error("[plugin:{}] failed to run script '{}': {}", this.manifest.id, this.sourceName, e.getMessage(), e);

            return false;
        }
        finally
        {
            Context.exit();
        }
    }
}
