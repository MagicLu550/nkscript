package net.noyark.www.nkscript.nkstarter

import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender
import cn.nukkit.plugin.Plugin
import cn.nukkit.plugin.PluginBase
import cn.nukkit.utils.TextFormat
import groovy.transform.CompileStatic
import net.noyark.www.nkscript.core.NKScriptParser
import net.noyark.www.nkscript.core.NKScriptPluginBase

@CompileStatic
class NKScript extends PluginBase {

    private Map commands = [
            "list" : "列出所有加载的script插件" ,
            "help" : "help"
    ]
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
                    this.parser.plugins.values().each{
                        Plugin x->
                            sender.sendMessage("${x.name}----${((NKScriptPluginBase)x).info.id}")
                    }
                }
                if(args[0] == "help"){
                    commands.each{
                        e->
                            this.logger.info("${TextFormat.RED}${e.key}:${TextFormat.GREEN}${e.value}")
                    }
                }
            }
            return true
        }
        false
    }
}
