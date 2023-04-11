import Client.DNFMaster;
import Hooker.KeyboardEventHooker;
import org.junit.jupiter.api.Test;

public class testKeyboardHooker {
    @Test
    public void test() throws InterruptedException {
        KeyboardEventHooker keyboardEventHooker = new KeyboardEventHooker(null, "");
        DNFMaster.enableRun=true;
        Thread keyboardHookThread = new Thread(keyboardEventHooker);
        keyboardHookThread.start();
        System.out.println("keyboard Hook Thread started");
        Thread.sleep(10000000);
    }
}
