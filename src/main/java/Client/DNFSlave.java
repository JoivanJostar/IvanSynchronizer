package Client;

import MessageConsumer.KeyboardMessageConsumer;
import MessageConsumer.MouseMessageConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * 从机
 */

public class DNFSlave {
    public static Logger logger=LoggerFactory.getLogger(DNFSlave.class);
    public static void main(String[] args) throws MQClientException {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("配置文件错误");
            return;
        }
        String nameServerAddr = properties.getProperty("NameServerAddr");
        String slaveName = properties.getProperty("SlaveName");
        if(nameServerAddr==null ||slaveName==null){
            logger.error("配置文件错误");
            return;
        }

        String groupName = slaveName;
        String mouseTopicName="MouseActions";//鼠标操作主题
        String keyboardTopicName="KeyboardActions";//键盘操作主题

        MouseMessageConsumer mouseMessageConsumer = new MouseMessageConsumer(groupName+"mouse", mouseTopicName,nameServerAddr);
        Thread mouseConsumerThread = new Thread(mouseMessageConsumer);
        mouseConsumerThread.start();
        logger.info("[Group={}] Mouse Consumer Thread Started",groupName+"mouse");


        KeyboardMessageConsumer keyboardMessageConsumer = new KeyboardMessageConsumer(groupName+"keyboard", keyboardTopicName,nameServerAddr);
        Thread keyboardConsumerThread = new Thread(keyboardMessageConsumer);
        keyboardConsumerThread.start();
        logger.info("[Group={}] Keyboard Consumer Thread Started",groupName+"keyboard");
    }
}
