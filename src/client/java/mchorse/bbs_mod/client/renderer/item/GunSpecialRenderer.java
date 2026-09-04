package mchorse.bbs_mod.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.blaze3d.vertex.PoseStack;
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
 * MC 26.2 removed BuiltinItemRendererRegistry. Custom 3D items are now rendered
 * through the data-driven SpecialModelRenderer system: the item model JSON uses
 * {@code "type": "minecraft:special"} and points at the registered special type.
 * The actual 3D form is drawn by BBS's own GunItemRenderer, which renders via the
 * BBS form pipeline (the VertexConsumer param is unused there).
 */
public class GunSpecialRenderer implements SpecialModelRenderer<ItemStack>
{
    /** MapCodec used to register this special type in SpecialModelRenderers.ID_MAPPER. */
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
        // [MC 26.2] The display transform is already applied to poseStack by the caller
        // (vanilla item rendering). BBS applies its own gun transform on top, so we use
        // NONE as the model mode - the per-hand/zoom transforms are not available here.
        this.renderer.render(stack, ItemDisplayContext.NONE, poseStack, null, lightCoords, overlayCoords);

        // Keep the item renderer's cached entity alive between frames.
        this.renderer.update();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer)
    {
        // [MC 26.2] Extents are used by vanilla to compute the model's bounding volume
        // (e.g. for first-person / GUI layout). Report a generous item-space unit box
        // centered on the origin.
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
