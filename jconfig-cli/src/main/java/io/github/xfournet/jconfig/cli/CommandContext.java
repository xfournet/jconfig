package io.github.xfournet.jconfig.cli;

import com.beust.jcommander.JCommander;
import io.github.xfournet.jconfig.JConfig;

public interface CommandContext {
    JCommander getJCommander();

    JConfig getJConfig();
}
