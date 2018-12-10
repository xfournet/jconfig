package io.github.xfournet.jconfig.cli.command;

import java.nio.file.Paths;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandContext;

@Parameters(commandNames = "merge", commandDescription = "Merge a configuration file or directory into target directory or to a specific file")
public class MergeCommand implements Command {

    @Parameter(names = {"--path", "-p"}, description = "Configuration directory or file to be merged", required = true)
    private String m_sourceFile;

    @Parameter(names = {"--file", "-f"}, description = "Configuration file to be updated")
    private String m_file;

    @Override
    public void execute(CommandContext ctx) {
        if (m_file != null) {
            ctx.getJConfig().merge(Paths.get(m_file), Paths.get(m_sourceFile));
        } else {
            ctx.getJConfig().merge(Paths.get(m_sourceFile));
        }
    }
}
