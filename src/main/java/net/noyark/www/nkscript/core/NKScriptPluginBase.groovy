package net.noyark.www.nkscript.core

import cn.nukkit.plugin.PluginBase

class NKScriptPluginBase extends PluginBase {

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
}
