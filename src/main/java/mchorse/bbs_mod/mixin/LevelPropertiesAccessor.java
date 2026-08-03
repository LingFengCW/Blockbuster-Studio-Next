package mchorse.bbs_mod.mixin;

import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PrimaryLevelData.class)
public interface LevelPropertiesAccessor
{
    /* Mojmap field name is "settings" ("levelInfo" was the old Yarn name). Verified against
       the 26.2 common jar constant pool: field `settings` of type LevelSettings still exists. */
    @Accessor("settings")
    public void bbs$setLevelInfo(LevelSettings info);
}