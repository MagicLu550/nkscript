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
import net.noyark.www.nkscript.dsl.Utils
import net.noyark.www.nkscript.nkstarter.NKScript

import java.lang.reflect.Field
import java.lang.reflect.Method

class NKScriptParser {

    Map<String,PluginBase> plugins = [:]

    static final String INFO_FILE = "info.ns"

    static final String MAIN_FILE = "main.ns"
    
    static final String EXTENDS = "extends"

    static final String IMPLEMENTS = "implements"

    static final String PARSE_YAML_CMD = "parseYamlCommands"

    static final String ENCODING = System.getProperty("file.encoding")

    static final String FILE = ".ns"

    public static final StringBuilder DEFAULT_IMPORT = new StringBuilder()
            .append("import ${NKScriptPluginBase.name}\n")
            .append("import ${Listener.name}\n")
            .append("import ${CommandSender.name}\n")
            .append("import ${Command.name}\n")
            .append("import ${Arguments.name}\n")
            .append("import ${CommandInfo.name}\n")
            .append("import ${MainPlugin.name}\n")

    private List<File> loadedFile = []

    private Map<String,File> nameFileMapping = [:]

    private Map<File,ResultEntry> fileEntryMapping = [:]

    private List<String> loadedPluginBase = []

    private Map<String,NKScriptPluginBase> pluginBaseMap = new HashMap<>()

    private GroovyClassLoader loader

    NKScriptParser(){
        this.loader = new GroovyClassLoader(this.class.classLoader)
    }

    List<NKScriptPluginBase> loadScripts(File dir,NKScript script){
        List list = []
        List result = []
        File[] files = dir.listFiles()
        if(files != null){
            for(File file in files){
                if(file.isDirectory()){
                    def res = prepareScript(file,script)
                    result.add(res)
                    fileEntryMapping.put(file,res)
                }
            }
        }
        for(ResultEntry entry in result){
            compileCode(entry.code,entry.info,entry.file,script,entry.description,list)
        }
        for(NKScriptPluginBase base in list){
            loadScriptFile(base,script)
        }
        return list
    }

    //前置脚本
    //1 提前编译加载
    //2 提前执行loadScriptFile



    ResultEntry prepareScript(File file,NKScript starter){
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
                def code = Utils.byInputStream(new FileInputStream(new File(main)),ENCODING)
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

                //TODO script depend
                starter.server.getLogger().info(starter.server.getLanguage().translateString("nukkit.plugin.load", description.getFullName()))
                ResultEntry entry = new ResultEntry()
                entry.info = scriptInfo
                entry.code = code
                entry.file = file
                entry.description = description
                nameFileMapping.put(scriptInfo.name,file)
                //插件加载
                return entry

            }else{
                starter.server.getLogger().critical(starter.server.language.translateString("nukkit.plugin.genericLoadError", scriptInfo.name))
            }
        }
        throw new PluginException("No info.ns")

    }

    private void compileCode(String code,ScriptInfo scriptInfo,File file,NKScript starter,PluginDescription description,List list){
        if(!loadedPluginBase.contains(scriptInfo.name)){
            List depends = scriptInfo.depends
            for(String depend in depends){
                File f = nameFileMapping[depend] //获得File
                ResultEntry entry = fileEntryMapping[f]
                compileCode(entry.code,entry.info,entry.file,starter,entry.description,list)
            }
            //println(compileMain(code,scriptInfo.name,scriptInfo.id,file,"${scriptInfo.id}.${scriptInfo.name}"))
            def mainClass = loader.parseClass(compileMain(code,scriptInfo.name,scriptInfo.id,file,"${scriptInfo.id}.${scriptInfo.name}"),scriptInfo.name)
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


    void loadScriptFile(NKScriptPluginBase pluginBase,NKScript starter){
        if(!pluginBase.enabled){
            List scriptDepend = pluginBase.info.scriptDepend
            for(String depend in scriptDepend){
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
                def fileName = "${pluginBase.scriptFile}/"+listener
                loadedFile.add(new File(fileName))
                Listener list = (Listener)(loader.parseClass(compileListener(Utils.byInputStream(new FileInputStream(fileName),ENCODING),listener.split("\\.")[0],pluginBase.info.id,pluginBase.scriptFile,pluginBase.class.name)).newInstance())
                autoMainPluginObject(list,list.class,pluginBase)
                starter.server.pluginManager.registerEvents(list,pluginBase)
            }
            def commands = pluginBase.info.commands
            for(String command in commands){
                def fileName = "${pluginBase.scriptFile}/"+command
                loadedFile.add(new File(fileName))
                Object obj = loader.parseClass(compileCommand(Utils.byInputStream(new FileInputStream(fileName),ENCODING),command.split("\\.")[0],pluginBase.info.id,pluginBase.scriptFile,pluginBase.class.name)).newInstance()
                autoMainPluginObject(obj,obj.class,pluginBase)
                starter.server.commandMap.registerSimpleCommands(obj)
            }
        }

    }

    private static autoMainPluginObject(Object obj,Class clz,NKScriptPluginBase pluginBase){
        Field[] fields = clz.declaredFields
        for(Field field in fields){
            if(field.getAnnotation(MainPlugin.class)!=null){
                field.accessible = true
                field.set(obj,pluginBase)
            }
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

    private String compileCommand(String code,String name,String pack,File scriptFile,String base){
        return compileCode(name,code,"",pack,"",scriptFile,base,false)
    }

    private String compileMain(String code,String name,String pack,File scriptFile,String base){
        return compileCode(name,code,NKScriptPluginBase.simpleName,pack,EXTENDS,scriptFile,base,true)
    }

    private String compileListener(String code,String name,String pack,File scriptFile,String base){
        return compileCode(name,code, Listener.name,pack,IMPLEMENTS,scriptFile,base,false)
    }

    private String compileCode(String name,String code,String parent,String pack,String method,File scriptFile,String base,boolean main){
        Map map = splitCode(code,scriptFile,pack,base)
        String realImport = map.imports
        String realCode = map.codes

        //${main?"@${MainPlugin.name}\nstatic ${name} instance":""}
        //public static ${name} getInstance(){
        //        return instance
        //    }
        String add = main?"@${MainPlugin.simpleName} \n static ${PluginBase.simpleName} instance \npublic static ${PluginBase.simpleName} getInstance(){instance}":""
        return """
package ${pack}
${realImport}
class ${name} ${method} ${parent} {
    ${add}

    ${realCode}
    
}
        """
    }

    private String compileCommon(String code,String pack,File scriptFile,String base){
        splitCode(code,scriptFile,pack,base)
        return """
package ${pack}

${code}
"""
    }

    private Map splitCode(String code,File scriptFile,String pack,String base){
        StringBuilder importsBuilder = new StringBuilder()
        StringBuilder realCode = new StringBuilder()
        List<String> codes = Utils.splitGroovyCode(code,"\n")
        for(String c in codes){
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
                            loader.parseClass(compileCommon(Utils.byInputStream(new FileInputStream(file),ENCODING),realPack,scriptFile,base))
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
        for(int i = 1;i<arr.length-1;i++){
            builder.append(".").append(arr[i])
        }
        builder.toString()
    }




    private static boolean checkClass(String className){
        try{
            Class.forName(className)
            return true
        }catch(ClassNotFoundException e){

            return false
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
        return info
    }
}
