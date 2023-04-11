package EventCodes;

//https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-keybdinput
public interface KeyboardEvnetCodes {
    public final static int KEYEVENTF_KEYDOWN =0x0000;
    public final static int KEYEVENTF_EXTENDEDKEY=0x0001;
    public final static int KEYEVENTF_KEYUP=0x0002;
    public final static int KEYEVENTF_SCANCODE=0x0008;
}
