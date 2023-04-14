# IvanSynchronizer

一款用java编写的于DNF的网络键鼠同步器，可以拿来打金搬砖，也可以用来做其他任何键鼠远控同步操作

## 本项目主要使用到了如下框架：

- RocketMQ (进行键鼠消息的发布和订阅，一对多通信)

- JNA（对主控机上的键盘击键，鼠标移动、点击、滚轮等Windows事件进行Hook，拦截对应事件后发布消息）

  

## 原理

目前的DNF工作室他们用的是多台物理机器（8/16/32）+分屏器（把所有机器的显卡输出到一个超大屏幕上）+键鼠同步器（硬件同步器，本质是键鼠并联）。

我的解决方案是用虚拟机多开，配合这个同步器软件，选择一台虚拟机当大号，其他虚拟机当小号，一起同步刷图，原理和工作室的物理同步思路是一样的，即1带N。

### 架构

基于RocketMQ进行键鼠消息的发布和订阅，虽然RocketMQ有C++的API，但是我当时写的时候还不知道（

RocketMQ采用顺序消费，因为键鼠消息是严格有序的，不可以玩并发消费，以鼠标消息为例，生产者Master在投递消息时，必须保证鼠标消息被投放到同一个消息队列里面，不能投递到多个队列里（因为可能不同队列，消息的消费的顺序不一定就是投递时的顺序），所以主题的读写队列设置为了1个。

并且需要让所有的Slave都能处理同一个消息，所以需要用到广播模式，但是这里的广播是用的组间广播，不是组内广播，RocketMQ的每个Group都能独立地从一个主题消费消息，所以一个组里用 一个消费者，对于同一个主题，因为只有一个读队列，多余的消费者没有意义，负载均衡算法会只让唯一一个消费者处理该队列。

为什么不用组内广播？即把所有订阅同一主题Slave的消费者放在一个Group里面？这样做当然可以，但是这种组内广播有一个缺陷：不具备失败重试机制，如果消息消费失败，不会重新投递，而用组间广播每个组是独立的，组内仍然是采用集群模式，所以具备重试机制。当然代价是组变得很多。

![1681228913047](https://github.com/JoivanJostar/IvanSynchronizer/blob/master/assets/1681228913047.png)



### 键鼠消息的拦截和事件发送

### Master拦截+发送消息

Master在底层键鼠事件的拦截用JNA调用User32.dll,编写钩子函数 LowLevelMouseProc LowLevelKeyboardProc，详细API介绍以及参数意义见MSDN。

一旦Hook回调函数被调用，就发送对应的消息包，消息格式分为键盘消息和鼠标消息，见工程的MessageEntity目录

序列化用的Json。

### Slave消费+发送事件

Slave收到消息后，进行消费：先对消息体Body进行反序列化得到Java对象，然后根据事件状态码，用JNA框架调用Native方法SendInput (用法见MSDN)发送键鼠事件。

## 使用：

1. 选择一台机器当做NameServer,负责处理注册和路由管理，再选一台机器当Broker负责维护主题和消息队列。

   这里我的选择是用我的宿主机（物理机器）当NameServer+Broker

2. 选择一台机器当主号机器，这里我选择一台虚拟机上主号，作为Mater节点，安装WeGame、DNF、以及本软件的Master和Slave程序，然后其余虚拟机克隆Master机，得到N个从机

3. 用RocketMQ管理工具（DashBoard)提前在当前集群内创建3个Topic,分别为ControlMessage、KeyboardActions、MouseActions、读写队列数量都选1，Pem=6

   - 主题名不要改，我硬编码到程序里面了

4. WeGame上号，Master启动DNFMaster.exe，（需要提前配置Config.propertise文件，设置NameServerAddr为NameServer机器的ipV4地址)，此时DNFMaster会和NameServer建立连接获取Broker信息。

5. 从机上好号后，启动DNFSlave 订阅键鼠消息,（需要提前配置Config.propertise文件，设置NameServerAddr为NameServer机器的ipV4地址，设置SlaveName从机名字，这个名字会作为GroupName创建消费者组，各个Slave机器上的SlaveName不要重名，否则有的机器会收不到消息（因为RocketMQ负载均衡算法使得每个队列只能由该组的一个消费者消费））

6. 第一次启动DNFSlave时可能会报异常，因为Group信息没有提前创建，没关系NameServer会自动创建Group,此时可以关闭程序重新启动就不会抛异常了。或者提前在DashBorad里面创建好Group，但是要和对应Slave名字保持一致

7. 开启同步：此时Master和Slave都已经就位，但是同步尚未开始，此时进入DashBoard，向ControlMessage主题发送一个tag=enable的消息，这个消息由Master订阅，并在收到此消息后开始发送键鼠消息到Broker，此时同步正式开始。

   如果需要临时关闭同步，向ControlMessage主题发送一个tag=disable的消息



## 注意事项

- 虚拟机要能过VMP等机制，否则腾讯TP虚拟化检测会不让运行游戏（过虚拟机虚拟化的方法是在虚拟机系统上打一些插件，过滤一些虚拟化指令等等、这里可以去找一些已经过VMP检测的虚拟机 52pj上就有，但是小心后门）

- 当心封号，DNF官方打压工作室，有行为检测，长期打金，玩得不像正常人就会被Ban，这是难以避免的。

- 收益问题，看个人机器上限吧。我的环境是2颗20核的至强+128G内存+QuadroRTX5000，够开很多很多VM了。

- 如果性能不够好，可在VM里面把windows调到性能模式，游戏的各种性能参数拉低，Wegame纯净模式。。。。。



## 开发时踩到的坑：

1.DNF模拟键盘操作时，由于DNF本身用的是DirectInput，绕过了Windows的消息机制，直接SendInput 虚拟VirtualKeyCode无效，根本不会被DNF接收到，需要改用ScanCode，发送硬件扫描码,ScanCode和键盘布局有关，可以由VKcode映射到ScanCode，相关函数为MapVirtualKeyEx

2.方向键（上下左右箭头）的处理比较特殊，我直接将方向键的VKcode映射为ScanCode,发现没有效果，后台调试一下发现4个方向箭头的ScanCode是扩展键（Extended Key),正常的ScanCode是用一个字节表示，扩展键高位加一个0xE0字节表示扩展，扩展键对应小键盘上的4，8，6，2，可以发现，确实有上下左右箭头。。。。以上箭头键为例，它的ScanCode如果直接映射，会发现返回结果只有一个字节，并且和小键盘8的ScanCode码一致。。。所以之前箭头无效是因为被当成小键盘数字键处理了。此时需要指明该键位是一个扩展键（通过额外的标志位指定，或者直接手动给高位字节赋值位0xE0),之后就可以用方向键移动了

3.DNF使用DirectInput是在每一帧渲染时读键盘设备，获取一次键盘按键情况，并且用的读取模式为DirectIO而非BufferedIO,如果相邻两个按键事件间隔很小的话，会出现这样一种情况:
假如我们想要按下U键盘

第一帧 读取键盘按键情况 U=keyUp 未按下

同步器发送U keyDown

同步器发送U keyUp

第二帧 读取键盘按键情况 U=keyUp未按下

此时游戏进程判断不出U被按下，根本原因是按键发送速率大于游戏渲染速率（以60帧为例，渲染间隔大概为16ms) DirectInput读取到当前渲染时的键盘状态依旧是没有按下。
对于一些连发器软件，可以发现他们有在两个Send之间Sleep(10ms)这种操作。
当然我们的同步器本身是基于网络的，从Consumer收到消息到消费完成，其延时也足以到帧渲染间隔，所以没有太大问题。



# 贴一张自己实机测试的图：

![1681229707664](https://github.com/JoivanJostar/IvanSynchronizer/blob/master/assets/1681229707664.png)
