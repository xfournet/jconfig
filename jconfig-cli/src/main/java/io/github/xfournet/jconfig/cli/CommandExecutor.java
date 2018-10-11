package io.github.xfournet.jconfig.cli;

import java.util.*;
import javax.annotation.*;

public interface CommandExecutor {
    void apply(@Nullable String dir, String confFile);

    void diff(@Nullable String dir, String referenceDir, String confFile);

    void merge(@Nullable String dir, String file, String sourceFile);

    void remove(@Nullable String dir, String file, List<String> entries);

    void set(@Nullable String dir, String file, List<String> entries);

    void normalize(@Nullable String dir, String file);

    void printHelp();
}
