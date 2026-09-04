package lingfeng.bbsnext.film.replays;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * A "material" clip placed on the action editor's material timeline. While
 * active (tick inside [tick, tick + duration)) it overrides part of the
 * replay's appearance:
 *
 * <ul>
 *   <li>{@code model}  - morph to another form (resolved from a model id)</li>
 *   <li>{@code skin}   - swap the player skin/texture (player MobForm + link)</li>
 *   <li>{@code equip}  - put an item into one equipment slot</li>
 * </ul>
 *
 * When no material clip is active the replay falls back to its baseline form
 * and the keyframe-driven equipment, so clips auto-revert at the edges.
 *
 * <p>Lives in the {@code lingfeng.bbsnext} namespace (personal code folder) but
 * in the {@code main} source set, because {@code Replay} (compiled for client
 * and server) holds and applies it.</p>
 */
public class MaterialClip extends ValueGroup
{
    public static final String TYPE_MODEL = "model";
    public static final String TYPE_SKIN = "skin";
    public static final String TYPE_EQUIP = "equip";

    public final ValueString type = new ValueString("type", TYPE_MODEL);
    public final ValueString target = new ValueString("target", "");
    public final ValueString slot = new ValueString("slot", "mainHand");
    public final ValueString item = new ValueString("item", "");
    public final ValueInt tick = new ValueInt("tick", 0);
    public final ValueInt duration = new ValueInt("duration", 30);
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);

    private transient String formKey = "";
    private transient Form cachedForm = null;
    private transient String itemKey = "";
    private transient ItemStack cachedItem = ItemStack.EMPTY;

    public MaterialClip(String id)
    {
        super(id);

        this.add(this.type);
        this.add(this.target);
        this.add(this.slot);
        this.add(this.item);
        this.add(this.tick);
        this.add(this.duration);
        this.add(this.enabled);
    }

    public boolean isActive(int tick)
    {
        return this.enabled.get() && tick >= this.tick.get() && tick < this.tick.get() + this.duration.get();
    }

    public Form resolveForm()
    {
        String key = this.type.get() + "|" + this.target.get();

        if (!key.equals(this.formKey) || this.cachedForm == null)
        {
            this.formKey = key;
            this.cachedForm = buildForm(this.type.get(), this.target.get());
        }

        return this.cachedForm;
    }

    public ItemStack resolveItem()
    {
        String key = this.item.get();

        if (!key.equals(this.itemKey))
        {
            this.itemKey = key;
            this.cachedItem = buildItem(key);
        }

        return this.cachedItem;
    }

    private static Form buildForm(String type, String target)
    {
        if (target == null || target.isEmpty())
        {
            return null;
        }

        if (TYPE_SKIN.equals(type))
        {
            MobForm mf = new MobForm();

            mf.mobID.set("minecraft:player");
            mf.texture.set(Link.assets(target));

            return mf;
        }

        if (target.startsWith("MODEL:"))
        {
            ModelForm f = new ModelForm();

            f.model.set(target.substring(6));

            return f;
        }

        if (target.startsWith("PARTICLE:"))
        {
            ParticleForm f = new ParticleForm();

            f.effect.set(target.substring(9));

            return f;
        }

        /* Plain mob id or "group:" model group -> MobForm morph */
        MobForm mf = new MobForm();

        if (target.startsWith("group:"))
        {
            String g = target.substring(6);

            mf.mobGroup.set(g);

            String[] parts = g.split("\\|");

            if (parts.length > 0)
            {
                mf.mobID.set(parts[0].trim());
            }
        }
        else
        {
            mf.mobID.set(target);
        }

        return mf;
    }

    private static ItemStack buildItem(String id)
    {
        if (id == null || id.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        try
        {
            return new ItemStack(BuiltInRegistries.ITEM.get(Identifier.parse(id)).orElseThrow().value());
        }
        catch (Throwable t)
        {
            /* keep empty stack if the id is invalid */
            return ItemStack.EMPTY;
        }
    }

    public static EquipmentSlot slotOf(String slot)
    {
        return switch (slot)
        {
            case "offHand" -> EquipmentSlot.OFFHAND;
            case "armorHead" -> EquipmentSlot.HEAD;
            case "armorChest" -> EquipmentSlot.CHEST;
            case "armorLegs" -> EquipmentSlot.LEGS;
            case "armorFeet" -> EquipmentSlot.FEET;
            default -> EquipmentSlot.MAINHAND;
        };
    }
}
