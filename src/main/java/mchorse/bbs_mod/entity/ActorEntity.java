package mchorse.bbs_mod.entity;

import com.mojang.serialization.Codec;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.network.ServerNetwork;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActorEntity extends LivingEntity implements IEntityFormProvider, Leashable
{
    public static AttributeSupplier.Builder createActorAttributes()
    {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.ATTACK_DAMAGE, 1D)
            .add(Attributes.MOVEMENT_SPEED, 0.1D)
            .add(Attributes.ATTACK_SPEED)
            .add(Attributes.LUCK);
    }

    private boolean despawn;
    private MCEntity entity = new MCEntity(this);
    private Form form;
    private Leashable.LeashData leashData;
    private String leashBone = "";
    private Vec3 leashOffset = Vec3.ZERO;
    private int leashHolderId = -1;       // resolved holder entity id (set at spawn)
    private int leashStartTick = -1;      // timeline clip window; -1 = always
    private int leashEndTick = -1;

    private Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();

    public ActorEntity(EntityType<? extends LivingEntity> entityType, Level world)
    {
        super(entityType, world);
    }

    public MCEntity getEntity()
    {
        return this.entity;
    }

    @Override
    public int getEntityId()
    {
        return this.getId();
    }

    @Override
    public Form getForm()
    {
        return this.form;
    }

    @Override
    public void setForm(Form form)
    {
        Form lastForm = this.form;

        this.form = form;

        if (!this.level().isClientSide())
        {
            if (lastForm != null) lastForm.onDemorph(this);
            if (form != null) form.onMorph(this);
        }
    }

    @Override
    public Leashable.LeashData getLeashData()
    {
        return this.leashData;
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData leashData)
    {
        this.leashData = leashData;
    }

    public String getLeashBone()
    {
        return this.leashBone;
    }

    public void setLeashBone(String bone)
    {
        this.leashBone = bone == null ? "" : bone;
    }

    public void setLeashOffset(Vec3 offset)
    {
        this.leashOffset = offset == null ? Vec3.ZERO : offset;
    }

    public int getLeashHolderId()
    {
        return this.leashHolderId;
    }

    public void setLeashHolderId(int id)
    {
        this.leashHolderId = id;
    }

    public void setLeashWindow(int startTick, int endTick)
    {
        this.leashStartTick = startTick;
        this.leashEndTick = endTick;
    }

    public boolean isLeashWindowed()
    {
        return this.leashStartTick >= 0 && this.leashEndTick >= 0;
    }

    public boolean isInLeashWindow(int tick)
    {
        if (!isLeashWindowed())
        {
            return true;
        }

        int lo = Math.min(leashStartTick, leashEndTick);
        int hi = Math.max(leashStartTick, leashEndTick);

        return tick >= lo && tick <= hi;
    }

    @Override
    public boolean canBeLeashed()
    {
        return true;
    }

    @Override
    public boolean mayBeLeashed()
    {
        return true;
    }

    @Override
    public Vec3 getLeashOffset(float partialTicks)
    {
        if (this.leashBone.isEmpty())
        {
            return Leashable.super.getLeashOffset(partialTicks);
        }

        return this.position().add(this.leashOffset);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance)
    {
        double d = this.getBoundingBox().getSize();

        if (Double.isNaN(d))
        {
            d = 1D;
        }

        return distance < (d * 256D) * (d * 256D);
    }

    public Iterable<ItemStack> getHandSlots()
    {
        return List.of(this.getItemBySlot(EquipmentSlot.MAINHAND), this.getItemBySlot(EquipmentSlot.OFFHAND));
    }

    public Iterable<ItemStack> getArmorSlots()
    {
        return List.of(this.getItemBySlot(EquipmentSlot.FEET), this.getItemBySlot(EquipmentSlot.LEGS), this.getItemBySlot(EquipmentSlot.CHEST), this.getItemBySlot(EquipmentSlot.HEAD));
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot)
    {
        return this.equipment.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack)
    {
        this.equipment.put(slot, stack == null ? ItemStack.EMPTY : stack);
    }

    @Override
    public HumanoidArm getMainArm()
    {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void tick()
    {
        super.tick();

        this.updateSwingTime();

        if (this.form != null)
        {
            this.form.update(this.entity);
        }

        if (this.leashData != null && this.level() instanceof ServerLevel serverLevel)
        {
            Leashable.tickLeash(serverLevel, this);
        }

        if (this.level().isClientSide())
        {
            return;
        }

        /* Pickup items */
        AABB box = this.getBoundingBox().inflate(1D, 0.5D, 1D);
        List<Entity> list = this.level().getEntities(this, box);

        for (Entity entity : list)
        {
            if (entity instanceof ItemEntity itemEntity)
            {
                ItemStack itemStack = itemEntity.getItem();
                int i = itemStack.getCount();

                if (!entity.isRemoved() && !itemEntity.hasPickUpDelay())
                {
                    ((ServerLevel) this.level()).getChunkSource().sendToTrackingPlayers(entity, new ClientboundTakeItemEntityPacket(entity.getId(), this.getId(), i));
                    entity.discard();
                }
            }
        }
    }

    @Override
    public void checkDespawn()
    {
        super.checkDespawn();

        if (this.despawn)
        {
            this.discard();
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player)
    {
        super.startSeenByPlayer(player);

        ServerNetwork.sendEntityForm(player, this);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input)
    {
        super.readAdditionalSaveData(input);
        this.despawn = input.read("despawn", Codec.BOOL).orElse(false);
        this.readLeashData(input);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output)
    {
        super.addAdditionalSaveData(output);
        output.store("despawn", Codec.BOOL, this.despawn);
        this.writeLeashData(output, this.leashData);
    }

    protected int getPermissionLevel()
    {
        return 4;
    }
}
