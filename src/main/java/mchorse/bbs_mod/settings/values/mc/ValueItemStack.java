package mchorse.bbs_mod.settings.values.mc;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.world.item.ItemStack;

public class ValueItemStack extends BaseValueBasic<ItemStack>
{
    public ValueItemStack(String id)
    {
        super(id, ItemStack.EMPTY);
    }

    /* MC 26.2: some code paths set the value to null (e.g. forms built from
     * data without an item component). Returning EMPTY instead of null keeps
     * every consumer (ItemStack.CODEC encoding, getDefaultDisplayName, ...)
     * safe without crashing the morph/send pipeline. */
    @Override
    public ItemStack get()
    {
        ItemStack stack = super.get();

        return stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public BaseType toData()
    {
        return KeyframeFactories.ITEM_STACK.toData(this.get());
    }

    @Override
    public void fromData(BaseType data)
    {
        this.set(KeyframeFactories.ITEM_STACK.fromData(data));
    }
}