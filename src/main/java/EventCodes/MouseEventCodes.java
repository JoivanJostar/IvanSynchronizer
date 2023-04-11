package EventCodes;

//MouseInput Code 这些码是SendInput时需要传入的，不是Hook时用到的WM码,作为消息体的一部分发送到Slave
//详细介绍： https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-mouseinput
public interface MouseEventCodes {
    public final static int MOUSEEVENTF_MOVE =0x0001;
    public final static int MOUSEEVENTF_LEFTDOWN=0x0002;
    public final static int MOUSEEVENTF_LEFTUP=0x0004;
    public final static int MOUSEEVENTF_RIGHTDOWN=0x0008;
    public final static int MOUSEEVENTF_RIGHTUP=0x0010;
    public final static int MOUSEEVENTF_WHEEL=0x0800;
    public final static int MOUSEEVENTF_ABSOLUTE=0x8000;
    public final static int MOUSEEVENTF_VIRTUALDESK=0x4000;

}
