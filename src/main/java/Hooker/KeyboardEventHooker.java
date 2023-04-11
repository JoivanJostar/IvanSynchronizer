package Hooker;

import Client.DNFMaster;
import Entity.KeyboardEventMessage;
import EventCodes.KeyboardEvnetCodes;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyboardEventHooker extends EventHooker implements Runnable{

    public KeyboardEventHooker(DefaultMQProducer producer, String topicName) {
        super(producer, topicName);
    }

    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Keyboard Hook Thread listening");
        WinUser.LowLevelKeyboardProc lowLevelKeyboardProc = new WinUser.LowLevelKeyboardProc() {
            @Override
            public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wparam, WinUser.KBDLLHOOKSTRUCT lparam) {
                if(DNFMaster.enableRun==false || nCode<0){
                    return user32.CallNextHookEx(null,nCode,wparam, new WinDef.LPARAM(lparam.getPointer().getLong(0)));
                }
                int WMSG = wparam.intValue();
                KeyboardEventMessage keyboardMessage = new KeyboardEventMessage();
                String tag="";
                int vkCode = lparam.vkCode;
                switch (WMSG){
                    case WinUser.WM_KEYDOWN:{
                        tag="KeyDown";
                        keyboardMessage.setEventCode(KeyboardEvnetCodes.KEYEVENTF_KEYDOWN);
                        keyboardMessage.setVkCode(vkCode);
                        logger.debug("{} vkCode = 0x{}",tag,String.format("%x",vkCode));
                      //  System.out.printf("KeyDown 0x%x \n",vkCode);
                        break;
                    }
                    case WinUser.WM_KEYUP:{
                        tag="KeyUp";
                        logger.debug("{} vkCode = 0x{}",tag,String.format("%x",vkCode));
                        keyboardMessage.setEventCode(KeyboardEvnetCodes.KEYEVENTF_KEYUP);
                        keyboardMessage.setVkCode(vkCode);
                        break;
                    }
                    //对于Alt键(左alt:0xa4 右alt:0xa5)的按下，无法直接用WM_KEYDOWN处理 windows认为此时是系统键操作
                        //按下Alt后再按另一个键，即Alt+X组合键，此时X也被认为是系统键操作
                        //即按住Alt后的所有按键都是SystemKey
                    case WinUser.WM_SYSKEYDOWN:{
                        tag="WM_SYSKEYDOWN";
                        logger.debug("{} vkCode = 0x{}",tag,String.format("%x",vkCode));
                        keyboardMessage.setEventCode(KeyboardEvnetCodes.KEYEVENTF_KEYDOWN);
                        keyboardMessage.setVkCode(vkCode);
                        break;
                    }
                    //按下Alt再按下其他键X
                    //这种情况下再松开X的事件被windows处理为SystemKey up 但是alt键的松开却被当作普通的keyup事件处理
                    case WinUser.WM_SYSKEYUP:{
                        tag="WM_SYSKEYUP";
                        logger.debug("{} vkCode = 0x{}",tag,String.format("%x",vkCode));
                        keyboardMessage.setEventCode(KeyboardEvnetCodes.KEYEVENTF_KEYUP);
                        keyboardMessage.setVkCode(vkCode);
                        break;
                    }
                    default:
                        tag="unknown keyboard action";
                        break;
                }
                if(tag.equals("unknown keyboard action")){
                    logger.error("{} vkCode = 0x{}",tag,String.format("%x",vkCode));
                    return user32.CallNextHookEx(null,nCode, wparam,new WinDef.LPARAM(lparam.getPointer().getLong(0)));
                }
                if(producer!=null)
                sendRocketMQMessage(keyboardMessage, tag);
                return user32.CallNextHookEx(null,nCode, wparam,new WinDef.LPARAM(lparam.getPointer().getLong(0)));
            }

        };
        //添加钩子，返回钩子句柄hhook
        WinUser.HHOOK hhook = user32.SetWindowsHookEx(User32.WH_KEYBOARD_LL, lowLevelKeyboardProc, hmodule, 0);
        WinUser.MSG msg = new WinUser.MSG();
        int result=-1;
        //为当前线程开启windows消息循环,阻塞住当前线程并等待回调函数执行
        while ((result = user32.GetMessage(msg, null, 0, 0)) != 0) {
            if (result == -1) {
                System.err.println("error in get message");
                break;
            } else {
                System.out.println("got message");
                user32.TranslateMessage(msg);
                user32.DispatchMessage(msg);
            }
        }
        user32.UnhookWindowsHookEx(hhook);
    }


}
