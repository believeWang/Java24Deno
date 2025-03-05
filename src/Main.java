import java.util.stream.Gatherers;
import java.util.stream.Stream;

public class Main {
  public static void main(String[] args) {
    System.out.println("SUM");
    Stream.of(1, 2, 3, 4).gather(Gatherers.fold(() -> 0, Integer::sum)).forEach(System.out::println);
    System.out.println("====");


    System.out.println("mapConcurrent");
    Stream.of(1, 2, 3).gather(Gatherers.mapConcurrent(2, x -> x * x)).forEach(System.out::println);
    System.out.println("====");
  }
}
