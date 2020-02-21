# NKscript 

简化Nk插件的开发模式，使用脚本化编程，无需编译直接运行

NKScript 脚本化编程方案

NKScript的目的是以简化开发为主，并且更好的调试插件。

### 对于项目结构和调用文件

NKScript主要分为5个模块

- info.ns
- main.ns
- listener
- command
- other

info.ns类似于nukkit的plugin.yml,其内容和plugin.yml也基本类似。
其中特殊的元素为`id`元素，是为了防止多个插件冲突而设计的，它会作为注册文件的类根目录。
在调用脚本时，格式就是id.脚本文件名称
例如id为`net.noyark`,则脚本hello.ns写作
```groovy
package net.noyark

class hello{
    ///...
}

```
调用的时候
```groovy
import net.noyark.hello
```
当然在写主类和监听器的时候，`package`和`class`可以省略
如main.ns
```groovy
@Override
void onLoad(){
    //...
}
```
listener.ns
```groovy
@EventHandler
void onJoin(PlayerJoinEvent e){
    
}

```
脚本的主文件必须为main.ns,调用主文件的时候，则是`import 你的id.插件名`
如插件名为`Hello`,id为`net.noyark`
```groovy
//导入主类
import net.noyark.Hello
```

总之，对于文件导入，除了主类是id.pluginName,其他都是id.脚本文件名,
只有listener和command，主类可以省略packageName和class,其他必须要
有类名

### info.ns书写
```groovy
info{
      name "Hello" //插件名称,必须有
      version "1.0.0" //版本，可不写，不写默认为1.0.0
      author "MagicLu" //作者名 可不写
      listeners ["listener.ns"] //注册的监听器 如果没有可不写
      commandsMap ["command.ns"] //注册的指令 如果没有可不写
      id "net.noyark.www" //id号，意义如同前面所讲 
      description "" //介绍，可不写
      permissions ([
           "FirstPlugin.fp" : [
                  description : "",
                  default : "op"
           ]
      ]) //和nk插件的plugin.yml结构相同
      commands ([
          fp : [
              "usage" : "/fp help",
              "description" : "指令介绍",
              "permission" : "FirstPlugin.fp"
          ]
      ]) //和nk插件的plugin.yml结构相同
}
depends{ //依赖
    softDepend ([])
    depend ([])
    loadBefore([])
}

```

其他基本和NK相同，采用groovy语法，并且支持java的写法，注册监听器和指令同样可以
使用原来的方法

