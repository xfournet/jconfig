package io.github.xfournet.jconfig.cli.command;

import javax.annotation.*;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandExecutor;

@Parameters(commandNames = "apply", commandDescription = "Apply configuration file to a directory")
public class ApplyCommand implements Command {

    @Parameter(names = {"--dir", "-d"}, description = "Directory to be updated, default is current directory")
    @Nullable
    private String m_dir;

    @Parameter(names = {"--config", "-c"}, description = "Configuration file to be applied", required = true)
    private String m_confFile;

    @Override
    public void execute(CommandExecutor ctx) {
        ctx.apply(m_dir, m_confFile);
    }
}
