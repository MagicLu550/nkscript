package net.noyark.www.nkscript.core

import cn.nukkit.command.PluginCommand
import cn.nukkit.event.Listener
import cn.nukkit.plugin.Plugin
import cn.nukkit.plugin.PluginBase
import cn.nukkit.plugin.PluginDescription
import cn.nukkit.plugin.PluginManager
import cn.nukkit.utils.PluginException
import net.noyark.www.nkscript.dsl.Utils
import net.noyark.www.nkscript.nkstarter.NKScript

import java.lang.reflect.Method

// NkScript插件结构
// - 根文件夹
// -   info.ns
// -   main.ns
// -   listener.ns
// -   commands.ns
// -   others
// info.ns
//info{
//    name "hello"
//    listeners ["listener.ns"]
//    commands ["commands.ns"]
//    version "1.0.0"
//    author "MagicLu"
//    id "net.noyark.www"
//    description ""
//    permissions ([
//            FirstPlugin.fp : [
//                    description : "",
//                    default : "op"
//            ]
//    ])
//    commands ([
//            fp : [
//                    "usage" : "/fp help",
//                    "description" : "指令介绍",
//                    "permission" : "FirstPlugin.fp"
//            ]
//    ])
//}
// main.ns
// import xxx
// def onLoad(){
// }
// def onEnable(){
// }
class NKScriptParser {

    Map<String,PluginBase> plugins = [:]

    static final String INFO_FILE = "info.ns"

    static final String MAIN_FILE = "main.ns"
    
    static final String EXTENDS = "extends"

    static final String IMPLEMENTS = "implements"

    static final String PARSE_YAML_CMD = "parseYamlCommands"

    private Map<String, PluginBase> scripts = new HashMap<>()

    private GroovyClassLoader loader

    NKScriptParser(){
        this.loader = new GroovyClassLoader(this.class.classLoader)
    }

    List<NKScriptPluginBase> loadScripts(File dir,NKScript script){
        List list = []
        File[] files = dir.listFiles()
        if(files != null){
            for(File file in files){
                if(file.isDirectory())
                    list.add(prepareScript(file,script))
            }
        }
        for(NKScriptPluginBase base in list){
            loadScriptFile(base,script)
        }
        return list
    }

    NKScriptPluginBase prepareScript(File file,NKScript starter){
        def infoFile = "${file.toString()}/" + INFO_FILE
        if(new File(infoFile).exists()){
            ScriptInfo scriptInfo = loadScriptInfo(new File(infoFile))
            //编译过程
            def main = "${file.toString()}/" + MAIN_FILE
            if(new File(main).exists()){
                def code = Utils.byInputStream(new FileInputStream(new File(main)),System.getProperty("file.encoding"))
                def mainClass = loader.parseClass(compileMain(code,scriptInfo.name,scriptInfo.id),scriptInfo.name)
                // 形成PluginDescription
                // permissions
                PluginDescription description = new PluginDescription([
                        name : scriptInfo.name,
                        commands : scriptInfo.commandsMap,
                        depend : scriptInfo.depends,
                        loadbefore : scriptInfo.loadBefore,
                        softdepend : scriptInfo.softDepend,
                        description : scriptInfo.description,
                        author : scriptInfo.author,
                        permissions : scriptInfo.permissions,
                        api : starter.getDescription().getCompatibleAPIs(),
                        version : scriptInfo.version,
                        main : mainClass.name
                ])


                starter.server.getLogger().info(starter.server.getLanguage().translateString("nukkit.plugin.load", description.getFullName()));
                //插件加载
                NKScriptPluginBase pluginBase = (NKScriptPluginBase)mainClass.newInstance()
                pluginBase.scriptFile = file
                pluginBase.info = scriptInfo
                pluginBase.init(starter.pluginLoader,starter.server,description,starter.dataFolder,pluginBase.scriptFile)
                return pluginBase

            }else{
                starter.server.getLogger().critical(starter.server.language.translateString("nukkit.plugin.genericLoadError", scriptInfo.name))
            }
        }
        throw new PluginException("No info.ns")

    }


    void loadScriptFile(NKScriptPluginBase pluginBase,NKScript starter){
        PluginManager manager = starter.server.pluginManager
        manager.plugins.put(pluginBase.info.name,pluginBase)
        this.plugins.put(pluginBase.info.name,pluginBase)

        Method method = manager.class.getDeclaredMethod(PARSE_YAML_CMD,Plugin.class)
        method.accessible = true
        List<PluginCommand> li = (List)method.invoke(manager,pluginBase)
        starter.server.commandMap.registerAll(pluginBase.getDescription().getName(), li)
        pluginBase.onLoad()
        
        //...
        //前置加载-三种
        // 1. 第三方库导入
        // 2. 插件前置加载
        // 3. 脚本插件加载
        List depends = pluginBase.info.depends
        List loadBefore = pluginBase.info.loadBefore
        List softDepend = pluginBase.info.softDepend

        Map<String,Plugin> loadedPlugins = manager.plugins //已经加载的插件
        Map plugins = getPluginFileByName(starter)

        loadDepend(depends,loadBefore,softDepend,manager,loadedPlugins,plugins,starter,pluginBase)

        //onEnable
        manager.enablePlugin(pluginBase)
        
        List listeners = pluginBase.info.listeners
        for(String listener in listeners){
            def fileName = "${pluginBase.scriptFile.name}/"+listener
            Listener list = (Listener)(loader.parseClass(compileListener(Utils.byInputStream(new FileInputStream(fileName),System.getProperty("file.coding")),listener.split("\\.")[0],pluginBase.info.id)).newInstance())
            starter.server.pluginManager.registerEvents(list,pluginBase)
        }
        def commands = pluginBase.info.commands
        for(String command : commands){
            def fileName = "${pluginBase.scriptFile.name}/"+command
            Object obj = loader.parseClass(compileListener(Utils.byInputStream(new FileInputStream(fileName),System.getProperty("file.coding")),command.split("\\.")[0],pluginBase.info.id)).newInstance()
            starter.server.commandMap.registerSimpleCommands(obj)
        }
    }

    private static void loadDepend(List depends,List loadBefore,List softDepend,PluginManager manager,Map<String,Plugin> loadedPlugins,Map<String,File> plugins,NKScript starter,PluginBase pluginBase){
        //检查加载depend
        def load = {
            String depend ->
            if(!loadedPlugins.containsKey(depend)){
                Plugin plugin = manager.loadPlugin(plugins.get(depend)).onLoad()
                loadDepend(
                        plugin.description.depend,plugin.description.loadBefore,plugin.description.softDepend
                        ,manager,loadedPlugins,plugins,starter,(PluginBase)plugin
                )
                manager.enablePlugin(plugin)
            }else{
                Plugin plugin = loadedPlugins[depend]
                if(!plugin.enabled){
                    loadDepend(
                            plugin.description.depend,plugin.description.loadBefore,plugin.description.softDepend
                            ,manager,loadedPlugins,plugins,starter,(PluginBase)plugin
                    )
                    manager.enablePlugin(plugin)
                }
            }
        }
        for(String depend in depends){
            if(plugins[depend] == null){
                starter.logger.error("${pluginBase.name} Could not load depend : ${depend}")
                return
            }else {
                load(depend)
            }
        }
        for(String depend : loadBefore){
            load(depend)
        }
        for(String soft : softDepend){
            URL[] url = new URL[1]
            url[0] = new File(soft).toURI().toURL()
            new URLClassLoader(url,this.classLoader)
        }
    }
    private static Map<String,File> getPluginFileByName(NKScript script){
        Map<String,File> map = [:]
        List<File> files = []
        File[] pluginFiles = script.dataFolder.parentFile.listFiles()
        getJarFile(pluginFiles,files)
        for(File f in files){
             map[Utils.getPluginYmlName(f)] = f
        }
        return map
    }

    private static void getJarFile(File[] pluginFiles,List files){
        if(pluginFiles!=null){
            for(File f in pluginFiles){
                if(f.isDirectory()){
                    getJarFile(f.listFiles(),files)
                }else if(f.name.endsWith(".jar")){
                    files.add(f)
                }
            }
        }
    }

    private static String compileCommand(String code,String name,String pack){
        return compileCode(name,code,"",pack,"")
    }

    private static String compileMain(String code,String name,String pack){
        return compileCode(name,code,NKScriptPluginBase.simpleName,pack,EXTENDS)
    }

    private static String compileListener(String code,String name,String pack){
        return compileCode(name,code, Listener.name,pack,IMPLEMENTS)
    }

    private static String compileCode(String name,String code,String parent,String pack,String method){
        Map map = splitCode(code)
        String realImport = map.imports
        String realCode = map.codes
        return """
package ${pack}
${realImport}
class ${name} ${method} ${parent} {
    ${realCode}
}
        """
    }
    private static Map splitCode(String code){
        StringBuilder importsBuilder = new StringBuilder()
        StringBuilder realCode = new StringBuilder()
        List<String> codes = Utils.splitGroovyCode(code,"\n")
        for(String c in codes){
            if(c.startsWith("import")){
                importsBuilder.append(c).append("\n")
            }else{
                realCode.append(c).append("\n")
            }
        }
        importsBuilder.append("import ${NKScriptPluginBase.name}")
        return [imports : importsBuilder, codes : realCode]
    }

    private static ScriptInfo loadScriptInfo(File infoFile){
        InfoParser parser = new InfoParser(infoFile)
        String name = parser.getValue("info.name","")[0]
        List listeners = (List)parser.getValue("info.listeners",[])[0]
        List commands = (List)parser.getValue("info.commandsMap",[])[0]
        String version = parser.getValue("info.version","1.0.0")[0]
        String author = parser.getValue("info.author","")[0]
        String id = parser.getValue("info.id","")[0]
        String description = parser.getValue("info.description","")[0]
        List depends = (List)parser.getValue("depends.depend",[])[0]
        List softDepend = (List)parser.getValue("depends.softDepend",[])[0]
        List loadBefore = (List)parser.getValue("depends.loadBefore",[])[0]
        Map permissions = (Map)parser.getValue("info.permissions",[:])[0]
        Map commandsMap = (Map)parser.getValue("info.commands",[:])[0]
        ScriptInfo info = new ScriptInfo()
        info.name = name
        info.commands = commands
        info.listeners = listeners
        info.version =  version
        info.author = author
        info.id = id
        info.depends = depends
        info.description = description
        info.permissions = permissions
        info.commandsMap = commandsMap
        info.loadBefore = loadBefore
        info.softDepend = softDepend
        return info
    }
}
