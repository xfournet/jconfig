package io.github.xfournet.jconfig.cli.command;

import javax.annotation.*;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandExecutor;

@Parameters(commandNames = "normalize", commandDescription = "Normalize a configuration file")
public class NormalizeCommand implements Command {

    @Parameter(names = {"--dir", "-d"}, description = "Directory to be updated, default is current directory")
    @Nullable
    private String m_dir;

    @Parameter(names = {"--file", "-f"}, description = "Configuration file to be normalized", required = true)
    private String m_file;

    @Override
    public void execute(CommandExecutor ctx) {
        ctx.normalize(m_dir, m_file);
    }
}
