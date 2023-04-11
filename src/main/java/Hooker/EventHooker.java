package Hooker;

import Entity.EventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class EventHooker {
   protected DefaultMQProducer producer=null;
   protected String topicName="";
   protected User32 user32=User32.INSTANCE;
   protected WinDef.HMODULE hmodule= Kernel32.INSTANCE.GetModuleHandle(null);  //获取当前模块的句柄
   protected ObjectMapper objectMapper = new ObjectMapper(); //jacson mapper 用于消息序列化

    public EventHooker(DefaultMQProducer producer, String topicName) {
        this.producer = producer;
        this.topicName = topicName;
    }
    protected void sendRocketMQMessage(EventMessage eventMessage, String tag) {
        String jsonMsgBody=null;
        try {
            jsonMsgBody=objectMapper.writeValueAsString(eventMessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.out.println("Entity2Json转换失败");
        }
        if(jsonMsgBody!=null){
            //创建消息
            Message message = new Message(topicName, tag,jsonMsgBody.getBytes(StandardCharsets.UTF_8));
            try {
                SendResult sendResult = producer.send(message, new MessageQueueSelector() {
                    @Override
                    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                        Integer id = (Integer) arg;//arg=0 默认只将键鼠操作消息发送到队列0，且这个Topic只设置一个队列
                        int index = id % mqs.size();
                        return mqs.get(index);
                    }
                }, 0);
            } catch (MQClientException e) {
                e.printStackTrace();
            } catch (RemotingException e) {
                e.printStackTrace();
            } catch (MQBrokerException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
