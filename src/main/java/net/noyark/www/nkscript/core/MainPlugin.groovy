package net.noyark.www.nkscript.core

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

// command类和listener类可以使用直接注入
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@interface MainPlugin {
}
