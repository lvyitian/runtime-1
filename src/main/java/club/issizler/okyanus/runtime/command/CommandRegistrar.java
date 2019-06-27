package club.issizler.okyanus.runtime.command;

import club.issizler.okyanus.api.cmd.ArgumentType;
import club.issizler.okyanus.api.cmd.CommandBuilder;
import club.issizler.okyanus.api.cmd.CommandSource;
import club.issizler.okyanus.runtime.SomeGlobals;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public class CommandRegistrar {

    private static Logger LOGGER = LogManager.getLogger();

    public static void register() {
        LOGGER.info("Okyanus: Late command registration");

        for (CommandBuilder command : club.issizler.okyanus.api.cmd.CommandManager.INSTANCE.__internal_getCommands()) {
            LOGGER.debug("Okyanus: Creating brigadier command for " + command.__internal_name());

            LiteralArgumentBuilder<ServerCommandSource> builder = literal(command.__internal_name());
            ArgumentBuilder argumentBuilder = null;

            List<Triple<String, ArgumentType, Boolean>> args = command.__internal_args();
            Command<ServerCommandSource> cmd = context -> command.__internal_runnable().run(new CommandSource(context));

            boolean wasPreviousOptional = false;

            if (command.__internal_isOpOnly()) {
                LOGGER.debug("  - Marked as OP only");
                builder = builder.requires(source -> source.hasPermissionLevel(3));
            }

            Collections.reverse(args);
            for (Triple<String, ArgumentType, Boolean> arg : args) {
                com.mojang.brigadier.arguments.ArgumentType type;

                switch (arg.getMiddle()) {
                    case PLAYER:
                        type = EntityArgumentType.players();
                        break;
                    case TEXT:
                    default:
                        type = StringArgumentType.string();
                        break;
                }

                LOGGER.debug("  - Set up argument " + arg.getLeft() + " typeExample=" + type.getExamples().toArray()[0] + " optional=" + arg.getRight());

                ArgumentBuilder argument = CommandManager.argument(arg.getLeft(), type);

                if (argumentBuilder == null) {
                    argumentBuilder = argument.executes(cmd);
                } else {
                    if (wasPreviousOptional)
                        argument = argument.executes(cmd);

                    argumentBuilder = argument.then(argumentBuilder);
                }

                wasPreviousOptional = arg.getRight();
            }

            if (argumentBuilder != null) {
                builder = builder.then(argumentBuilder);

                if (wasPreviousOptional)
                    builder = builder.executes(cmd);
            } else {
                builder = builder.executes(cmd);
            }

            LOGGER.debug("  - Register the command");

            CommandNode<ServerCommandSource> vanillaCommand = SomeGlobals.commandDispatcher.getRoot().getChild(command.__internal_name());
            if (vanillaCommand != null) {
                LOGGER.info("Okyanus: Overwriting a vanilla command (/" + command.__internal_name() + "). Just letting you know");
                SomeGlobals.commandDispatcher.getRoot().getChildren().remove(vanillaCommand);
            }

            SomeGlobals.commandDispatcher.register(builder);
        }
    }

}
