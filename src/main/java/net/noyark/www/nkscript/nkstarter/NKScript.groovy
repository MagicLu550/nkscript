package net.noyark.www.nkscript.nkstarter

import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender
import cn.nukkit.plugin.Plugin
import cn.nukkit.plugin.PluginBase
import net.noyark.www.nkscript.core.NKScriptParser
import net.noyark.www.nkscript.core.NKScriptPluginBase

class NKScript extends PluginBase {

    private NKScriptParser parser

    @Override
    void onLoad() {
        this.logger.info("NKScript 插件启动，作者: MagicLu550,请将脚本放在插件配置文件夹下")
    }

    @Override
    void onEnable() {
        if(!this.dataFolder.exists()) this.dataFolder.mkdir()
        this.parser = new NKScriptParser()
        this.parser.loadScripts(this.dataFolder,this)
    }

    @Override
    void onDisable() {
        this.logger.info("NKScript 插件关闭")
    }

    @Override
    boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if("script" == command.name){
            if(args.length == 1){
                if(args[0] == "list"){
                    sender.sendMessage("NKScript列表")
                    this.parser.plugins.values().forEach{
                        Plugin x->
                        sender.sendMessage("${x.name}----${((NKScriptPluginBase)x).info.id}")
                    }
                }
            }
            return true
        }
        return false
    }
}
