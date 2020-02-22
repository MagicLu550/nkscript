package net.noyark.www.nkscript.core

import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender;

class CommandInfo {

    CommandSender sender

    Command command

    String label

    String[] args

    CommandSender getSender() {
        return sender
    }

    void setSender(CommandSender sender) {
        this.sender = sender
    }

    Command getCommand() {
        return command
    }

    void setCommand(Command command) {
        this.command = command
    }

    String getLabel() {
        return label
    }

    void setLabel(String label) {
        this.label = label
    }

    String[] getArgs() {
        return args
    }

    void setArgs(String[] args) {
        this.args = args
    }
}
