package io.github.xfournet.jconfig.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.*;
import javax.annotation.*;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import io.github.xfournet.jconfig.JConfig;
import io.github.xfournet.jconfig.cli.command.ApplyCommand;
import io.github.xfournet.jconfig.cli.command.DiffCommand;
import io.github.xfournet.jconfig.cli.command.HelpCommand;
import io.github.xfournet.jconfig.cli.command.MergeCommand;
import io.github.xfournet.jconfig.cli.command.NormalizeCommand;
import io.github.xfournet.jconfig.cli.command.RemoveCommand;
import io.github.xfournet.jconfig.cli.command.SetCommand;

public class JConfigCli {

    public static void main(String[] args) {
        if (!new JConfigCli("jconfig").run(args)) {
            System.exit(1);
        }
    }

    private final String m_programName;
    private final Path m_targetRoot;
    private final Predicate<String> m_diffPathFilter;

    public JConfigCli(String programName) {
        this(programName, Paths.get(""), s -> true);
    }

    public JConfigCli(String programName, Path targetRoot, Predicate<String> diffPathFilter) {
        m_programName = programName;
        m_targetRoot = targetRoot;
        m_diffPathFilter = diffPathFilter;
    }

    public boolean run(String[] args) {
        Map<String, Command> commandTable = new LinkedHashMap<>();

        addCommand(commandTable, new ApplyCommand());
        addCommand(commandTable, new DiffCommand());
        addCommand(commandTable, new MergeCommand());
        addCommand(commandTable, new RemoveCommand());
        addCommand(commandTable, new SetCommand());
        addCommand(commandTable, new NormalizeCommand());

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
            command.execute(new CommandExecutorImpl(jc, m_targetRoot));
        } catch (JConfigException e) {
            System.err.printf(m_programName + ": %s%n", e.getMessage());
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

    private final class CommandExecutorImpl implements CommandExecutor {
        private final JConfig m_jConfig = JConfig.newJConfig();
        private final JCommander m_jc;
        private final Path m_targetRoot;

        CommandExecutorImpl(JCommander jc, Path targetRoot) {
            m_jc = jc;
            m_targetRoot = targetRoot;
        }

        private Path resolveTargetDir(@Nullable String name) {
            return name != null ? m_targetRoot.resolve(name) : m_targetRoot;
        }

        @Override
        public void apply(@Nullable String dir, String confFile) {
            Path targetDir = resolveTargetDir(dir);
            m_jConfig.apply(targetDir, Paths.get(confFile));
        }

        @Override
        public void diff(@Nullable String dir, String referenceDir, String confFile) {
            Path targetFile = resolveTargetDir(dir);
            m_jConfig.diff(targetFile, Paths.get(referenceDir), m_diffPathFilter, Paths.get(confFile));
        }

        @Override
        public void merge(@Nullable String dir, String file, String sourceFile) {
            Path targetFile = resolveTargetDir(dir).resolve(file);
            m_jConfig.mergeFiles(targetFile, Paths.get(file), targetFile);
        }

        @Override
        public void remove(@Nullable String dir, String file, List<String> entries) {
            Path targetFile = resolveTargetDir(dir).resolve(file);
            m_jConfig.removeEntries(targetFile, entries);
        }

        @Override
        public void set(@Nullable String dir, String file, List<String> entries) {
            Path targetFile = resolveTargetDir(dir).resolve(file);
            m_jConfig.setEntries(targetFile, entries);
        }

        @Override
        public void normalize(@Nullable String dir, String file) {
            Path targetFile = resolveTargetDir(dir).resolve(file);
            m_jConfig.normalize(targetFile);
        }

        @Override
        public void printHelp() {
            m_jc.usage();
        }
    }
}
