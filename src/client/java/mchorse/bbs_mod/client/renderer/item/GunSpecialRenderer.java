package mchorse.bbs_mod.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * SpecialModelRenderer for the BBS gun item (GUN_ITEM).
 *
 * See ModelBlockSpecialRenderer for the MC 26.2 rationale. The 3D gun form is
 * drawn by BBS's own GunItemRenderer (NBT-driven, rendered through the BBS
 * CustomVertexConsumer pipeline).
 */
public class GunSpecialRenderer implements SpecialModelRenderer<ItemStack>
{
    public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

    private final GunItemRenderer renderer = new GunItemRenderer();

    @Override
    public ItemStack extractArgument(ItemStack stack)
    {
        return stack;
    }

    @Override
    public void submit(ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor)
    {
        // [MC 26.2] First-person zoom form is only selected for FIRST_PERSON_* modes,
        // which are not passed to submit(); NONE is used here (zoom is an editor feature).
        this.renderer.render(stack, ItemDisplayContext.NONE, poseStack, null, lightCoords, overlayCoords);

        this.renderer.update();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer)
    {
        // [MC 26.2] Report a generous item-space unit box; BBS gun forms are user-defined.
        consumer.accept(new Vector3f(-0.5F, 0F, -0.5F));
        consumer.accept(new Vector3f(0.5F, 0F, -0.5F));
        consumer.accept(new Vector3f(-0.5F, 0F, 0.5F));
        consumer.accept(new Vector3f(0.5F, 0F, 0.5F));
        consumer.accept(new Vector3f(-0.5F, 1F, -0.5F));
        consumer.accept(new Vector3f(0.5F, 1F, -0.5F));
        consumer.accept(new Vector3f(-0.5F, 1F, 0.5F));
        consumer.accept(new Vector3f(0.5F, 1F, 0.5F));
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked<ItemStack>
    {
        @Override
        public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.BakingContext context)
        {
            return new GunSpecialRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<ItemStack>> type()
        {
            return MAP_CODEC;
        }
    }
}
