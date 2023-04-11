import EventCodes.KeyboardEvnetCodes;
import EventCodes.MouseEventCodes;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32VK;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.junit.jupiter.api.Test;

import java.awt.*;

public class testSendInput {
    User32 user32=User32.INSTANCE;

    @Test
    public void testKeybdInput(){
        //只能用下面这行来申请一个结构体数组，且必须直接操作input[i]内存，不要让input[i]=xxx;
        //因为JNA要求结构体数组的内存必须连续
        WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(2);
        inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
        inputs[0].input.setType("ki");
        inputs[0].input.ki.dwFlags=new WinDef.DWORD(KeyboardEvnetCodes.KEYEVENTF_KEYDOWN);
        inputs[0].input.ki.wVk= new WinDef.WORD(Win32VK.VK_0.code);

        inputs[1].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
        inputs[1].input.setType("ki");
        inputs[1].input.ki.dwFlags=new WinDef.DWORD(KeyboardEvnetCodes.KEYEVENTF_KEYUP);
        inputs[1].input.ki.wVk= new WinDef.WORD(Win32VK.VK_0.code);
        WinDef.DWORD dword = user32.SendInput(new WinDef.DWORD(2), inputs, inputs[0].size());
    }
    //测试Alt系统组合键
    @Test
    public void testComboKeybdInput(){
        WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(4);
        inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
        inputs[0].input.setType("ki");
        inputs[0].input.ki.dwFlags=new WinDef.DWORD(KeyboardEvnetCodes.KEYEVENTF_KEYDOWN);
        inputs[0].input.ki.wVk= new WinDef.WORD(Win32VK.VK_LMENU.code);

        inputs[1].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
        inputs[1].input.setType("ki");
        inputs[1].input.ki.dwFlags=new WinDef.DWORD(KeyboardEvnetCodes.KEYEVENTF_KEYDOWN);
        inputs[1].input.ki.wVk= new WinDef.WORD(Win32VK.VK_TAB.code);

        inputs[2].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
        inputs[2].input.setType("ki");
        inputs[2].input.ki.dwFlags=new WinDef.DWORD(KeyboardEvnetCodes.KEYEVENTF_KEYUP);
        inputs[2].input.ki.wVk= new WinDef.WORD(Win32VK.VK_TAB.code);

        inputs[3].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
        inputs[3].input.setType("ki");
        inputs[3].input.ki.dwFlags=new WinDef.DWORD(KeyboardEvnetCodes.KEYEVENTF_KEYUP);
        inputs[3].input.ki.wVk= new WinDef.WORD(Win32VK.VK_LMENU.code);
        WinDef.DWORD dword = user32.SendInput(new WinDef.DWORD(4), inputs, inputs[0].size());
    }
    @Test
    public void testMouseMoveClick(){
        user32.SetCursorPos(500,412);
        WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
        inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_MOUSE);
        inputs[0].input.setType("mi");
        inputs[0].input.mi.dwFlags=new WinDef.DWORD(MouseEventCodes.MOUSEEVENTF_RIGHTDOWN);
        user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());

        user32.SetCursorPos(500,412);
        inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_MOUSE);
        inputs[0].input.setType("mi");
        inputs[0].input.mi.dwFlags=new WinDef.DWORD(MouseEventCodes.MOUSEEVENTF_RIGHTUP);
        user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());
    }

    //必须要先SetCursorPos设置鼠标的屏幕分辨率坐标，不要用MouseMove的dx,dy，这两个值取值为（0，65535），不是屏幕分辨率坐标，
    //dx dy的坐标空间是(0,0) (65535,65535)
    //屏幕分辨率坐标空间是(0,0) (1920,1080)
    //这两个是不同的坐标空间！
    //如果用插值变换算法映射屏幕分辨率坐标空间的话，结果不怎么准！
    //Hook得到的是屏幕分辨率坐标，要统一坐标空间
    @Test
    public void testMouseWheelMove(){
        user32.SetCursorPos(500,412);
        WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
        inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_MOUSE);
        inputs[0].input.setType("mi");
        inputs[0].input.mi.mouseData=new WinDef.DWORD(240);
        inputs[0].input.mi.dwFlags=new WinDef.DWORD(MouseEventCodes.MOUSEEVENTF_WHEEL);
        user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());

    }
    @Test
    public void testScanCodeInputKeyDown(int key){
        WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
        inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
        inputs[0].input.setType("ki");
        inputs[0].input.ki.wVk=new WinDef.WORD(0);
        inputs[0].input.ki.wScan=new WinDef.WORD(key);
        inputs[0].input.ki.dwFlags=new WinDef.DWORD(KeyboardEvnetCodes.KEYEVENTF_KEYDOWN|KeyboardEvnetCodes.KEYEVENTF_SCANCODE);
        user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());
    }

    @Test
    public void testScanCodeInputKeyUp(int key){
        WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
        inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
        inputs[0].input.setType("ki");
        inputs[0].input.ki.wVk=new WinDef.WORD(0);
        inputs[0].input.ki.wScan=new WinDef.WORD(key);
        inputs[0].input.ki.dwFlags=new WinDef.DWORD(KeyboardEvnetCodes.KEYEVENTF_KEYUP|KeyboardEvnetCodes.KEYEVENTF_SCANCODE);
        user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());
    }
    @Test
    public void testScanCodeClick(){
       // WinUser.MAPVK_VK_TO_CHAR, WinUser.MAPVK_VK_TO_VSC, WinUser.MAPVK_VK_TO_VSC_EX, WinUser.MAPVK_VSC_TO_VK, WinUser.MAPVK_VSC_TO_VK_EX
        WinDef.HKL hkl = user32.GetKeyboardLayout(0);
        int i = user32.MapVirtualKeyEx(Win32VK.VK_0.code, WinUser.MAPVK_VK_TO_VSC, hkl);
        testScanCodeInputKeyDown(i);
        testScanCodeInputKeyUp(i);
    }

    //方向键的扫描码很特殊，它被映射为了0xE048，是一个小键盘扩展键
    //而0x48就是小键盘上8的ScanCode
    //即高字节加上0xE0就表示方向键的ScanCode
    //可以直接通过指定dwFlag包含KEYEVENTF_EXTENDEDKEY标志，则会自动给传入的WScan高字节加上0xE0
    //https://handmade.network/forums/articles/t/2823-keyboard_inputs_-_scancodes%252C_raw_input%252C_text_input%252C_key_names
    @Test
    public void testExtendedKey(){
        WinDef.HKL hkl = user32.GetKeyboardLayout(0);
        int key = user32.MapVirtualKeyEx(Win32VK.VK_UP.code, User32.MAPVK_VK_TO_VSC, hkl);
        WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
        inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
        inputs[0].input.setType("ki");
        inputs[0].input.ki.wVk=new WinDef.WORD(0);
        inputs[0].input.ki.wScan=new WinDef.WORD(key);



        inputs[0].input.ki.dwFlags=new WinDef.DWORD(KeyboardEvnetCodes.KEYEVENTF_KEYDOWN|KeyboardEvnetCodes.KEYEVENTF_SCANCODE
                |KeyboardEvnetCodes.KEYEVENTF_EXTENDEDKEY);
        user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());

        inputs[0].input.ki.dwFlags=new WinDef.DWORD(KeyboardEvnetCodes.KEYEVENTF_KEYUP|KeyboardEvnetCodes.KEYEVENTF_SCANCODE
                |KeyboardEvnetCodes.KEYEVENTF_EXTENDEDKEY);
      //  user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());
    }
}
