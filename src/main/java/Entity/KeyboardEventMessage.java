package Entity;

public class KeyboardEventMessage extends EventMessage{
    Integer vkCode;
    public KeyboardEventMessage() {
        super();
    }

    public Integer getVkCode() {
        return vkCode;
    }

    public void setVkCode(Integer vkCode) {
        this.vkCode = vkCode;
    }
}
