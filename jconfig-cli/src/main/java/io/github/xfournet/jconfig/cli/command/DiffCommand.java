package io.github.xfournet.jconfig.cli.command;

import java.nio.file.Paths;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandContext;

@Parameters(commandNames = "diff", commandDescription = "Generate a diff configuration file by comparing two directory")
public class DiffCommand implements Command {

    @Parameter(names = {"--referenceDir", "-r"}, description = "Reference directory to be compared", required = true)
    private String m_referenceDir;

    @Parameter(names = {"--config", "-c"}, description = "Configuration file to be generated", required = true)
    private String m_confFile;

    @Override
    public void execute(CommandContext ctx) {
        ctx.getJConfig().diff(Paths.get(m_referenceDir), Paths.get(m_confFile));
    }
}
