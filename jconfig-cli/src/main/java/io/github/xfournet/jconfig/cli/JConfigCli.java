package io.github.xfournet.jconfig.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.*;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.JConfig;
import io.github.xfournet.jconfig.cli.command.ApplyCommand;
import io.github.xfournet.jconfig.cli.command.DiffCommand;
import io.github.xfournet.jconfig.cli.command.HelpCommand;
import io.github.xfournet.jconfig.cli.command.MergeCommand;
import io.github.xfournet.jconfig.cli.command.RemoveCommand;
import io.github.xfournet.jconfig.cli.command.SetCommand;

public class JConfigCli {

    public static void main(String[] args) {
        if (!new JConfigCli("jconfig", defaultCommands(), Paths.get(""), path -> true).run(args)) {
            System.exit(1);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static List<Command> defaultCommands() {
        return Arrays.asList(new ApplyCommand(), new DiffCommand(), new MergeCommand(), new RemoveCommand(), new SetCommand());
    }

    private final String m_programName;
    private final List<Command> m_commands;
    private final Path m_targetDir;
    private final Predicate<Path> m_diffPathFilter;

    public JConfigCli(String programName, List<Command> commands, Path targetDir, Predicate<Path> diffPathFilter) {
        m_programName = programName;
        m_commands = commands;
        m_targetDir = targetDir;
        m_diffPathFilter = diffPathFilter;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean run(String[] args) {
        Map<String, Command> commandTable = new LinkedHashMap<>();

        for (Command command : m_commands) {
            addCommand(commandTable, command);
        }

        HelpCommand helpCommand = new HelpCommand();
        addCommand(commandTable, helpCommand);

        JCommander jc = new JCommander();
        jc.setCaseSensitiveOptions(false);
        jc.setColumnSize(160);
        jc.setProgramName(m_programName);
        commandTable.values().forEach(jc::addCommand);

        String error = null;
        try {
            jc.parse(args);
        } catch (ParameterException e) {
            error = e.getMessage();
        }

        String parsedCommand = jc.getParsedCommand();
        if (parsedCommand != null) {
            parsedCommand = parsedCommand.toLowerCase(Locale.ROOT);
        }

        Command command = commandTable.get(parsedCommand);
        if (command == null) {
            command = helpCommand;
        }

        if (error != null) {
            System.err.printf("Error: %s%n", error);
            if (command != helpCommand) {
                return false;
            }
        }

        try {
            command.execute(new CommandContextImpl(jc, JConfig.newDefaultJConfig(m_targetDir, m_diffPathFilter)));
        } catch (JConfigException e) {
            System.err.printf("%s: %s%n", m_programName, e.getMessage());
            return false;
        }

        return error == null;
    }

    private void addCommand(Map<String, Command> commandTable, Command command) {
        Parameters parametersAnnotation = command.getClass().getAnnotation(Parameters.class);
        if (parametersAnnotation == null) {
            throw new IllegalArgumentException("Command has no @Parameters annotation: " + command);
        }

        for (String commandName : parametersAnnotation.commandNames()) {
            commandName = commandName.toLowerCase(Locale.ROOT);
            Command existingCommand = commandTable.put(commandName, command);
            if (existingCommand != null) {
                throw new IllegalStateException("Two commands are registered with same name: " + commandName);
            }
        }
    }

    private final class CommandContextImpl implements CommandContext {
        private final JCommander m_jCommander;
        private final JConfig m_jConfig;

        CommandContextImpl(JCommander jCommander, JConfig jConfig) {
            m_jCommander = jCommander;
            m_jConfig = jConfig;
        }

        @Override
        public JCommander getJCommander() {
            return m_jCommander;
        }

        @Override
        public JConfig getJConfig() {
            return m_jConfig;
        }
    }
}
