package MessageConsumer;


import Entity.MouseMessage;
import EventCodes.MouseEventCodes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MouseMessageConsumer implements Runnable{
    private String nameservAddr;
    private String groupName;
    private String topicName;
    private ObjectMapper objectMapper = new ObjectMapper();
    private User32 user32=User32.INSTANCE;
    private Logger logger= LoggerFactory.getLogger(MouseMessageConsumer.class);
    public MouseMessageConsumer(String groupName, String topicName,String nameservAddr) {
        this.groupName = groupName;
        this.topicName = topicName;
        this.nameservAddr=nameservAddr;
    }

    @Override
    public void run() {
        DefaultMQPushConsumer mouseConsumer = new DefaultMQPushConsumer(groupName);
        // 设置NameServer地址
        mouseConsumer.setNamesrvAddr(nameservAddr);
        //订阅一个或多个topic，并指定tag过滤条件，这里指定*表示接收所有tag的消息
        try {
            mouseConsumer.subscribe(topicName, "*");
        } catch (MQClientException e) {
            e.printStackTrace();
            return;
        }
        mouseConsumer.setAllocateMessageQueueStrategy(new AllocateMessageQueueAveragely());
        //注册回调接口来处理从Broker中收到的消息
        mouseConsumer.registerMessageListener(new MouseMessageListener());
        // 启动Consumer
        try {
            mouseConsumer.start();
            logger.info("Mouse Consumer Started");
        } catch (MQClientException e) {
            e.printStackTrace();
        }
    }

    private class MouseMessageListener implements MessageListenerOrderly {
        @Override
        public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
            //处理消息
            for (MessageExt msg : msgs) {
                logger.debug("recv mouse msg");
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                try {
                    //消息反序列化
                    MouseMessage mouseMessage = objectMapper.readValue(body, MouseMessage.class);
                    Integer eventCode = mouseMessage.getEventCode();
                    switch (eventCode){
                        case MouseEventCodes.MOUSEEVENTF_MOVE:
                        case MouseEventCodes.MOUSEEVENTF_LEFTDOWN:
                        case MouseEventCodes.MOUSEEVENTF_LEFTUP:
                        case MouseEventCodes.MOUSEEVENTF_RIGHTDOWN:
                        case MouseEventCodes.MOUSEEVENTF_RIGHTUP:
                        {
                            Integer x = mouseMessage.getX();
                            Integer y = mouseMessage.getY();
                            user32.SetCursorPos(x,y);//设置鼠标坐标到屏幕对应位置
                            WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
                            inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_MOUSE);
                            inputs[0].input.setType("mi");
                            inputs[0].input.mi.dwFlags=new WinDef.DWORD(eventCode);
                            user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());

                            break;
                        }
                        case MouseEventCodes.MOUSEEVENTF_WHEEL:{
                            Integer x = mouseMessage.getX();
                            Integer y = mouseMessage.getY();
                            Integer delta = mouseMessage.getDelta();
                            user32.SetCursorPos(x,y);
                            WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
                            inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_MOUSE);
                            inputs[0].input.setType("mi");
                            inputs[0].input.mi.mouseData=new WinDef.DWORD(delta);
                            inputs[0].input.mi.dwFlags=new WinDef.DWORD(eventCode);
                            user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());
                            break;
                        }
                        default:{
                            logger.error("unknown mouse event code {}",eventCode);
                            break;
                        }
                    }
                } catch (JsonProcessingException e) {
                    logger.error("MouseMessage反序列化失败 body={}",body);
                    e.printStackTrace();
                }
            }
            return ConsumeOrderlyStatus.SUCCESS;
        }
    }
}
