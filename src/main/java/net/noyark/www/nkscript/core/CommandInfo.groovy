package net.noyark.www.nkscript.core

import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender
import groovy.transform.CompileStatic

@CompileStatic
class CommandInfo {

    CommandSender sender

    Command command

    String label

    String[] args

    CommandSender getSender() { sender }

    void setSender(CommandSender sender) {
        this.sender = sender
    }

    Command getCommand() { command }

    void setCommand(Command command) {
        this.command = command
    }

    String getLabel() { label }

    void setLabel(String label) {
        this.label = label
    }

    String[] getArgs() { args }

    void setArgs(String[] args) {
        this.args = args
    }
}
