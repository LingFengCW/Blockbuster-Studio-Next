package lingfeng.bbsnext.film.replays;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.settings.values.core.ValueList;

/**
 * Ordered list of {@link MaterialClip}s owned by a {@link Replay}. Backed by
 * {@link ValueList} so it serialises/deserialises like the rest of the value
 * tree (add / remove / reorder + auto id re-index via {@code sync()}).
 *
 * <p>Lives in the {@code lingfeng.bbsnext} namespace (personal code folder) but
 * in the {@code main} source set, because {@code Replay} (compiled for client
 * and server) holds and applies it.</p>
 */
public class MaterialClips extends ValueList<MaterialClip>
{
    public MaterialClips(String id)
    {
        super(id);
    }

    @Override
    protected MaterialClip create(String id)
    {
        return new MaterialClip(id);
    }

    /** Offset every material clip's start tick by {@code tick} (mirrors
     *  {@code Clips.shift} so the whole replay can be shifted as one unit). */
    public void shift(float tick)
    {
        for (MaterialClip mc : this.getAllTyped())
        {
            mc.tick.set(Math.round(mc.tick.get() + tick));
        }
    }

    /** Apply every active material clip to {@code entity} at {@code tick}. Shared
     *  by actor replays and the camera replay's material timeline so both use
     *  identical form/equipment swap semantics. Snapshots the natural form and
     *  restores it on frames with no active clip, so morphs are not clobbered. */
    public void applyTo(IEntity entity, int tick)
    {
        Form naturalForm = entity.getForm();
        boolean formOverridden = false;

        for (MaterialClip mc : this.getAllTyped())
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

                    String expression = mc.expression.get();

                    if (!expression.isEmpty() && f instanceof ModelForm modelForm)
                    {
                        modelForm.setActiveExpression(expression);
                    }
                }
            }
        }

        if (!formOverridden && naturalForm != null)
        {
            entity.setForm(naturalForm);
        }
    }
}
