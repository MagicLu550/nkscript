package net.noyark.www.nkscript.core

import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender
import cn.nukkit.plugin.PluginBase

abstract class NKScriptPluginBase extends PluginBase {

    private ScriptInfo info

    private File scriptFile

    ScriptInfo getInfo() {
        return info
    }

    void setInfo(ScriptInfo info) {
        this.info = info
    }

    File getScriptFile() {
        return scriptFile
    }

    void setScriptFile(File scriptFile) {
        this.scriptFile = scriptFile
    }

    @Override
    boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CommandInfo info = new CommandInfo()
        info.command = command
        info.sender = sender
        info.label = label
        info.args = args
        return onCommand(info)
    }

    abstract boolean onCommand(CommandInfo info)
}
