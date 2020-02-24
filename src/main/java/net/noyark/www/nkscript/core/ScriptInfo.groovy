package net.noyark.www.nkscript.core

import groovy.transform.CompileStatic

@CompileStatic
class ScriptInfo {

    String name

    String id

    List listeners

    List commands

    String version

    String author

    List depends

    String description

    Map permissions

    Map commandsMap

    List loadBefore

    List softDepend

    List scriptDepend



    String getName() { name }

    void setName(String name) {
        this.name = name
    }

    String getId() { id }

    void setId(String id) {
        this.id = id
    }

    List<String> getListeners() { listeners }

    void setListeners(List listeners) {
        this.listeners = listeners
    }

    List<String> getCommands() { commands }

    void setCommands(List commands) {
        this.commands = commands
    }

    String getVersion() { version }

    void setVersion(String version) {
        this.version = version
    }

    String getAuthor() { author }

    void setAuthor(String author) {
        this.author = author
    }

    List<String> getDepends() { depends }

    void setDepends(List depends) {
        this.depends = depends
    }

    String getDescription() { description }

    void setDescription(String description) {
        this.description = description
    }

    Map getPermissions() { permissions }

    void setPermissions(Map permissions) {
        this.permissions = permissions
    }

    Map getCommandsMap() { commandsMap }

    void setCommandsMap(Map commandsMap) {
        this.commandsMap = commandsMap
    }

    List getLoadBefore() { loadBefore }

    void setLoadBefore(List loadBefore) {
        this.loadBefore = loadBefore
    }

    List getSoftDepend() { softDepend }

    void setSoftDepend(List softDepend) {
        this.softDepend = softDepend
    }

    List<String> getScriptDepend() { scriptDepend }

    void setScriptDepend(List scriptDepend) {
        this.scriptDepend = scriptDepend
    }
}
