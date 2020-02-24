package net.noyark.www.nkscript.dsl

import cn.nukkit.utils.Config
import groovy.transform.CompileStatic

import java.util.jar.JarEntry
import java.util.jar.JarFile

@CompileStatic
class Utils {

    static String byInputStream(InputStream input){
        return input.text
    }

    static String byFileName(String file){
        return byFile(new File(file))
    }

    static String byFile(File file){
        return file.text
    }
    static List<String> splitGroovyCode(String code,String chars){
        int index = 0
        def arr = []
        def builder = new StringBuilder()
        boolean start = false
        (0..<code.size()).each{
            it->
                if(code[it] == "\""||code[it] == "'") {
                    if (!start) {
                        index++
                        start = true
                    } else {
                        index--
                        start = false
                    }
                }
                if(index == 0){
                    if(code[it] == chars){
                        arr.add(builder)
                        builder = new StringBuilder()
                    }else{
                        builder.append(code[it])
                    }
                }else{
                    builder.append(code[it])
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
                Config config = new Config(byInputStream(jar.getInputStream(entry)),Config.YAML)
                return config.getString("name")
            }
        }
        null
    }



}
