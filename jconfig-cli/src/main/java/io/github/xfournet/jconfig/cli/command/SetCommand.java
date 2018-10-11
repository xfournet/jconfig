package io.github.xfournet.jconfig.cli.command;

import java.util.*;
import javax.annotation.*;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandExecutor;

@Parameters(commandNames = "set", commandDescription = "Set the value of one or many entries in a configuration file")
public class SetCommand implements Command {

    @Parameter(names = {"--dir", "-d"}, description = "Directory to be updated, default is current directory")
    @Nullable
    private String m_dir;

    @Parameter(names = {"--file", "-f"}, description = "Configuration file to be updated", required = true)
    private String m_file;

    @Parameter(description = "<entry 1> [<entry 2> ... <entry n>]", required = true)
    private List<String> m_entries = new ArrayList<>();

    @Override
    public void execute(CommandExecutor ctx) {
        ctx.set(m_dir, m_file, m_entries);
    }
}
