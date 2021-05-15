import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.common.EntityStreamingSupport;
import akka.http.javadsl.common.JsonEntityStreamingSupport;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;

import java.util.Random;

public class SimpleClient {
  private static final JsonEntityStreamingSupport jsonStream = EntityStreamingSupport.json();
  public static void main(String[] args){
    var system = ActorSystem.create("asd");
    var http = Http.get(system);

    var getEntities = HttpRequest.GET("http://localhost:8080/stream");

    var response = http.singleRequest(getEntities);
    var byteStrUnmarsh = Jackson.byteStringUnmarshaller(SimpleServer.Entity.class);

    response.thenCompose(
      r -> {
        var entity = HttpEntities.create(ContentTypes.APPLICATION_JSON, r.entity().getDataBytes().map(e -> json()));
        var postRequest = HttpRequest.POST("http://localhost:8080/stream").withEntity(entity);
        return http.singleRequest(postRequest);
      }
    ).thenAccept(
      r -> r.entity().withContentType(ContentTypes.APPLICATION_JSON).getDataBytes()
        .via(jsonStream.framingDecoder())
        .map(byteStr -> byteStrUnmarsh.unmarshal(byteStr, system))
        .to(Sink.foreach(future -> future.thenAccept(ent -> System.out.println("r0: entity: id - " + ent.getId() + ", val - "+ ent.getVal()))))
        .run(system)
    );
  }

  private static ByteString json(){
    var random = new Random();
    return ByteString.fromString(String.format("{\"id\":\"%s\",\"val\":%d}", "sad", random.nextInt()));
  }
}
