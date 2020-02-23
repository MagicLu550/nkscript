package net.noyark.www.nkscript.dsl

import cn.nukkit.utils.Config
import net.noyark.www.nkscript.core.NKScriptParser
import org.apache.commons.io.IOUtils

import java.util.jar.JarEntry
import java.util.jar.JarFile

class Utils {

    static String byInputStream(InputStream input,String charSet){
        def builder = new StringBuilder()
        IOUtils.readLines(input,charSet).forEach{
            x->
                builder.append(x).append("\n")
        }
        return builder
    }

    static List<String> splitGroovyCode(String code,String chars){
        int index = 0
        def arr = []
        def builder = new StringBuilder()
        boolean start = false
        (0..code.size()-1).each {
            i->
                if(code[i] == "\""||code[i] == "'") {
                    if (!start) {
                        index++
                        start = true
                    } else {
                        index--
                        start = false
                    }
                }
                if(index == 0){
                    if(code[i] == chars){
                        arr.add(builder)
                        builder = new StringBuilder()
                    }else{
                        builder.append(code[i])
                    }
                }else{
                    builder.append(code[i])
                }
        }
        if(!builder.toString().isEmpty())arr.add(builder.toString())
        arr
    }

    static String getPluginYmlName(File file){
        JarFile jar = new JarFile(file)
        def entries = jar.entries()
        while (entries.hasMoreElements()){
            JarEntry entry = entries.nextElement()
            if(entry.name.endsWith("plugin.yml")){
                Config config = new Config(byInputStream(jar.getInputStream(entry), NKScriptParser.ENCODING),Config.YAML)
                return config.getString("name")
            }
        }
        null
    }

}
