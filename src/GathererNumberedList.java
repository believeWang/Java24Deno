import java.util.List;
import java.util.stream.Gatherer;

public class GathererNumberedList {
  // 自訂 Gatherer：為每個元素添加序號
  public static Gatherer<String, Integer, String> addIndexNumber() {
    return Gatherer.ofSequential(
        // Initializer: 初始化索引為 1（從 1 開始編號）
        () -> 1,
        // Integrator: 為當前元素添加序號並推送
        Gatherer.Integrator.ofGreedy(
            (state, element, downstream) -> {
              String numberedItem = state + ". " + element; // 拼接序號和元素
              downstream.push(numberedItem); // 推送結果
              state++; // 索引遞增
              return true; // 繼續處理
            }),
        // Finisher: 無需額外操作
        Gatherer.defaultFinisher());
  }

  public static void main(String[] args) {
    List<String> fruits = List.of("apple", "banana", "cherry");

    // 使用 Stream 和 Gatherer
    List<String> numberedFruits = fruits.stream().gather(addIndexNumber()).toList();

    System.out.println(numberedFruits); // 輸出: [1. apple, 2. banana, 3. cherry]
  }
}
