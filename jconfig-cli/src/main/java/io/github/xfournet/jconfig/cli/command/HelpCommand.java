package io.github.xfournet.jconfig.cli.command;

import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandExecutor;

@Parameters(commandNames = "help", commandDescription = "Display help")
public class HelpCommand implements Command {

    @Override
    public void execute(CommandExecutor ctx) {
        ctx.printHelp();
    }
}
