package Entity;
//消息实体对象
public class MouseMessage extends EventMessage{
    private Integer x; //cursor的屏幕像素空间xy坐标
    private Integer y;
    private Integer delta; //如果是滚轮 则是滚动数值

    public MouseMessage() {
        super();
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }
}
