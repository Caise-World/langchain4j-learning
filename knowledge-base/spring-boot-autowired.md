# Spring Boot 中 @Autowired 注解详解

## 一、三种注入方式

### 1. 构造器注入（Constructor Injection）

推荐方式，通过构造器注入依赖。

```java
@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**优点：**
- 依赖不可变（final）
- 不支持循环依赖检测
- 便于单元测试
- 确保依赖不为 null

**缺点：**
- 代码量稍多

### 2. Setter 注入（Setter Injection）

通过 setter 方法注入依赖。

```java
@Service
public class UserService {
    private UserRepository userRepository;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**优点：**
- 可选依赖注入
- 可在注入后修改

**缺点：**
- 依赖可变
- 可能产生 NullPointerException

### 3. 字段注入（Field Injection）

直接在字段上使用 @Autowired。

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}
```

**优点：**
- 代码最简洁

**缺点：**
- 依赖可变
- 无法注入后修改
- 难以单元测试（需要反射）
- 违背单一职责原则

## 二、@Autowired 的 required 属性

```java
@Autowired(required = false)
private UserRepository userRepository;
```

- `required = true`（默认）：找不到 bean 时抛出异常
- `required = false`：找不到 bean 时忽略

## 三、构造器注入的最佳实践

Spring 4.3+ 推荐使用构造器注入：

```java
@Service
public class OrderService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrderService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }
}
```

或使用 `@RequiredArgsConstructor`（Lombok）：

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
}
```

## 四、装配方式对比

| 特性 | 构造器注入 | Setter 注入 | 字段注入 |
|------|-----------|-------------|----------|
| 可变依赖 | ❌ | ✅ | ✅ |
| 不可变依赖 | ✅ | ❌ | ❌ |
| 循环依赖检测 | ✅ | ❌ | ❌ |
| 单元测试 | ✅ | ⚠️ | ❌ |
| 可选依赖 | ❌ | ✅ | ⚠️ |
| 代码简洁 | ⚠️ | ⚠️ | ✅ |