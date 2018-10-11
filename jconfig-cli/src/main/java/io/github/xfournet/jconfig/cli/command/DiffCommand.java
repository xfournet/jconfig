package io.github.xfournet.jconfig.cli.command;

import javax.annotation.*;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandExecutor;

@Parameters(commandNames = "diff", commandDescription = "Generate a diff configuration file by comparing two directory")
public class DiffCommand implements Command {

    @Parameter(names = {"--dir", "-d"}, description = "Directory result to be compared from the reference, default is current directory")
    @Nullable
    private String m_dir;

    @Parameter(names = {"--referenceDir", "-r"}, description = "Reference directory to be compared", required = true)
    private String m_referenceDir;

    @Parameter(names = {"--config", "-c"}, description = "Configuration file to be generated", required = true)
    private String m_confFile;

    @Override
    public void execute(CommandExecutor ctx) {
        ctx.diff(m_dir, m_referenceDir, m_confFile);
    }
}
