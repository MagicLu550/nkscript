package net.noyark.www.nkscript.core

import cn.nukkit.command.CommandSender
import cn.nukkit.command.PluginCommand
import cn.nukkit.command.simple.Arguments
import cn.nukkit.command.simple.Command
import cn.nukkit.event.Listener
import cn.nukkit.plugin.Plugin
import cn.nukkit.plugin.PluginBase
import cn.nukkit.plugin.PluginDescription
import cn.nukkit.plugin.PluginManager
import cn.nukkit.utils.PluginException
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import net.noyark.www.nkscript.dsl.Utils
import net.noyark.www.nkscript.nkstarter.NKScript

import java.lang.reflect.Method

/**
 * NKScriptParser类，脚本解析主类，主要职能是加载脚本。
 * 每一个脚本的主文件是main.ns，配置文件是info.ns
 * 解析info.ns采用了类似gradle的方式,同样使用了groovy语法
 * {@code @CompileStatic}是静态编译的标志，
 * 除非特定方法标记了{@code @CompileStatic(SKIP)}，将跳过
 * 类型检查和静态编译
 * NKScriptParser class, the main class for script parsing, whose main function is to load scripts.
 * The main file of each script is main.ns and the configuration file is info.ns
 * Parsing info.ns uses a gradle-like approach, and also uses groovy syntax
 * {@code @CompileStatic} is a sign of static compilation,
 * Skip unless specific method is marked with {@code @CompileStatic (SKIP)}
 * Type checking and static compilation
 * @author MagicLu550
 * @since JDK1.8 Nukkit API 1.0.9
 * @organization Pioneer
 */
@CompileStatic
class NKScriptParser {

    /**
     * 该字段用于存储已经加载的脚本主类对象，key为info.ns文件中的name
     * This field is used to store the script main class object that has been loaded.
     * The key is the name in the info.ns file.
     */
    Map<String,PluginBase> plugins = [:]



    static final String INFO_FILE = "info.ns"

    static final String MAIN_FILE = "main.ns"
    
    static final String EXTENDS = "extends"

    static final String IMPLEMENTS = "implements"

    static final String PARSE_YAML_CMD = "parseYamlCommands"

    static final String FILE = ".ns"

    static final String LIB_DIR = "libs"

    public static final StringBuilder DEFAULT_IMPORT = new StringBuilder()
            .append("import ${NKScriptPluginBase.name}\n")
            .append("import ${Listener.name}\n")
            .append("import ${CommandSender.name}\n")
            .append("import ${Command.name}\n")
            .append("import ${Arguments.name}\n")
            .append("import ${CommandInfo.name}\n")
            .append("import ${MainPlugin.name}\n")
            .append("import ${PluginBase.name}\n")

    List<File> loadedFile = []

    Map<String,File> nameFileMapping = [:]

    Map<File,ResultEntry> fileEntryMapping = [:]

    List<String> loadedPluginBase = []

    Map<String,NKScriptPluginBase> pluginBaseMap = new HashMap<>()

    GroovyClassLoader loader

    List<File> jarFiles

    NKScriptParser(){
        this.loader = new GroovyClassLoader(this.class.classLoader)
    }

    List<NKScriptPluginBase> loadScripts(File dir,NKScript script){
        List<NKScriptPluginBase> list = []
        List<ResultEntry> result = []
        dir.listFiles()?.toList()?.each{
            file->
                if(file.isDirectory()){
                    def res = prepareScript(file,script)
                    result.add(res)
                    fileEntryMapping.put(file,res)
                }
        }
        result.each{
            entry->
                compileCode(entry.code,entry.info,entry.file,script,entry.description,list)
        }

        list.each{
            base ->
                loadScriptFile(base,script)
        }

        return list
    }

    ResultEntry prepareScript(File file,NKScript starter){
        def depends = "${file.toString()}/" + LIB_DIR
        def dFile = new File(depends)
        if(dFile.exists()){
            dFile.listFiles()?.toList()?.each{
                f->
                    loader.addURL(f.toURI().toURL())
            }
        }
        def infoFile = "${file.toString()}/" + INFO_FILE
        def iFile = new File(infoFile)
        if(iFile.exists()){
            loadedFile.add(iFile)
            ScriptInfo scriptInfo = loadScriptInfo(new File(infoFile))
            //编译过程
            def main = "${file.toString()}/" + MAIN_FILE
            def mFile = new File(main)
            if(mFile.exists()){
                loadedFile.add(mFile)
                def code = Utils.byFileName(main)
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
                        main : "${scriptInfo.id}.${scriptInfo.name}".toString()
                ])

                starter.server.getLogger().info(starter.server.getLanguage().translateString("nukkit.plugin.load", [description.getFullName()]))
                ResultEntry entry = new ResultEntry()
                entry.info = scriptInfo
                entry.code = code
                entry.file = file
                entry.description = description
                nameFileMapping.put(scriptInfo.name,file)
                //插件加载
                return entry

            }else{
                starter.server.getLogger().critical(starter.server.language.translateString("nukkit.plugin.genericLoadError", [scriptInfo.name]))
            }
        }
        throw new PluginException("No info.ns")

    }

    private void compileCode(String code,ScriptInfo scriptInfo,File file,NKScript starter,PluginDescription description,List list){
        if(!loadedPluginBase.contains(scriptInfo.name)){
            scriptInfo.depends.each{
                depend ->
                    File f = (File)nameFileMapping[depend] //获得File
                    ResultEntry entry = fileEntryMapping[f]
                    compileCode(entry.code,entry.info,entry.file,starter,entry.description,list)
            }
            def mainClass = loader.parseClass(compileMain(code,scriptInfo.name,scriptInfo.id,file,"${scriptInfo.id}.${scriptInfo.name}",scriptInfo.retainMainClass),scriptInfo.name)
            NKScriptPluginBase pluginBase = (NKScriptPluginBase)mainClass.newInstance()
            autoMainPluginObject(pluginBase,pluginBase.class,pluginBase)
            pluginBase.scriptFile = file
            pluginBase.info = scriptInfo
            pluginBase.init(starter.pluginLoader,starter.server,description,starter.dataFolder,pluginBase.scriptFile)
            loadedPluginBase.add(scriptInfo.name)
            pluginBaseMap[scriptInfo.name] = pluginBase
            list.add(pluginBase)
        }

    }


    @CompileStatic(TypeCheckingMode.SKIP)
    void loadScriptFile(NKScriptPluginBase pluginBase,NKScript starter){
        if(!pluginBase.enabled){
            pluginBase.info.scriptDepend.each{
                depend ->
                    loadScriptFile(pluginBaseMap[depend],starter)
            }
            PluginManager manager = starter.server.pluginManager

            manager.plugins.put(pluginBase.info.name,pluginBase)

            this.plugins.put(pluginBase.info.name,pluginBase)

            Method method = manager.class.getDeclaredMethod(PARSE_YAML_CMD,Plugin.class)

            method.accessible = true

            List<PluginCommand> li = (List)method.invoke(manager,pluginBase)

            starter.server.commandMap.registerAll(pluginBase.getDescription().getName(), li)

            pluginBase.onLoad()

            List depends = pluginBase.info.depends
            List loadBefore = pluginBase.info.loadBefore
            List softDepend = pluginBase.info.softDepend

            Map<String,Plugin> loadedPlugins = manager.plugins

            Map plugins = getPluginFileByName(starter)

            loadDepend(depends,loadBefore,softDepend,manager,loadedPlugins,plugins,starter,pluginBase)

            manager.enablePlugin(pluginBase)

            pluginBase.info.listeners.each{
                listener->
                    def fileName = "${pluginBase.scriptFile}/"+listener
                    loadedFile.add(new File(fileName))
                    // arg 2 name
                    // arg 3 pack
                    Map map = getPackageAndName(pluginBase,listener)
                    Listener list = (Listener)(loader.parseClass(compileListener(Utils.byInputStream(new FileInputStream(fileName)),map.name,map.pack,pluginBase.scriptFile,pluginBase.class.name)).newInstance())
                    autoMainPluginObject(list,list.class,pluginBase)
                    starter.server.pluginManager.registerEvents(list,pluginBase)
            }
            pluginBase.info.commands.each{
                command ->
                    def fileName = "${pluginBase.scriptFile}/"+command
                    loadedFile.add(new File(fileName))
                    Map map = getPackageAndName(pluginBase,command)
                    Object obj = loader.parseClass(compileCommand(Utils.byInputStream(new FileInputStream(fileName)),map.name,map.pack,pluginBase.scriptFile,pluginBase.class.name)).newInstance()
                    autoMainPluginObject(obj,obj.class,pluginBase)
                    starter.server.commandMap.registerSimpleCommands(obj)
            }
        }

    }

    private static Map getPackageAndName(NKScriptPluginBase pluginBase,String fileName){
        String file = fileName.split("\\.")[0].replace(File.separator,".")
        String pack = "${pluginBase.info.id}.${getPackage(file)}".toString()
        String name = file.split("\\.").last()
        return [pack : pack,name : name]
    }


    private String compileCommand(String code,String name,String pack,File scriptFile,String base){
        return compileCode(name,code,"",pack,"",scriptFile,base,false,false)
    }

    private String compileMain(String code,String name,String pack,File scriptFile,String base,boolean remain){
        return compileCode(name,code,NKScriptPluginBase.simpleName,pack,EXTENDS,scriptFile,base,true,remain)
    }

    private String compileListener(String code,String name,String pack,File scriptFile,String base){
        return compileCode(name,code, Listener.name,pack,IMPLEMENTS,scriptFile,base,false,false)
    }

    private String compileCode(String name,String code,String parent,String pack,String method,File scriptFile,String base,boolean main,boolean remain){
        Map map = splitCode(code,scriptFile,pack,base)
        String realImport = map.imports
        String realCode = map.codes
        String add = main?"@${MainPlugin.simpleName} \n static ${PluginBase.simpleName} instance \npublic static ${PluginBase.simpleName} getInstance(){instance}":""
        String realAdd = remain?realCode:"""
            class ${name} ${method} ${parent} {
                 ${add}

                ${realCode}
    
            }
        """
        return """
            package ${pack}
            ${realImport}
            ${realAdd}
        """
    }

    private String compileCommon(String code,String pack,File scriptFile,String base){
        splitCode(code,scriptFile,pack,base)
        return """
            package ${pack}

            ${code}
        """
    }


    @CompileStatic(TypeCheckingMode.SKIP)
    private Map splitCode(String code,File scriptFile,String pack,String base){
        StringBuilder importsBuilder = new StringBuilder()
        StringBuilder realCode = new StringBuilder()
        Utils.splitGroovyCode(code,"\n").each{
            c ->
                c = c.toString()
                if(c.startsWith("import")){
                    String className = c.substring("import".size()).replace(" ","").replace("\t","")
                    importsBuilder.append(c).append("\n")
                    if(!checkClass(className)){
                        //导入类
                        if(className.startsWith(pack)&&!(className == base)){
                            String name = className.substring(pack.size())
                            name = name.replace(".","/")
                            def file = "${scriptFile.toString()}${name}${FILE}"
                            def sFile = new File(file)
                            def realPack = getPackage(className)
                            if(!loadedFile.contains(sFile)){
                                loader.parseClass(compileCommon(Utils.byFileName(file),realPack,scriptFile,base))
                                loadedFile.add(sFile)
                            }

                        }
                    }
                }else{
                    realCode.append(c).append("\n")
                }
        }
        importsBuilder.append(DEFAULT_IMPORT)
        return [imports : importsBuilder, codes : realCode]
    }

    private static String getPackage(String className){
        StringBuilder builder = new StringBuilder()
        def arr = className.split("\\.")
        builder.append(arr[0])
        if(arr.length > 1){
            (1..<arr.length-1).each{
                builder.append(".").append(arr[it])
            }
        }

        builder.toString()
    }

    private static boolean checkClass(String className){
        try{
            Class.forName(className)
            true
        }catch(ClassNotFoundException e){
            false
        }
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
        List scriptDepend = (List)parser.getValue("depends.scriptDepend",[])[0]
        Map permissions = (Map)parser.getValue("info.permissions",[:])[0]
        Map commandsMap = (Map)parser.getValue("info.commands",[:])[0]
        Boolean retain = (Boolean)parser.getValue("info.retain",false)[0]
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
        info.scriptDepend = scriptDepend
        info.retainMainClass = retain
        return info
    }

    //如果有人吐槽我为啥不用it取代那个i，我觉得it可读性不高，只有我在遍历数字时会用到
    private static autoMainPluginObject(Object obj,Class clz,NKScriptPluginBase pluginBase){
        clz.declaredFields.toList().each {
            i->
                if(i.getAnnotation(MainPlugin.class)!=null){
                    i.accessible = true
                    i.set(obj,pluginBase)
                }
        }
    }

    private void loadDepend(List<String> depends,List<String> loadBefore,List<String> softDepend,PluginManager manager,Map<String,Plugin> loadedPlugins,Map<String,File> plugins,NKScript starter,PluginBase pluginBase){
        //检查加载depend
        def load = {
            String depend ->
                if(!loadedPlugins.containsKey(depend)){
                    Plugin plugin = manager.loadPlugin(plugins.get(depend))
                    plugin.onLoad()
                    loadDepend(
                            plugin.description.depend,
                            plugin.description.loadBefore,
                            plugin.description.softDepend,
                            manager,
                            loadedPlugins,
                            plugins,
                            starter,
                            (PluginBase)plugin
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
        depends.each{
            depend->
                if(!plugins[depend]){
                    starter.logger.error("${pluginBase.name} Could not load depend : ${depend}")
                    return
                }else {
                    load(depend)
                }
        }
        loadBefore.each{
            depend ->
                load(depend)
        }
        softDepend.each{
            soft ->
                loader.addURL(new File("${((NKScriptPluginBase)pluginBase).scriptFile}/"+soft).toURI().toURL())
        }
    }
    private Map<String,File> getPluginFileByName(NKScript script){
        Map<String,File> map = [:]
        File[] pluginFiles = script.dataFolder.parentFile.listFiles()
        if(!jarFiles){
            jarFiles = []
            getJarFile(pluginFiles,jarFiles)
        }
        jarFiles.each{
            f->
                map[Utils.getPluginYmlName(f)] = f
        }
        return map
    }

    private static void getJarFile(File[] pluginFiles,List files){
        pluginFiles?.toList()?.each{
            f->
                if(f.isDirectory())
                    getJarFile(f.listFiles(),files)
                else if(f.name.endsWith(".jar"))
                    files.add(f)
        }
    }

}
