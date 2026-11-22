package ru.wardreg.ccpower.debug;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/** Registers the admin-only {@code /ccpower debug} command. */
public final class CCPowerCommands {
    private CCPowerCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("ccpower")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.literal("debug")
                        .executes(CCPowerCommands::toggleDebug))));
    }

    private static int toggleDebug(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = DebugState.toggle(player.getUUID());
        player.sendSystemMessage(Component.literal(
            "[CC: Power] Jade diagnostics " + (enabled ? "enabled" : "disabled")));
        return 1;
    }
}
