package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Convenience API injected into a script action's scope as the {@code bbs}
 * global. Scripts drive a character by calling these helpers; they also get
 * the raw {@code actor} / {@code player} / {@code replay} / {@code film}
 * references (plus full Java interop via Rhino's {@code Packages.*}) for
 * anything more advanced.
 *
 * <p>Supported effects (declared in the script header via {@code // @use ...}):
 * <ul>
 *   <li>potion / health / held item — direct state changes</li>
 *   <li>pose — named character pose (best-effort, depends on the form)</li>
 *   <li>limbs — exposed through {@code replay.form} / {@code actor} for advanced users</li>
 * </ul>
 */
public class ScriptActionAPI
{
    private final LivingEntity actor;
    private final SuperFakePlayer player;
    private final Replay replay;
    private final Film film;
    private final ScriptActionClip clip;
    private final int tick;

    public ScriptActionAPI(LivingEntity actor, SuperFakePlayer player, Replay replay, Film film, ScriptActionClip clip, int tick)
    {
        this.actor = actor;
        this.player = player;
        this.replay = replay;
        this.film = film;
        this.clip = clip;
        this.tick = tick;
    }

    public void log(Object message)
    {
        BBSMod.LOGGER.info("[script:{}] {}", clip.title.get(), message);
    }

    public void warn(Object message)
    {
        BBSMod.LOGGER.warn("[script:{}] {}", clip.title.get(), message);
    }

    public void error(Object message)
    {
        BBSMod.LOGGER.error("[script:{}] {}", clip.title.get(), message);
    }

    /* ---- potion effects ---- */

    public void potion(Object id, int durationTicks, int amplifier)
    {
        Holder<MobEffect> effect = resolveEffect(id);

        if (effect == null)
        {
            warn("potion(): unknown effect id " + id);

            return;
        }

        if (actor != null)
        {
            actor.addEffect(new MobEffectInstance(effect, Math.max(1, durationTicks), Math.max(0, amplifier)));
        }
    }

    public void potionClear()
    {
        if (actor != null)
        {
            actor.removeAllEffects();
        }
    }

    public void potionRemove(Object id)
    {
        Holder<MobEffect> effect = resolveEffect(id);

        if (effect != null && actor != null)
        {
            actor.removeEffect(effect);
        }
    }

    /* ---- held item ---- */

    public void heldMain(Object item)
    {
        setHand(InteractionHand.MAIN_HAND, item);
    }

    public void heldOff(Object item)
    {
        setHand(InteractionHand.OFF_HAND, item);
    }

    private void setHand(InteractionHand hand, Object item)
    {
        ItemStack stack = resolveItem(item);

        if (player != null)
        {
            player.setItemInHand(hand, stack);
        }

        if (actor != null)
        {
            actor.setItemInHand(hand, stack);
        }
    }

    /* ---- health ---- */

    public void health(float value)
    {
        if (actor != null)
        {
            actor.setHealth(value);
        }
    }

    /* ---- pose / limbs (best-effort) ---- */

    /**
     * Trigger a named pose on the character's form, if the form supports it.
     * Silently no-ops (with a warning) when the form has no {@code setPose}.
     */
    public void pose(Object name)
    {
        try
        {
            Object form = replay != null ? replay.form.get() : null;

            if (form == null)
            {
                return;
            }

            java.lang.reflect.Method m = form.getClass().getMethod("setPose", String.class);

            m.invoke(form, String.valueOf(name));
        }
        catch (NoSuchMethodException e)
        {
            warn("pose(): form 不支持 setPose（肢体状态请用 replay.form / actor 直接操作）");
        }
        catch (Exception e)
        {
            warn("pose(): " + e.getMessage());
        }
    }

    /** Direct access to the character's form for advanced limb/pose manipulation. */
    public Object form()
    {
        return replay != null ? replay.form.get() : null;
    }

    /* ---- resolvers ---- */

    private Holder<MobEffect> resolveEffect(Object id)
    {
        if (id instanceof MobEffect me)
        {
            return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(me);
        }

        if (id instanceof Number n)
        {
            MobEffect e = BuiltInRegistries.MOB_EFFECT.byId(n.intValue());

            return e == null ? null : BuiltInRegistries.MOB_EFFECT.wrapAsHolder(e);
        }

        String s = String.valueOf(id).trim();

        if (!s.contains(":"))
        {
            s = "minecraft:" + s;
        }

        try
        {
            MobEffect e = BuiltInRegistries.MOB_EFFECT.getValue(Identifier.parse(s));

            return e == null ? null : BuiltInRegistries.MOB_EFFECT.wrapAsHolder(e);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private ItemStack resolveItem(Object item)
    {
        if (item instanceof ItemStack is)
        {
            return is;
        }

        if (item instanceof net.minecraft.world.item.Item i)
        {
            return new ItemStack(i);
        }

        String s = String.valueOf(item).trim();

        if (!s.contains(":"))
        {
            s = "minecraft:" + s;
        }

        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(s)));
    }
}
