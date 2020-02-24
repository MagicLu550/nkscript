package net.noyark.www.nkscript.dsl


import java.lang.reflect.Method


abstract class DSLParser {

    GroovyClassLoader loader

    Class fileClass

    Map<String, Method> methodMap = [:]

    Map<String,Object[]> entry = [:]


    DSLParser(File file){
        this(new FileInputStream(file))
    }

    DSLParser(InputStream inputStream){
        this.loader = new GroovyClassLoader(DSLParser.class.getClassLoader())
        this.fileClass = loader.parseClass(Utils.byInputStream(inputStream))
        this.fileClass.metaClass.methodMissing = {
            String name,args->
                def nowName = new StringBuilder()
                def allNames = []
                new Throwable().stackTrace.toList().each{
                    x->
                        if(x.toString().startsWith(this.class.name)){
                            def strs = x.toString().split("\\.")
                            def str = strs[strs.length-2]
                            str = str.substring(0,str.indexOf("("))
                            if(!(str == "doCall")&&!(str == "<init>")&&!(str == "main")){
                                allNames.add(str)
                            }
                        }
                }
                for(int i = allNames.size()-1;i>=0;i--){
                    nowName.append(allNames[i]).append(".")
                }
                nowName = nowName.substring(0,nowName.lastIndexOf("."))
                entry["${nowName}.${name}"] = args
        }
        this.loadDSLMethod()
        this.fileClass.getMethod("run").invoke(fileClass.newInstance())
    }

    Object[] getValue(String key,Object defaultValue){
        def t = entry[key]
        t==null?[defaultValue]:t
    }

    void loadDSLMethod(){
        this.class.methods.toList().forEach{
            x->
                if(x.getAnnotation(DSLMethod.class)){
                    this.methodMap[x.name] = x
                    this.fileClass.metaClass."${x.name}" = {
                        Closure closure->
                            x.invoke(this,closure)
                    }
                }
        }
    }
}
