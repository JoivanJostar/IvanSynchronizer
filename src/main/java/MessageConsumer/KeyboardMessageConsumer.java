package MessageConsumer;

import Entity.KeyboardEventMessage;
import EventCodes.KeyboardEvnetCodes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32VK;
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

public class KeyboardMessageConsumer implements Runnable{
    private String groupName;
    private String topicName;
    private ObjectMapper objectMapper = new ObjectMapper();
    private User32 user32=User32.INSTANCE;
    private Logger logger= LoggerFactory.getLogger(MouseMessageConsumer.class);
    private String nameservAddr;

    public KeyboardMessageConsumer(String groupName, String topicName,String nameservAddr) {
        this.groupName = groupName;
        this.topicName = topicName;
        this.nameservAddr=nameservAddr;
    }
    @Override
    public void run() {
        DefaultMQPushConsumer keyboardConsumer = new DefaultMQPushConsumer(groupName);
        // 设置NameServer地址
        keyboardConsumer.setNamesrvAddr(nameservAddr);
        //订阅一个或多个topic，并指定tag过滤条件，这里指定*表示接收所有tag的消息
        try {
            keyboardConsumer.subscribe(topicName, "*");
        } catch (MQClientException e) {
            e.printStackTrace();
            return;
        }
        keyboardConsumer.setAllocateMessageQueueStrategy(new AllocateMessageQueueAveragely());
        //注册回调接口来处理从Broker中收到的消息
        keyboardConsumer.registerMessageListener(new KeyboardMessageConsumer.KeyboardMessageListener());
        // 启动Consumer
        try {
            keyboardConsumer.start();
            logger.info("Keyboard Consumer Started");
        } catch (MQClientException e) {
            e.printStackTrace();
        }
    }
    private class KeyboardMessageListener implements MessageListenerOrderly{
        @Override
        public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
            for (MessageExt msg : msgs) {
                logger.debug("recv keyborad msg");
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                try {
                    KeyboardEventMessage keyboardEventMessage = objectMapper.readValue(body, KeyboardEventMessage.class);
                    Integer eventCode = keyboardEventMessage.getEventCode();
                    WinDef.HKL hkl = user32.GetKeyboardLayout(0);

                    //发送SendInput按键必须用ScanCode即硬件扫描码，DNF这种游戏不用VK事件逻辑
                    switch (eventCode){
                        case KeyboardEvnetCodes.KEYEVENTF_KEYDOWN:{
                            Integer vkCode = keyboardEventMessage.getVkCode();
                            WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
                            inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
                            inputs[0].input.setType("ki");
                            inputs[0].input.ki.wVk=new WinDef.WORD(0);
                            int scanCode = user32.MapVirtualKeyEx(vkCode, User32.MAPVK_VK_TO_VSC, hkl);
                            inputs[0].input.ki.wScan=new WinDef.WORD(scanCode);
                            eventCode=eventCode|KeyboardEvnetCodes.KEYEVENTF_SCANCODE;
                            if(vkCode== Win32VK.VK_UP.code||vkCode==Win32VK.VK_DOWN.code||vkCode==Win32VK.VK_LEFT.code||vkCode==Win32VK.VK_RIGHT.code){
                                eventCode=eventCode|KeyboardEvnetCodes.KEYEVENTF_EXTENDEDKEY;
                                logger.debug("Extended ScanCode");
                            }
                            inputs[0].input.ki.dwFlags=new WinDef.DWORD(eventCode);
                            user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());
                            logger.debug("Scan Code 0x{} KeyDown",String.format("%x",scanCode));
                            break;
                        }
                        case KeyboardEvnetCodes.KEYEVENTF_KEYUP:{
                            Integer vkCode = keyboardEventMessage.getVkCode();
                            WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
                            inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
                            inputs[0].input.setType("ki");
                            inputs[0].input.ki.wVk=new WinDef.WORD(0);
                            int scanCode = user32.MapVirtualKeyEx(vkCode, User32.MAPVK_VK_TO_VSC, hkl);
                            inputs[0].input.ki.wScan=new WinDef.WORD(scanCode);
                            eventCode=eventCode|KeyboardEvnetCodes.KEYEVENTF_SCANCODE;
                            if(vkCode== Win32VK.VK_UP.code||vkCode==Win32VK.VK_DOWN.code||vkCode==Win32VK.VK_LEFT.code||vkCode==Win32VK.VK_RIGHT.code){
                                eventCode=eventCode|KeyboardEvnetCodes.KEYEVENTF_EXTENDEDKEY;
                                logger.debug("Extended ScanCode");
                            }
                            inputs[0].input.ki.dwFlags=new WinDef.DWORD(eventCode);
                            user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());
                            logger.debug("Scan Code 0x{} KeyUp",String.format("%x",scanCode));
                            break;
                        }
                        default:{
                            logger.error("unknown keyboard event code {}",eventCode);
                            break;
                        }
                    }

                } catch (JsonProcessingException e) {
                    logger.error("keyboardEventMessage body={}",body);
                    e.printStackTrace();
                }
            }
            return ConsumeOrderlyStatus.SUCCESS;
        }
    }
}
