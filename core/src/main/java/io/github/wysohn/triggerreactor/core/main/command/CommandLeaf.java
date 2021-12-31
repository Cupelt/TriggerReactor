package io.github.wysohn.triggerreactor.core.main.command;

import io.github.wysohn.triggerreactor.core.bridge.ICommandSender;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class CommandLeaf implements ITriggerCommand {
    final String[] commands;
    final Usage usage;
    final BiFunction<ICommandSender, Queue<String>, Boolean> argsFn;

    public CommandLeaf(String command, Usage usage, BiFunction<ICommandSender, Queue<String>, Boolean> argsFn) {
        this(new String[]{command}, usage, argsFn);
    }

    public CommandLeaf(String[] commands, Usage usage, BiFunction<ICommandSender, Queue<String>, Boolean> argsFn) {
        this.commands = commands;
        this.usage = usage;
        this.argsFn = argsFn;

        Arrays.sort(commands);
    }

    @Override
    public boolean onCommand(ICommandSender sender, Queue<String> args) {
        if(args.isEmpty() || Arrays.binarySearch(this.commands, args.peek()) < 0)
            return false;

        args.poll();

        return argsFn.apply(sender, args);
    }

    @Override
    public List<String> onTab(ListIterator<String> args) {
        String current = args.next();

        List<String> out;
        // don't return commands until only one argument left
        if (args.hasNext()) {
            out = new LinkedList<>();
        } else {
            out = Arrays.stream(commands)
                    .filter(c -> c.startsWith(current))
                    .collect(Collectors.toCollection(LinkedList::new));
        }

        args.previous();
        return out;
    }

    @Override
    public void printUsage(ICommandSender sender, int depth) {
        usage.printUsage(sender, INDENT * Math.max(0, 2 - depth));
    }
}