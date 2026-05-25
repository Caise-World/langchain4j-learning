# Java 并发编程：JUC 核心组件

## 一、CyclicBarrier vs CountDownLatch

### 1. CountDownLatch（倒计时门闩）

允许一个或多个线程等待其他线程完成操作。

```java
public class Worker implements Runnable {
    private final CountDownLatch latch;
    
    public Worker(CountDownLatch latch) {
        this.latch = latch;
    }
    
    @Override
    public void run() {
        try {
            System.out.println("开始处理任务...");
            Thread.sleep(1000);
            System.out.println("任务完成");
            latch.countDown();  // 计数 -1
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// 使用示例
CountDownLatch latch = new CountDownLatch(3);
new Thread(new Worker(latch)).start();
new Thread(new Worker(latch)).start();
new Thread(new Worker(latch)).start();

latch.await();  // 等待计数归零
System.out.println("所有任务完成");
```

**核心特点：**
- 计数递减，只能使用一次
- 计数为 0 时门闩打开
- 不可重用

### 2. CyclicBarrier（循环栅栏）

让一组线程相互等待，直到所有线程都到达某个点后一起继续执行。

```java
public class BarrierWorker implements Runnable {
    private final CyclicBarrier barrier;
    
    public BarrierWorker(CyclicBarrier barrier) {
        this.barrier = barrier;
    }
    
    @Override
    public void run() {
        try {
            System.out.println("线程 " + Thread.currentThread().getName() + " 到达屏障");
            barrier.await();  // 等待其他线程
            System.out.println("所有线程到达，继续执行");
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// 使用示例
CyclicBarrier barrier = new CyclicBarrier(3);
new Thread(new BarrierWorker(barrier), "T1").start();
new Thread(new BarrierWorker(barrier), "T2").start();
new Thread(new BarrierWorker(barrier), "T3").start();
```

**核心特点：**
- 可重用（循环）
- 等待所有线程到达
- 适用于多阶段任务

### 3. 核心区别对比

| 特性 | CountDownLatch | CyclicBarrier |
|------|---------------|---------------|
| 计数方向 | 递减 | 递增 |
| 重复使用 | 不可重用 | 可重用（循环） |
| 等待方式 | 线程等待计数归零 | 所有线程相互等待 |
| 典型场景 | 主线程等待子线程 | 线程间相互等待 |
| 栅栏打破 | 不会 | 可能（超时或中断） |

### 4. 选择建议

**使用 CountDownLatch 当：**
- 一个线程需要等待多个其他线程完成
- 任务是一次性的
- 主从模式

**使用 CyclicBarrier 当：**
- 多个线程需要相互等待
- 有多个阶段的任务
- 需要复用

## 二、ConcurrentHashMap（JDK 1.7 vs 1.8）

### 1. JDK 1.7：Segment 分段锁

```java
// JDK 1.7 结构
ConcurrentHashMap<K, V> {
    Segment<K, V>[] segments;  // 分段数组
    static final int DEFAULT_CONCURRENCY_LEVEL = 16;
}

Segment<K, V> {
    volatile HashEntry<K, V>[] table;
    final ReentrantLock lock;
}
```

**特点：**
- 16 个 Segment，分段加锁
- 每个 Segment 独立锁
- 锁粒度：Segment 级别

### 2. JDK 1.8：CAS + Synchronized

```java
// JDK 1.8 结构
ConcurrentHashMap<K, V> {
    transient volatile Node<K, V>[] table;
    transient volatile int sizeCtl;
}
```

**优化：**
- 取消 Segment，直接用 Node 数组
- 使用 CAS 操作更新
- synchronized 锁住头节点（解决 hash 冲突）
- 红黑树优化（链表过长时）

### 3. size() 方法的原子性保证

```java
// JDK 1.8 实现
public int size() {
    if (c < 0) {
        if (c == -1) {
            return baseCount;
        }
        return -c;
    }
    if (casBaseCount(c = baseCount, c + 1)) {
        return check() ? baseCount : sumCount();
    }
    return sumCount();
}
```

## 三、HashMap JDK 1.8 红黑树优化

### 1. 为什么引入红黑树

JDK 1.7 及之前，HashMap 使用链表解决 hash 冲突：

```
[桶0] -> [Node1] -> [Node2] -> [Node3] -> ...
         (链表查找 O(n))
```

**问题：**
- 当 hash 冲突严重时，链表过长
- 查找复杂度退化为 O(n)
- 性能下降

**解决方案：**
- JDK 1.8 引入红黑树（自平衡二叉查找树）
- 查找复杂度稳定在 O(log n)

### 2. 红黑树转换条件

```java
// 链表转红黑树的阈值
static final int TREEIFY_THRESHOLD = 8;  // 链表长度 >= 8

// 红黑树转链表的阈值
static final int UNTREEIFY_THRESHOLD = 6;  // 红黑树节点数 <= 6
```

**触发流程：**
```
put(key, value)
    ↓
计算 hash，找到桶位置
    ↓
桶为空 → 直接插入
    ↓
桶已有元素 → 判断是链表还是红黑树
    ↓
链表 → 遍历查找/插入，长度达到 8 → 转红黑树
    ↓
红黑树 → TreeNode 插入
```

### 3. 扩容时红黑树处理

```java
// JDK 1.8 扩容逻辑
if (oldTab != null) {
    for (int j = 0; j < oldCap; ++j) {
        Node<K, V> e = oldTab[j];
        if ((e = oldTab[j]) != null) {
            oldTab[j] = null;
            if (e.next == null) {
                newTab[e.hash & (newCap - 1)] = e;
            } else if (e instanceof TreeNode) {
                // 红黑树拆分
                ((TreeNode<K, V>) e).split(this, newTab, j, oldCap);
            } else {
                // 链表拆分
                Node<K, V> loHead = null, loTail = null;
                Node<K, V> hiHead = null, hiTail = null;
                // ... 逻辑保持
            }
        }
    }
}
```

### 4. 树化阈值详解

**为什么是 8？**
- 链表长度 8 时，红黑树和链表的平均查找时间接近
- 红黑树节点对象比链表节点大（TreeNode vs Node）
- 避免在临界点频繁转换

**计算公式：**
```
链表查找时间 = n/2 (平均)
红黑树查找时间 = log2(n)

n/2 ≈ log2(n) 
n ≈ 2*log2(n)
当 n = 8 时，两种结构性能相当
```

## 四、CompletableFuture 异步编程

### 1. 基本使用

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        // 异步任务
        return "Hello";
    })
    .thenApply(result -> result + " World")
    .thenAccept(result -> System.out.println(result));
```

### 2. 组合异步任务

```java
CompletableFuture<String> future1 = CompletableFuture
    .supplyAsync(() -> "Hello");
CompletableFuture<String> future2 = CompletableFuture
    .supplyAsync(() -> "World");

// 合并两个结果
CompletableFuture<String> combined = future1
    .thenCombine(future2, (r1, r2) -> r1 + " " + r2);

// 输出：Hello World
combined.join();
```

### 3. 异常处理

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        throw new RuntimeException("Error");
    })
    .exceptionally(ex -> {
        return "Default Value";
    })
    .whenComplete((result, ex) -> {
        if (ex != null) {
            System.out.println("Error: " + ex.getMessage());
        } else {
            System.out.println("Result: " + result);
        }
    });
```