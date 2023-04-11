package Client;

import Hooker.KeyboardEventHooker;
import Hooker.MouseEventHooker;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * 主控机
 */
public class DNFMaster {
    public static boolean enableRun=false;
    public static void main(String[] args) throws MQClientException {
        Logger logger = LoggerFactory.getLogger(DNFMaster.class);

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("配置文件错误");
            return;
        }
        String nameServerAddr = properties.getProperty("NameServerAddr");
        if(nameServerAddr==null){
            logger.error("配置文件错误");
            return;
        }

        String mouseTopicName="MouseActions";
        String keyboardTopicName="KeyboardActions";
        String controlTopicName="ControlMessage";

        DefaultMQProducer mouseProducer = new DefaultMQProducer("MasterMouse");
        mouseProducer.setNamesrvAddr(nameServerAddr);
        mouseProducer.start();
        MouseEventHooker mouseEventHooker = new MouseEventHooker(mouseProducer, mouseTopicName);
        Thread mouseHookThread = new Thread(mouseEventHooker);
        mouseHookThread.start();

        DefaultMQProducer keyboardProducer = new DefaultMQProducer("MasterKeyboard");
        keyboardProducer.setNamesrvAddr(nameServerAddr);
        keyboardProducer.start();
        KeyboardEventHooker keyboardEventHooker = new KeyboardEventHooker(keyboardProducer, keyboardTopicName);
        Thread keyboardHookThread = new Thread(keyboardEventHooker);
        keyboardHookThread.start();


        //监听开启、关闭同步
        DefaultMQPushConsumer mouseConsumer = new DefaultMQPushConsumer("MasterControl");
        // 设置NameServer地址
        mouseConsumer.setNamesrvAddr(nameServerAddr);
        //订阅一个或多个topic，并指定tag过滤条件，这里指定*表示接收所有tag的消息
        try {
            mouseConsumer.subscribe(controlTopicName, "*");
        } catch (MQClientException e) {
            e.printStackTrace();
            return;
        }
        mouseConsumer.setAllocateMessageQueueStrategy(new AllocateMessageQueueAveragely());
        //注册回调接口来处理从Broker中收到的消息
        mouseConsumer.registerMessageListener(new MessageListenerOrderly() {
            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
                for (MessageExt msg : msgs) {
                    String tags = msg.getTags();
                    if(tags.equals("enable")){
                        enableRun=true;
                        logger.info("开启同步");
                    }else {
                        enableRun=false;
                        logger.info("关闭同步");
                    }
                }
                return ConsumeOrderlyStatus.SUCCESS;
            }
        });
        // 启动Consumer
        try {
            mouseConsumer.start();
            logger.info("Control Consumer Started");
        } catch (MQClientException e) {
            e.printStackTrace();
        }

    }
}