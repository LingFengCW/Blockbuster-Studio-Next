package mchorse.bbs_mod.cubic.render.vanilla;

import com.google.common.collect.Maps;
import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class ArmorRenderer
{
    private static final Map<String, Identifier> ARMOR_TEXTURE_CACHE = Maps.newHashMap();

    private final HumanoidModel innerModel;
    private final HumanoidModel outerModel;
    private boolean slim;

    public ArmorRenderer(EntityModelSet modelSet, boolean slim)
    {
        /* MC 26.2: ModelManager.getModel(ModelLayerLocation) was removed. Entity models
           (including the player body) are baked via EntityModelSet.bakeLayer(ModelLayerLocation).
           We bake the real player body model as the armor geometry root. This restores a
           real (non-empty) HumanoidModel and avoids the NoSuchElementException that an empty
           ModelPart would trigger in HumanoidModel's constructor. */
        ModelLayerLocation layer = slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER;

        this.innerModel = new HumanoidModel(modelSet.bakeLayer(layer));
        this.outerModel = new HumanoidModel(modelSet.bakeLayer(layer));
        this.slim = slim;
    }

    public void renderArmorSlot(PoseStack matrices, VertexConsumer vertexConsumers, IEntity entity, EquipmentSlot armorSlot, ArmorType type, int light)
    {
        renderArmorParts(getModel(armorSlot).root(), matrices, vertexConsumers, light, armorSlot, type, 1F, 1F, 1F);
    }

    private void renderArmorParts(ModelPart part, PoseStack matrices, VertexConsumer vertexConsumers, int light, EquipmentSlot slot, ArmorType type, float red, float green, float blue)
    {
        int color = 0xFF000000 | ((int) (red * 255) << 16) | ((int) (green * 255) << 8) | (int) (blue * 255);
        part.render(matrices, vertexConsumers, light, 0, color);
    }

    private HumanoidModel getModel(EquipmentSlot slot)
    {
        return slot == EquipmentSlot.LEGS ? this.innerModel : this.outerModel;
    }

    private boolean usesInnerModel(EquipmentSlot slot)
    {
        return slot == EquipmentSlot.LEGS;
    }

    private Identifier getArmorTexture(EquipmentSlot slot, boolean secondLayer, String overlay)
    {
        String materialName = slot.getName();
        String id = "textures/models/armor/" + materialName + "_layer_" + (secondLayer ? 2 : 1) + (overlay == null ? "" : "_" + overlay) + ".png";
        return ARMOR_TEXTURE_CACHE.computeIfAbsent(id, Identifier::parse);
    }
}
