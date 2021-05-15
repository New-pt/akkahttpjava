import akka.event.Logging;
import akka.stream.Attributes;

public class Main {
  public static void main(String[] args){
    Attributes loglevels = Attributes.logLevels(Logging.InfoLevel(), Logging.WarningLevel(), Logging.DebugLevel());
  }
}
