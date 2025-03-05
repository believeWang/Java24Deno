import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

public class DistinctByKeyGatherer {
  // 泛型方法，接受一個鍵提取器 Function
  public static <T, K> Gatherer<T, Set<K>, T> distinctByKey(Function<? super T, ? extends K> keyExtractor) {
    return Gatherer.ofSequential(
        // Initializer: 創建一個 HashSet 來記錄已見過的鍵
        HashSet::new,
        // Integrator: 檢查鍵是否已存在，若否則加入並推送元素
        Gatherer.Integrator.ofGreedy((state, element, downstream) -> {
          K key = keyExtractor.apply(element); // 提取鍵
          if (state.add(key)) { // 如果鍵是新的，加入並推送
            downstream.push(element);
          }
          return true; // 繼續處理
        }),
        // Finisher: 這裡無需額外處理，使用默認
        Gatherer.defaultFinisher()
    );
  }

  // 測試用的簡單 Person 類
  static class Person {
    private final String name;
    private final int age;

    public Person(String name, int age) {
      this.name = name;
      this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    @Override
    public String toString() { return "Person{name='" + name + "', age=" + age + "}"; }
  }

  public static void main(String[] args) {
    // 測試數據
    Stream<Person> persons = Stream.of(
        new Person("Alice", 25),
        new Person("Bob", 30),
        new Person("Alice", 28), // 重複名字
        new Person("Charlie", 35)
    );

    // 使用 Gatherer 根據 name 去重
    var result = persons.gather(distinctByKey(Person::getName)).toList();
    System.out.println(result);
    // 輸出: [Person{name='Alice', age=25}, Person{name='Bob', age=30}, Person{name='Charlie', age=35}]
  }
}
