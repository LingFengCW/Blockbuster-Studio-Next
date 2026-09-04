package lingfeng.bbsnext.film.replays;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public class PotionClip extends ValueGroup
{
    public static final String TYPE_SPEED = "minecraft:speed";
    public static final String TYPE_JUMP = "minecraft:jump_boost";
    public static final String TYPE_STRENGTH = "minecraft:strength";
    public static final String TYPE_INVIS = "minecraft:invisibility";
    public static final String TYPE_FIRE = "minecraft:fire_resistance";

    public final ValueString type = new ValueString("type", TYPE_SPEED);
    public final ValueString effect = new ValueString("effect", "速度");
    public final ValueString name = new ValueString("name", "药水");
    public final ValueInt tick = new ValueInt("tick", 0);
    public final ValueInt duration = new ValueInt("duration", 30);
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);

    public PotionClip(String id)
    {
        super(id);

        this.add(this.type);
        this.add(this.effect);
        this.add(this.name);
        this.add(this.tick);
        this.add(this.duration);
        this.add(this.enabled);
    }

    public boolean isActive(int tick)
    {
        return this.enabled.get() && tick >= this.tick.get() && tick < this.tick.get() + this.duration.get();
    }
}
