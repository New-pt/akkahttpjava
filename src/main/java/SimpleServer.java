import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.common.EntityStreamingSupport;
import akka.http.javadsl.common.JsonEntityStreamingSupport;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.Attributes;
import akka.stream.javadsl.Source;
import akka.stream.typed.javadsl.ActorFlow;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class SimpleServer extends AllDirectives{
  private final ActorRef<SimpleActor.Message> postStreamActor;
  private final Attributes loglevels;
  private final JsonEntityStreamingSupport jsonStream = EntityStreamingSupport.json();

  public SimpleServer(ActorRef<SimpleActor.Message> postStreamActor, Attributes loglevels){
    this.postStreamActor = postStreamActor;
    this.loglevels = loglevels;
  }

  public <T> CompletionStage<ServerBinding> start(ActorSystem<T> sys){
    return Http.get(sys).newServerAt("localhost", 8080).bind(this.createRoute());
  }

  private Route createRoute(){
    return path("stream", () ->
      concat(routeStreamGet(), routeStreamPost())
    );
  }

  private Route routeStreamGet(){
    var srcEntity = Source.repeat(new Entity("asd", 3)).take(50).throttle(1, Duration.ofSeconds(1));
    return get(() ->
        completeOKWithSource(srcEntity, Jackson.marshaller(), jsonStream)
    );
  }

  private Route routeStreamPost(){
    return post(() ->
      entityAsSourceOf(Jackson.byteStringUnmarshaller(Entity.class), jsonStream, src -> {
        var askFlow =
          ActorFlow.<Entity, SimpleActor.Message, Entity>ask(
            postStreamActor, Duration.ofSeconds(2), (i, r) -> {
              if(i.getVal() > 0)
                return new SimpleActor.Up(r);
              else if(i.getVal() < 0)
                return new SimpleActor.Down(r);
              else return new SimpleActor.SimpleMessage(i.getVal(), r);
            });
        var responseSrc =
          src
            .log("POST:/stream")
            .via(askFlow)
            .withAttributes(loglevels);
        return completeOKWithSource(responseSrc, Jackson.marshaller(), jsonStream);
      })
    );
  }


  public static class Entity{
    private String id;
    private int val;

    public Entity(){}
    
    public Entity(String id, int val){
      this.id = id;
      this.val = val;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public int getVal() {
      return val;
    }

    public void setVal(int val) {
      this.val = val;
    }

    @Override
    public String toString() {
      return "Entity{ " +
        "id='" + id + '\'' +
        ", val=" + val +
        '}';
    }
  }
}


