package io.github.xfournet.jconfig.cli.command;

import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandContext;

@Parameters(commandNames = "help", commandDescription = "Display help")
public class HelpCommand implements Command {

    @Override
    public void execute(CommandContext ctx) {
        ctx.getJCommander().usage();
    }
}
