package io.github.xfournet.jconfig.cli.command;

import java.nio.file.Paths;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandContext;

@Parameters(commandNames = "apply", commandDescription = "Apply configuration file to a directory")
public class ApplyCommand implements Command {

    @Parameter(names = {"--config", "-c"}, description = "Configuration file to be applied", required = true)
    private String m_confFile;

    @Override
    public void execute(CommandContext ctx) {
        ctx.getJConfig().apply(Paths.get(m_confFile));
    }
}
