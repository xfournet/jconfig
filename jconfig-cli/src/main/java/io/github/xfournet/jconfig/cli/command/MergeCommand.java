package io.github.xfournet.jconfig.cli.command;

import javax.annotation.*;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandExecutor;

@Parameters(commandNames = "merge", commandDescription = "Merge a configuration file into another one file")
public class MergeCommand implements Command {

    @Parameter(names = {"--dir", "-d"}, description = "Directory to be updated, default is current directory")
    @Nullable
    private String m_dir;

    @Parameter(names = {"--file", "-f"}, description = "Configuration file to be updated", required = true)
    private String m_file;

    @Parameter(names = {"--sourceFile", "-s"}, description = "Configuration file to be merged", required = true)
    private String m_sourceFile;

    @Override
    public void execute(CommandExecutor ctx) {
        ctx.merge(m_dir, m_file, m_sourceFile);
    }
}
