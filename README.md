# Java Stream Gatherer

# 1. 背景與動機

Java 的 Stream API 自 Java 8 引入以來，提供了強大的數據處理能力，讓開發者可以用函數式的方式操作集合數據。Stream 管道由三部分組成：

1. **來源（Source）**：如集合或陣列。
2. **中間操作（Intermediate Operations）**：如 `map`、`filter`、`flatMap` 等，用於轉換或過濾數據。
3. **終端操作（Terminal Operations）**：如 `collect`、`reduce`、`forEach` 等，產生最終結果。

然而，雖然 Stream API 提供了許多內建中間操作，但有些複雜的數據處理需求（例如窗口處理、摺疊、條件轉換等）無法直接通過現有操作實現。開發者往往需要使用笨拙的解決方案，比如中間集合或複雜的
`Collector`，這降低了代碼的可讀性和效率。

為了解決這個問題，Java 引入了 **Stream Gatherers**，作為一種可擴展的機制，讓開發者可以自訂中間操作，從而增強 Stream API
的靈活性和表現力。這類似於終端操作中的 `Collector`，但專注於中間階段的數據轉換。

# 2. 什麼是 Stream Gatherers？

**Stream Gatherers** 是一種新的中間操作 API，允許開發者在 Stream 管道中插入自訂的轉換邏輯。它通過
`Stream.gather(Gatherer<T, A, R>)` 方法實現，其中：

- **T**：輸入流的元素類型。
- **A**：Gatherer 內部維護的狀態類型（可選）。
- **R**：輸出流的元素類型。

Gatherers 的核心思想是將輸入流中的元素轉換為輸出流中的元素，並支持以下特性：

1. **多樣化的轉換**
    - 支援一對一、一對多、多對一或多對多的元素轉換。

2. **狀態管理**
    - 可以記錄先前處理的元素，影響後續元素的轉換。

3. **短路處理**
    - 可以在特定條件下提前終止流處理（類似 `limit` 或 `takeWhile`）。

4. **並行支持**
    - 可以選擇性地支援並行處理。

# 3. Gatherer 的工作原理

**Gatherers** 的運作類似於一個“數據處理器”，它從上游（輸入流）接收元素，根據自訂邏輯進行轉換，並將結果推送至下游（輸出流）。以下是其工作流程：

1. **初始化**
    - 如果提供了 `Initializer`，則創建一個狀態對象（例如一個列表或計數器）。

2. **整合**
    - `Integrator` 逐一處理輸入元素，可能更新狀態並決定是否向下游推送元素。
    - 它返回一個布林值：`false` 表示短路（停止處理）。

3. **合併**
    - 在並行執行時，`Combiner` 將多個線程的狀態合併。

4. **結束**
    - 流結束後，`Finisher` 可執行最終操作，例如推送剩餘數據。

---

### 特性與優勢

- **動態數據轉換**：Gatherers 的輸出是一個新的 Stream，而不是單一結果。
- **適用於中間操作**：其設計類似於 `Collector`，但更加靈活，專注於中間階段的處理。

這使得 Gatherers 特別適用於需要複雜數據轉換的場景。

# 4. **內建 Gatherers**

Java 24 在 java.util.stream.Gatherers 類中提供了一些實用的內建 Gatherer，方便開發者直接使用：

* **fold**：將流元素逐步摺疊為單一結果，並在結束時輸出。
    * 示例：`Stream.of(1, 2, 3, 4).gather(Gatherers.fold(() -> 0, Integer::sum)).toList() → [10]`。
  

* **mapConcurrent**：並行映射元素，使用虛擬線程處理，保持順序。
    * 示例：`Stream.of(1, 2, 3).gather(Gatherers.mapConcurrent(2, x -> x * x)).toList() → [1, 4, 9]`。

* **scan**：執行前綴掃描，逐步累積結果並立即輸出。
    * 示例：`Stream.of(1, 2, 3).gather(Gatherers.scan(() -> 0, Integer::sum)).toList() → [1, 3, 6]`。
  
* **windowFixed**：將流分成固定大小的窗口。
    * 示例：`Stream.of(1, 2, 3, 4).gather(Gatherers.windowFixed(2)).toList() → [[1, 2], [3, 4]]`。
  
* **windowSliding**：生成滑動窗口，每個窗口包含前一窗口的部分元素和新元素。
    * 示例：`Stream.of(1, 2, 3).gather(Gatherers.windowSliding(2)).toList() → [[1, 2], [2, 3]]`。
      
  這些內建 Gatherers 解決了常見的數據處理需求，同時展示了自訂 Gatherers 的潛力。
  

---

### Gatherers 的關鍵組成函數

Gatherers 由四個關鍵函數組成（部分為可選），這些函數共同定義了轉換邏輯：

1. **Initializer（初始化器）**（可選）
    - 提供初始狀態。
    - 類型：`Supplier<A>`。

2. **Integrator（整合器）**（必需）
    - 定義如何處理每個輸入元素並生成輸出。
    - 類型：`Integrator<A, T, R>`。

3. **Combiner（合併器）**（可選）
    - 用於並行處理時合併多個狀態。
    - 類型：`BinaryOperator<A>`。

4. **Finisher（結束器）**（可選）
    - 在流結束時執行最終處理。
    - 類型：`BiConsumer<A, Downstream<? super R>>`。

# Java Stream Gatherer 工作原理：四個角色的詳細說明


## Gatherer 的整體工作原理

Gatherer 是 Java Stream API 中的一種中間操作，通過 `Stream.gather(Gatherer<T, A, R>)` 方法將輸入流（類型為 `T`）轉換為輸出流（類型為
`R`），並允許自訂轉換邏輯。其核心思想是像一個“數據處理器”：

- 從上游（輸入流）接收元素。
- 根據自訂邏輯處理這些元素（可能維護內部狀態 `A`）。
- 將結果推送至下游（輸出流）。

Gatherer 的四個角色共同實現這個處理過程，每個角色有特定的職責，且有些是可選的。以下是詳細介紹：

---

## 1. Initializer（初始化器）

- **類型**：`Supplier<A>`
- **職責**：提供 Gatherer 的初始狀態（`A`），在流處理開始前執行一次。
- **是否必需**：可選。如果未提供，則默認為無狀態（`A` 為 `null`）。
- **執行時機**：在流開始處理第一個元素之前，且僅執行一次（即使是並行流，每個線程也會獨立調用）。

### 作用

Initializer 負責設置 Gatherer 的內部狀態，例如創建一個集合、計數器或其他數據結構，用於後續的元素處理。如果你的 Gatherer
不需要狀態（例如簡單映射），可以省略。

### 示例

假設我們要實現一個累加器，計算流中所有數字的和，並在結束時輸出：

```java
Supplier<Integer> initializer = () -> 0; // 初始狀態為 0
```

# 2. Integrator（整合器）

- **類型**：`Gatherer.Integrator<A, T, R>`  
- **職責**：定義如何處理每個輸入元素（`T`），更新狀態（`A`），並決定是否將結果（`R`）推送至下游。  
- **是否必需**：必需。`Integrator` 是 Gatherer 的核心邏輯。  
- **執行時機**：對流中的每個元素執行一次（按順序或並行執行）。  

---

示例
```java
interface Integrator<A, T, R> {
    boolean integrate(A state, T element, Downstream<? super R> downstream);
}

```
* state：當前狀態（由 Initializer 提供或先前更新）。
* element：當前處理的輸入元素。
* downstream：下游對象，用於推送結果（downstream.push(R)）。
* 返回值 boolean：true 表示繼續處理，false 表示短路（停止處理後續元素，類似 limit）。

# 3. Combiner（合併器）
* **類型**：BinaryOperator (即 BiFunction<A, A, A>)
* **職責**：在並行流中，合併多個線程的狀態（A），確保最終結果一致。
* **是否必需**：可選。如果未提供，Gatherer 默認只支援順序執行（ofSequential）。
* **執行時機**：僅在並行流中執行，在各線程完成後合併狀態。

作用
Combiner 是為並行處理設計的，解決多線程狀態的合併問題。例如，如果每個線程獨立維護一個局部狀態（如計數器或集合），Combiner 將這些狀態合併為一個整體狀態。

示例
假設我們用 Set 記錄唯一元素，在並行流中：

```java
BinaryOperator<Set<Integer>> combiner = (set1, set2) -> { 
   set1.addAll(set2); // 合併兩個 Set 
   return set1; 
};
```
如果不支援並行，應使用 Gatherer.ofSequential，省略 Combiner。

# 4. Finisher（結束器）
* **類型**：BiConsumer<A, Downstream<? super R>>
* **職責**：在流處理結束後，根據最終狀態（A）執行最後的操作，例如推送最終結果至下游。
* **是否必需**：可選。如果未提供，則使用 Gatherer.defaultFinisher()（不執行任何操作）。
* **執行時機**：在所有元素處理完畢後執行一次（並行流中在 Combiner 之後）。

作用
Finisher 提供了一個清理或總結的機會，特別適合需要延遲推送結果的場景（例如累加後輸出總和）。

示例
完成累加器，將最終和推送至下游：

```java
BiConsumer<Integer, Downstream<? super Integer>> finisher = (state, downstream) -> {
   downstream.push(state); // 推送最終累加結果
};
```
