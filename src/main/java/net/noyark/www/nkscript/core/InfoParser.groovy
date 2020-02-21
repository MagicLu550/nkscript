package net.noyark.www.nkscript.core

import net.noyark.www.nkscript.dsl.DSLMethod;
import net.noyark.www.nkscript.dsl.DSLParser


// info.ns
//
class InfoParser extends DSLParser {

    InfoParser(File file) {
        super(file,"utf-8")
    }

    @Override
    void byProperty(String name, Object args, String stack) {
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
