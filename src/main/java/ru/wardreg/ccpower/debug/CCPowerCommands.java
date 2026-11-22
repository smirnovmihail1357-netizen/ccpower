package ru.wardreg.ccpower.debug;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

/** Registers the admin-only {@code /ccpower debug} command. */
public final class CCPowerCommands {
    private CCPowerCommands() {}

    // Forge 1.15.2 has no RegisterCommandsEvent: commands are registered on
    // FMLServerStartingEvent with a CommandDispatcher<CommandSource>.
    public static void register(FMLServerStartingEvent event) {
        event.getCommandDispatcher().register(
            Commands.literal("ccpower")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                    .executes(CCPowerCommands::toggleDebug)));
    }

    private static int toggleDebug(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        boolean enabled = DebugState.toggle(player.getUUID());
        player.sendMessage(new StringTextComponent(
            "[CC: Power] diagnostics " + (enabled ? "enabled" : "disabled")));
        return 1;
    }
}
