package net.noyark.www.nkscript.core

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



    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    String getId() {
        return id
    }

    void setId(String id) {
        this.id = id
    }

    List getListeners() {
        return listeners
    }

    void setListeners(List listeners) {
        this.listeners = listeners
    }

    List getCommands() {
        return commands
    }

    void setCommands(List commands) {
        this.commands = commands
    }

    String getVersion() {
        return version
    }

    void setVersion(String version) {
        this.version = version
    }

    String getAuthor() {
        return author
    }

    void setAuthor(String author) {
        this.author = author
    }

    List getDepends() {
        return depends
    }

    void setDepends(List depends) {
        this.depends = depends
    }

    String getDescription() {
        return description
    }

    void setDescription(String description) {
        this.description = description
    }

    Map getPermissions() {
        return permissions
    }

    void setPermissions(Map permissions) {
        this.permissions = permissions
    }

    Map getCommandsMap() {
        return commandsMap
    }

    void setCommandsMap(Map commandsMap) {
        this.commandsMap = commandsMap
    }

    List getLoadBefore() {
        return loadBefore
    }

    void setLoadBefore(List loadBefore) {
        this.loadBefore = loadBefore
    }

    List getSoftDepend() {
        return softDepend
    }

    void setSoftDepend(List softDepend) {
        this.softDepend = softDepend
    }
}
