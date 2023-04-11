package Hooker;

import Client.DNFMaster;
import Entity.MouseMessage;
import EventCodes.MouseEventCodes;
import com.sun.jna.platform.win32.*;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MouseEventHooker extends EventHooker implements Runnable{
    //我不明白为什么JNA没有定义鼠标输入相关的WinMessage，这些是我直接从MSDN上查的win32API里的定义：
    //https://learn.microsoft.com/en-us/windows/win32/inputdev/mouse-input-notifications
    private final static int WM_MOUSEMOVE =0x0200;
    private final static int WM_LBUTTONDOWN=0x0201;
    private final static int WM_LBUTTONUP=0x0202;
    private final static int WM_RBUTTONDOWN=0x0204;
    private final static int WM_RBUTTONUP=0x0205;
    private final static int WM_MOUSEWHEEL=0x020A;


    public MouseEventHooker(DefaultMQProducer producer, String topicName){
        super(producer,topicName);
    }

    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Mouse Event Hooker listening");
        //鼠标Hook
        WinUser.HOOKPROC mouseHookProc = new WinUser.LowLevelMouseProc(){
            //编写回调函数，完成监听和事件packet发送
            //这是操作系统级别的中断，会直接打断/唤醒当前Hooker线程去执行回调函数
            @Override
            public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wparam, WinUser.MSLLHOOKSTRUCT lparam) {
                if(DNFMaster.enableRun==false || nCode<0){
                    return user32.CallNextHookEx(null,nCode,wparam, new WinDef.LPARAM(lparam.getPointer().getLong(0)));
                }
                int WMSG = wparam.intValue();
                MouseMessage mouseMessage = new MouseMessage();
                String tag="";
                switch (WMSG){
                    case WM_LBUTTONDOWN:{
                        //System.out.println("左键按下");
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_LEFTDOWN);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        tag="LeftButtonDown";
                        logger.debug("{} x = {}; y = {}",tag,mouseMessage.getX(),mouseMessage.getY());
                        break;
                    }
                    case WM_LBUTTONUP:{
                       // System.out.println("左键抬起");
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_LEFTUP);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        tag="LeftButtonUp";
                        logger.debug("{} x = {}; y = {}",tag,mouseMessage.getX(),mouseMessage.getY());
                        break;
                    }
                    case WM_RBUTTONDOWN:{
                        //System.out.println("右键按下");
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_RIGHTDOWN);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        tag="RightButtonDown";
                        logger.debug("{} x = {}; y = {}",tag,mouseMessage.getX(),mouseMessage.getY());
                        break;
                    }
                    case WM_RBUTTONUP:{
                        //System.out.println("右键抬起");
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_RIGHTUP);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        tag="RightButtonUp";
                        logger.debug("{} x = {}; y = {}",tag,mouseMessage.getX(),mouseMessage.getY());
                        break;
                    }
                    //鼠标移动事件可以忽略，减少峰值流量
                    case WM_MOUSEMOVE:{
                       // System.out.println("鼠标移动"+ "x="+lparam.pt.x+" y="+lparam.pt.y);
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_MOVE);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        tag="MouseMove";
                        break;
                    }
                    case WM_MOUSEWHEEL:{
                        //System.out.println("滚轮");
                        /*
                        If the message is WM_MOUSEWHEEL, the high-order word of this member is the wheel delta.
                        The low-order word is reserved. A positive value indicates that the wheel was rotated forward,
                        away from the user; a negative value indicates that the wheel was rotated backward, toward the user.
                         One wheel click is defined as WHEEL_DELTA, which is 120.
                         https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-msllhookstruct
                         */
                        //获取滚轮滚动值delta，一个整数，以120位单位进行滚动，正数表示向上滚，负数表示向下滚
                        int delta = lparam.mouseData >> 16;
                        // System.out.println(delta);
                        mouseMessage.setEventCode(MouseEventCodes.MOUSEEVENTF_WHEEL);
                        mouseMessage.setX(lparam.pt.x);
                        mouseMessage.setY(lparam.pt.y);
                        mouseMessage.setDelta(delta);
                        tag="WheelMove";
                        logger.debug("{} x = {}; y = {} delta={}",tag,mouseMessage.getX(),mouseMessage.getY(),delta);
                        break;
                    }
                    default:
                        tag="unknown mouse action";
                }
                if(tag.equals("unknown mouse action")){
                    logger.error("{} WM={}",tag,WMSG);
                    return user32.CallNextHookEx(null,nCode, wparam,new WinDef.LPARAM(lparam.getPointer().getLong(0)));
                }
                if( producer!=null)
                sendRocketMQMessage(mouseMessage,tag);
                return user32.CallNextHookEx(null,nCode, wparam,new WinDef.LPARAM(lparam.getPointer().getLong(0)));
            }
        };
        //添加鼠标钩子，返回钩子句柄hhook
        WinUser.HHOOK hhook = user32.SetWindowsHookEx(User32.WH_MOUSE_LL, mouseHookProc, hmodule, 0);
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
