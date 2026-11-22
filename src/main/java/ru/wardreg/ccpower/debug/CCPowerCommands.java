package ru.wardreg.ccpower.debug;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Registers the admin-only {@code /ccpower debug} command (Forge 1.16.5). */
public final class CCPowerCommands {
    private CCPowerCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("ccpower")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                    .executes(CCPowerCommands::toggleDebug)));
    }

    private static int toggleDebug(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        boolean enabled = DebugState.toggle(player.getGameProfile().getId());
        context.getSource().sendSuccess(new StringTextComponent(
            "[CC: Power] Jade diagnostics " + (enabled ? "enabled" : "disabled")), false);
        return 1;
    }
}
