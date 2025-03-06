import java.util.stream.Gatherers;
import java.util.stream.Stream;

public class DefaultGatherer {
  public static void main(String[] args) {
    System.out.println("SUM");
    Stream.of(1, 2, 3, 4).gather(Gatherers.fold(() -> 0, Integer::sum)).forEach(System.out::println);
    System.out.println("====");


    System.out.println("mapConcurrent");
    System.out.println(Stream.of(1, 2, 3).gather(Gatherers.mapConcurrent(2, x -> x * x)).toList());
    System.out.println("====");

    System.out.println("scan"); // 銀行餘額顯示
    System.out.println(Stream.of(1, 2, 3).gather(Gatherers.scan(() -> 0, Integer::sum)).toList());
    System.out.println("====");

    System.out.println("windowFixed");// 用來切割sqs 訊息
    System.out.println( Stream.of(1, 2, 3, 4).gather(Gatherers.windowFixed(2)).toList());
    System.out.println("====");

    System.out.println("windowSliding"); //平均三天的溫度
    System.out.println( Stream.of(1, 2, 3).gather(Gatherers.windowSliding(2)).toList());
    System.out.println("====");




  }
}
