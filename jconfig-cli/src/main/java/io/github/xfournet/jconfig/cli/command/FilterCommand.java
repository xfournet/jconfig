package io.github.xfournet.jconfig.cli.command;

import java.nio.file.Paths;
import java.util.*;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.cli.Command;
import io.github.xfournet.jconfig.cli.CommandContext;

@Parameters(commandNames = "filter", commandDescription = "Filter a file")
public class FilterCommand implements Command {

    @Parameter(names = {"--file", "-f"}, description = "File to be updated", required = true)
    private String m_file;

    @Parameter(description = "<var1=value> [<var2=value> ... <varn=value>]", required = true)
    private List<String> m_vars = new ArrayList<>();

    @Override
    public void execute(CommandContext ctx) {
        Map<String, String> vars = new HashMap<>();
        for (String var : m_vars) {
            int pos = var.indexOf('=');
            if (pos != -1) {
                vars.put(var.substring(0, pos), var.substring(pos + 1));
            } else {
                vars.put(var, "");
            }
        }

        ctx.getJConfig().filter(Paths.get(m_file), vars::get);
    }
}
