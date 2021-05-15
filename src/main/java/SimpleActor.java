import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

public class SimpleActor {
  interface Message{

  }
  public static class SimpleMessage implements Message{
    private final ActorRef<SimpleServer.Entity> ref;

    private final int s;
    public SimpleMessage(int s, ActorRef<SimpleServer.Entity> ref){
      this.ref = ref;
      this.s = s;
    }
  }
  public static class Up implements Message{
    private final ActorRef<SimpleServer.Entity> ref;
    public Up(ActorRef<SimpleServer.Entity> ref){
      this.ref = ref;
    }
  }
  public static class Down implements Message{
    private final ActorRef<SimpleServer.Entity> ref;
    public Down(ActorRef<SimpleServer.Entity> ref){
      this.ref = ref;
    }
  }


  public static Behavior<Message> create(){
    return createReceive(0);
  }

  public static Behavior<Message> createReceive(int n){
    return Behaviors.receive(Message.class)
      .onMessage(SimpleMessage.class, SimpleActor::onSimpleMsg)
      .onMessage(Up.class, msg -> onUpMsg(n, msg))
      .onMessage(Down.class, msg -> onDownMsg(n, msg))
      .build();
  }
  private static Behavior<Message> onUpMsg(int n, Up msg){
    int newN = n + 1;
    msg.ref.tell(new SimpleServer.Entity("up", newN));
    return createReceive(newN);
  }
  private static Behavior<Message> onDownMsg(int n, Down msg){
    int newN = n - 1;
    msg.ref.tell(new SimpleServer.Entity("down", newN));

    return createReceive(newN);
  }
  private static Behavior<Message> onSimpleMsg(SimpleMessage msg){
    msg.ref.tell(new SimpleServer.Entity("blank", msg.s));
    return Behaviors.same();
  }
}
