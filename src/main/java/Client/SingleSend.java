package Client;

import EventCodes.KeyboardEvnetCodes;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32VK;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

/**
 * 测试环境用的临时类，忽略
 */

@Deprecated
public class SingleSend {
    static User32  user32=User32.INSTANCE;
    public static void main(String[] args) throws InterruptedException {
        WinDef.HKL hkl = user32.GetKeyboardLayout(0);
        int i = user32.MapVirtualKeyEx(Win32VK.VK_U.code, WinUser.MAPVK_VK_TO_VSC, hkl);
        System.out.println("u的ScanCode="+i);
        Thread.sleep(5000);
        testScanCodeDown(Win32VK.VK_U.code);
       // Thread.sleep(500);
        testScanCodeUp(Win32VK.VK_U.code);
        System.out.println("u Click");

        Thread.sleep(2000);

        testScanCodeDown(Win32VK.VK_UP.code);
        System.out.println("Moving up");
        Thread.sleep(3000);
        testScanCodeUp(Win32VK.VK_UP.code);
        System.out.println("end");
    }


    public static void testScanCodeDown(int vkCode){
        WinDef.HKL hkl = user32.GetKeyboardLayout(0);
        int key = user32.MapVirtualKeyEx(vkCode, User32.MAPVK_VK_TO_VSC, hkl);
        WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
        inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
        inputs[0].input.setType("ki");
        inputs[0].input.ki.wVk=new WinDef.WORD(0);
        inputs[0].input.ki.wScan=new WinDef.WORD(key);
        int event=KeyboardEvnetCodes.KEYEVENTF_KEYDOWN|KeyboardEvnetCodes.KEYEVENTF_SCANCODE;
        if(vkCode== Win32VK.VK_UP.code||vkCode==Win32VK.VK_DOWN.code||vkCode==Win32VK.VK_LEFT.code||vkCode==Win32VK.VK_RIGHT.code){
            event=event|KeyboardEvnetCodes.KEYEVENTF_EXTENDEDKEY;
        }
        inputs[0].input.ki.dwFlags=new WinDef.DWORD(event);
        user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());
    }
    public static void testScanCodeUp(int vkCode){
        WinDef.HKL hkl = user32.GetKeyboardLayout(0);
        int key = user32.MapVirtualKeyEx(vkCode, User32.MAPVK_VK_TO_VSC, hkl);
        WinUser.INPUT[] inputs= (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
        inputs[0].type=new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD);
        inputs[0].input.setType("ki");
        inputs[0].input.ki.wVk=new WinDef.WORD(0);
        inputs[0].input.ki.wScan=new WinDef.WORD(key);
        int event=KeyboardEvnetCodes.KEYEVENTF_KEYUP|KeyboardEvnetCodes.KEYEVENTF_SCANCODE;
        if(vkCode== Win32VK.VK_UP.code||vkCode==Win32VK.VK_DOWN.code||vkCode==Win32VK.VK_LEFT.code||vkCode==Win32VK.VK_RIGHT.code){
            event=event|KeyboardEvnetCodes.KEYEVENTF_EXTENDEDKEY;
        }
        inputs[0].input.ki.dwFlags=new WinDef.DWORD(event);
        user32.SendInput(new WinDef.DWORD(1), inputs, inputs[0].size());
    }
}
