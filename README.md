# Android App监听自身卸载，反馈统计

## 方案
1. 监听系统卸载广播：只能监听到其他应用的卸载广播，无法监听到自己是否被卸载。
2. 读取系统log：第三方软件卸载无法得知。
3. 静默安装另一个程序，监听自己是否被卸载：需要root权限。
4. Java线程轮询，监听/data/data/{package-name}目录是否存在：卸载app，进程退出，线程也被销毁。
5. C进程轮询，监听/data/data/{package-name}目录是否存在：目前业界普遍采用的方案。

## 原理
从前四种方案可以看到，单纯的Java层代码是无法监听自身卸载的。既然Java层无法实现，我们试着使用C语言在底层实现。借助Java层进程fork出来的C进程在应用被卸载后不会被销毁，监听/data/data/{package-name}目录是否存在，如果不存在，就证明应用被卸载了。
本程序采用第5种解决方案，对其进行优化，通过linux中的[inotify](http://man7.org/linux/man-pages/man7/inotify.7.html)机制来监听应用的卸载。

## 实现
1. fork()子进程
2. 创建监听文件
3. 初始化inotify实例
4. 注册监听事件
5. 调用read函数开始监听
6. 卸载反馈统计

## 场景
1. 正常卸载
2. 断网卸载
3. 清除数据（5.0以上不支持）
4. 插拔USB线
5. 覆盖安装
6. 内部存储移到SD卡
7. 开机启动监测（官方不推荐）

## 参考
* [http://www.cnblogs.com/zealotrouge/p/3182617.html](http://www.cnblogs.com/zealotrouge/p/3182617.html)
* [http://www.jianshu.com/p/189e319a5c45](http://www.jianshu.com/p/189e319a5c45)
* [http://blog.csdn.net/allen315410/article/details/42555415](http://blog.csdn.net/allen315410/article/details/42555415)
* [http://www.netzmafia.de/skripten/unix/linux-daemon-howto.html](http://www.netzmafia.de/skripten/unix/linux-daemon-howto.html)

## About
* Email：venshine.cn@gmail.com

## License
    Copyright (C) 2016 venshine.cn@gmail.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.



