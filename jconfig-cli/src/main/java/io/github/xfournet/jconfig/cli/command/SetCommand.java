package io.github.xfournet.jconfig.cli.command;

import java.nio.file.Paths;
import java.util.*;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandContext;

@Parameters(commandNames = "set", commandDescription = "Set the value of one or many entries in a configuration file")
public class SetCommand implements Command {

    @Parameter(names = {"--file", "-f"}, description = "Configuration file to be updated", required = true)
    private String m_file;

    @Parameter(description = "<entry 1> [<entry 2> ... <entry n>]", required = true)
    private List<String> m_entries = new ArrayList<>();

    @Override
    public void execute(CommandContext ctx) {
        ctx.getJConfig().setEntries(Paths.get(m_file), m_entries);
    }
}
