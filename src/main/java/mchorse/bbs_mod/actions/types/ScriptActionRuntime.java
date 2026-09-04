package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import net.minecraft.world.entity.LivingEntity;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;

/**
 * Runs a {@link ScriptActionClip}'s JavaScript source against a character.
 *
 * <p>The script gets a fresh Rhino scope (interpreter mode, ES6) with the
 * character references and a {@code bbs} helper injected:</p>
 * <pre>
 *   bbs.potion("speed", 200, 1);          // 给角色加速 10 秒
 *   bbs.heldMain("diamond_sword");         // 主手换成钻石剑
 *   bbs.health(20);                         // 回满血
 *   bbs.pose("wave");                       // 触发一个 pose（若 form 支持）
 *   // actor / player / replay / film / tick 也在作用域内，可用 Packages.* 做高级操作
 * </pre>
 *
 * <p>The script source is compiled once and cached on the clip (keyed by a
 * revision counter) so repeated ticks don't recompile.</p>
 */
public class ScriptActionRuntime
{
    public static void run(ScriptActionClip clip, LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        String src = clip.script.get();

        if (src == null || src.isBlank())
        {
            return;
        }

        Context cx = Context.enter();

        try
        {
            cx.setOptimizationLevel(-1);
            cx.setLanguageVersion(Context.VERSION_ES6);

            Scriptable scope = new ImporterTopLevel(cx);

            scope.put("actor", scope, Context.javaToJS(actor, scope));
            scope.put("player", scope, Context.javaToJS(player, scope));
            scope.put("replay", scope, Context.javaToJS(replay, scope));
            scope.put("film", scope, Context.javaToJS(film, scope));
            scope.put("tick", scope, tick);
            scope.put("duration", scope, clip.duration.get());

            ScriptActionAPI api = new ScriptActionAPI(actor, player, replay, film, clip, tick);

            scope.put("bbs", scope, Context.javaToJS(api, scope));

            String name = clip.title.get();

            cx.evaluateString(scope, src, (name == null || name.isEmpty() ? "script" : name) + " <script>", 1, null);
        }
        catch (Exception e)
        {
            BBSMod.LOGGER.error("[script:{}] runtime error: {}", clip.title.get(), e.getMessage(), e);
        }
        finally
        {
            Context.exit();
        }
    }
}
