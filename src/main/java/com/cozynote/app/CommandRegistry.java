package com.cozynote.app;

import java.util.EnumMap;
import java.util.Map;

public final class CommandRegistry {
    private final Map<AppCommand, Runnable> commands = new EnumMap<>(AppCommand.class);

    public void register(AppCommand command, Runnable action) {
        commands.put(command, action);
    }

    public void execute(AppCommand command) {
        Runnable action = commands.get(command);
        if (action == null) {
            throw new IllegalStateException("Command not registered: " + command);
        }
        action.run();
    }
}
