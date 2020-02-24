package net.noyark.www.nkscript.core

import groovy.transform.CompileStatic
import net.noyark.www.nkscript.dsl.DSLMethod;
import net.noyark.www.nkscript.dsl.DSLParser


// info.ns
//
@CompileStatic
class InfoParser extends DSLParser {

    InfoParser(File file) {
        super(file)
    }

    @DSLMethod
    void info(Closure closure){
        closure()
    }

    @DSLMethod
    void depends(Closure closure){
        closure()
    }
}
