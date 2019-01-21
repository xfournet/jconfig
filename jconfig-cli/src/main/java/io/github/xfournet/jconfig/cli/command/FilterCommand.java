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

    @Parameter(description = "<expr1=value> [<expr2=value> ... <exprn=value>]", required = true)
    private List<String> m_expressionArgs = new ArrayList<>();

    @Override
    public void execute(CommandContext ctx) {
        Map<String, String> expressionMapping = new HashMap<>();
        for (String arg : m_expressionArgs) {
            int pos = arg.indexOf('=');
            if (pos != -1) {
                expressionMapping.put(arg.substring(0, pos), arg.substring(pos + 1));
            } else {
                expressionMapping.put(arg, "");
            }
        }

        ctx.getJConfig().filter(Paths.get(m_file), expressionMapping::get);
    }
}
