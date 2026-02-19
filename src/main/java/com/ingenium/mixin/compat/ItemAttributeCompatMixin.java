package com.ingenium.mixin.compat;

import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Compatibility shim for Fabric Item API v1 redirect on 1.20.1.
 * Some runtime combinations expect Item#getAttributeModifiers(ItemStack, EquipmentSlot)
 * which vanilla 1.20.1 does not define. We provide a non-invasive overload that
 * delegates to the vanilla single-arg implementation to preserve behavior while
 * avoiding NoSuchMethodError.
 */
@Mixin(value = Item.class, priority = 500)
public abstract class ItemAttributeCompatMixin {
    /**
     * Compatibility shim for Fabric Item API v1.
     * Provides the expected two-arg method signature that some runtime environments miss.
     */
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(
            ItemStack stack, EquipmentSlot slot) {
        // Delegate to the vanilla one-arg method to avoid recursion through ItemStack
        return ((Item)(Object)this).getAttributeModifiers(slot);
    }
}
