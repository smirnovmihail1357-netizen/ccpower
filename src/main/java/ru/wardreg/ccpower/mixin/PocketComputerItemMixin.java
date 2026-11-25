package ru.wardreg.ccpower.mixin;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.pocket.core.PocketHolder;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.util.NonNegativeId;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.wardreg.ccpower.CCPowerConfig;
import ru.wardreg.ccpower.power.ComputerPowerState;
import ru.wardreg.ccpower.power.DeviceKind;
import ru.wardreg.ccpower.power.PowerManager;

/**
 * Binds a pocket computer's RF buffer to its ItemStack. The server keeps the
 * authoritative value in the stack's data component (so it survives reloads
 * and travels with the item); the client reads the very same component to
 * draw the durability-style bar.
 */
@Mixin(value = PocketComputerItem.class, remap = false)
public abstract class PocketComputerItemMixin {
    private static net.minecraft.core.component.DataComponentType<Long> rf() {
        return ru.wardreg.ccpower.registry.ModRegistry.POCKET_RF.get();
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void ccpower$bindPocketRf(ItemStack stack, PocketHolder holder,
                                      boolean changed, CallbackInfo ci) {
        ServerLevel level = holder.level();
        if (level == null || level.isClientSide()) return;

        NonNegativeId id = stack.get(ModRegistry.DataComponents.COMPUTER_ID.get());
        if (id == null) return;

        ComputerPowerState state = PowerManager.state(id.id());
        Long stored = stack.get(rf());
        state.hydratePortable(stored == null ? Long.MAX_VALUE : stored, DeviceKind.POCKET);
        state.bindPocketStack(stack);
        state.syncPortableToStack();
    }

    /** RF is shown as a durability bar whenever the stack carries our component. */
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(rf());
    }

    public int getBarWidth(ItemStack stack) {
        Long stored = stack.get(rf());
        long capacity = CCPowerConfig.pocketBufferRF();
        if (stored == null || capacity <= 0) return 0;
        long value = Math.max(0, Math.min(stored, capacity));
        return Math.max(1, (int) Math.round(13.0 * value / capacity));
    }

    public int getBarColor(ItemStack stack) {
        Long stored = stack.get(rf());
        long capacity = CCPowerConfig.pocketBufferRF();
        double ratio = stored == null || capacity <= 0
            ? 0.0 : Math.min(stored, capacity) / (double) capacity;
        if (ratio > 0.5) return 0x00D63E;
        if (ratio > 0.2) return 0xE8C600;
        return 0xD63E00;
    }
}
