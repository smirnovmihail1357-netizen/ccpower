package ru.wardreg.ccpower.debug;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Registers the admin-only {@code /ccpower debug} command. */
public final class CCPowerCommands {
    private CCPowerCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("ccpower")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                    .executes(CCPowerCommands::toggleDebug)));
    }

    private static int toggleDebug(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = DebugState.toggle(player.getUUID());
        player.sendMessage(new TextComponent(
            "[CC: Power] Jade diagnostics " + (enabled ? "enabled" : "disabled")),
            player.getUUID());
        return 1;
    }
}
