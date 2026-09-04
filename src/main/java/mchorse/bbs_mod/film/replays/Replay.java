package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.values.ValuePoint;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueForm;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;

import lingfeng.bbsnext.film.replays.MaterialClip;
import lingfeng.bbsnext.film.replays.MaterialClips;
import lingfeng.bbsnext.film.replays.PotionClip;
import lingfeng.bbsnext.film.replays.PotionClips;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

public class Replay extends ValueGroup
{
    public final ValueForm form = new ValueForm("form");
    public final ReplayKeyframes keyframes = new ReplayKeyframes("keyframes");
    public final FormProperties properties = new FormProperties("properties");
    public final Clips actions = new Clips("actions", BBSMod.getFactoryActionClips());
    /** Material timeline: per-tick appearance overrides (model/skin/equip). */
    public final MaterialClips materials = new MaterialClips("materials");
    /** Potion-effect timeline: per-tick status effects (action characters only). */
    public final PotionClips potions = new PotionClips("potions");

    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueBoolean masked = new ValueBoolean("masked", false);
    public final ValueString label = new ValueString("label", "");
    public final ValueString nameTag = new ValueString("name_tag", "");
    public final ValueBoolean shadow = new ValueBoolean("shadow", true);
    public final ValueBoolean locked = new ValueBoolean("locked", false);
    public final ValueFloat shadowSize = new ValueFloat("shadow_size", 0.5F);
    public final ValueInt looping = new ValueInt("looping", 0);

    public final ValueBoolean actor = new ValueBoolean("actor", false);
    public final ValueBoolean fp = new ValueBoolean("fp", false);
    /** Character kind: "keyframe" (driven by {@link #keyframes}) or "action"
     *  (driven by {@link #actions} ActionClips). Exposed in the action editor. */
    public final ValueString characterType = new ValueString("characterType", "keyframe");
    public final ValueBoolean relative = new ValueBoolean("relative", false);
    public final ValuePoint relativeOffset = new ValuePoint("relativeOffset", new Point(0, 0, 0));

    public final ValueBoolean axesPreview = new ValueBoolean("axes_preview", false);
    public final ValueString axesPreviewBone = new ValueString("axes_preview_bone", "");

    public Replay(String id)
    {
        super(id);

        this.add(this.form);
        this.add(this.keyframes);
        this.add(this.properties);
        this.add(this.actions);
        this.add(this.materials);
        this.add(this.potions);

        this.add(this.enabled);
        this.add(this.label);
        this.add(this.nameTag);
        this.add(this.shadow);
        this.add(this.locked);
        this.add(this.shadowSize);
        this.add(this.looping);

        this.add(this.actor);
        this.add(this.fp);
        this.add(this.characterType);
        this.add(this.relative);
        this.add(this.relativeOffset);

        this.add(this.axesPreview);
        this.add(this.axesPreviewBone);
    }

    public String getName()
    {
        String label = this.label.get();

        if (!label.isEmpty())
        {
            return label;
        }

        Form form = this.form.get();

        if (form == null)
        {
            return "-";
        }

        return form.getDisplayName();
    }

    public void shift(float tick)
    {
        this.keyframes.shift(tick);
        this.properties.shift(tick);
        this.actions.shift(tick);
        this.materials.shift(tick);
        this.potions.shift(tick);
    }

    public void applyActions(LivingEntity actor, SuperFakePlayer fakePlayer, Film film, int tick)
    {
        List<Clip> clips = this.actions.getClips(tick);

        for (Clip clip : clips)
        {
            ((ActionClip) clip).apply(actor, fakePlayer, film, this, tick);
        }
    }

    public void applyClientActions(int tick, IEntity entity, Film film)
    {
        tick = this.getTick(tick);

        List<Clip> clips = this.actions.getClips(tick);

        for (Clip clip : clips)
        {
            if (clip instanceof ActionClip actionClip && actionClip.isClient())
            {
                actionClip.applyClient(entity, film, this, tick);
            }
        }

        /* Materials override appearance while active and auto-revert at edges.
         * Snapshot the "natural" form (baseline + morph actions already applied
         * above) so a non-active frame restores it without clobbering morphs. */
        Form naturalForm = entity.getForm();
        boolean formOverridden = false;

        for (MaterialClip mc : this.materials.getAllTyped())
        {
            if (!mc.isActive(tick))
            {
                continue;
            }

            if (MaterialClip.TYPE_EQUIP.equals(mc.type.get()))
            {
                entity.setEquipmentStack(MaterialClip.slotOf(mc.slot.get()), mc.resolveItem());
            }
            else
            {
                Form f = mc.resolveForm();

                if (f != null)
                {
                    entity.setForm(f);
                    formOverridden = true;
                }
            }
        }

        if (!formOverridden && naturalForm != null)
        {
            entity.setForm(naturalForm);
        }
    }

    public int getTick(int tick)
    {
        return this.looping.get() > 0 ? tick % this.looping.get() : tick;
    }
}