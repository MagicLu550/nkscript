package net.noyark.www.nkscript.core

import cn.nukkit.plugin.PluginDescription;

class ResultEntry {

    private String code

    private ScriptInfo info

    private File file

    private PluginDescription description

    String getCode() { code }

    void setCode(String code) {
        this.code = code
    }

    ScriptInfo getInfo() { info }

    void setInfo(ScriptInfo info) {
        this.info = info
    }

    File getFile() { file }

    void setFile(File file) {
        this.file = file
    }

    PluginDescription getDescription() { description }

    void setDescription(PluginDescription description) {
        this.description = description
    }
}
