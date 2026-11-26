---
description: 'Guidelines for building Java base applications'
applyTo: '**/*.java'
---

# Java Development

## General Instructions

- First, prompt the user if they want to integrate static analysis tools (SonarQube, PMD, Checkstyle)
  into their project setup. If yes, provide guidance on tool selection and configuration.
- If the user declines static analysis tools or wants to proceed without them, continue with implementing the Best practices, bug patterns and code smell prevention guidelines outlined below.
- Address code smells proactively during development rather than accumulating technical debt.
- Focus on readability, maintainability, and performance when refactoring identified issues.
- Use IDE / Code editor reported warnings and suggestions to catch common patterns early in development.

## Best practices

- **DTOs**: For classes primarily intended to store data (e.g., DTOs, immutable data structures), **use traditional classes instead of records**.
- **Pattern Matching**: Utilize pattern matching for `instanceof` and `switch` expression to simplify conditional logic and type casting.
- **Type Inference**: Use `var` for local variable declarations to improve readability, but only when the type is explicitly clear from the right-hand side of the expression.
- **Immutability**: Favor immutable objects. Make classes and fields `final` where possible. Use collections from `List.of()`/`Map.of()` for fixed data. Use `Stream.toList()` to create immutable lists.
- **Streams and Lambdas**: Use the Streams API and lambda expressions for collection processing. Employ method references (e.g., `stream.map(Foo::toBar)`).
- **Null Handling**: Avoid returning or accepting `null`. Use `Optional<T>` for possibly-absent values and `Objects` utility methods like `equals()` and `requireNonNull()`.
- **Tests**: Use JUnit `assertTrue()`, `assertEquals()` instead of Java `assert`.

### Naming Conventions

- Follow Google's Java style guide:
    - `UpperCamelCase` for class and interface names.
    - `lowerCamelCase` for method and variable names.
    - `UPPER_SNAKE_CASE` for constants.
    - `lowercase` for package names.
    - `methodTested_scenario_expectedResult` for test names
- Use nouns for classes (`UserService`) and verbs for methods (`getUserById`).
- Avoid abbreviations and Hungarian notation.

### Bug Patterns

| Rule ID | Description                                                 | Example / Notes                                                                                  |
| ------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `S2095` | Resources should be closed                                  | Use try-with-resources when working with streams, files, sockets, etc.                           |
| `S1698` | Objects should be compared with `.equals()` instead of `==` | Especially important for Strings and boxed primitives.                                           |
| `S1905` | Redundant casts should be removed                           | Clean up unnecessary or unsafe casts.                                                            |
| `S3518` | Conditions should not always evaluate to true or false      | Watch for infinite loops or if-conditions that never change.                                     |
| `S108`  | Unreachable code should be removed                          | Code after `return`, `throw`, etc., must be cleaned up.                                          |

## Code Smells

| Rule ID | Description                                            | Example / Notes                                                               |
| ------- | ------------------------------------------------------ | ----------------------------------------------------------------------------- |
| `S107`  | Methods should not have too many parameters            | Refactor into helper classes or use builder pattern.                          |
| `S121`  | Duplicated blocks of code should be removed            | Consolidate logic into shared methods.                                        |
| `S138`  | Methods should not be too long                         | Break complex logic into smaller, testable units.                             |
| `S3776` | Cognitive complexity should be reduced                 | Simplify nested logic, extract methods, avoid deep `if` trees.                |
| `S1192` | String literals should not be duplicated               | Replace with constants or enums.                                              |
| `S1854` | Unused assignments should be removed                   | Avoid dead variables—remove or refactor.                                      |
| `S109`  | Magic numbers should be replaced with constants        | Improves readability and maintainability.                                     |
| `S1188` | Catch blocks should not be empty                       | Always log or handle exceptions meaningfully.                                 |

## Clean Code

### CC1. Methods do one thing, at one level of abstraction

- A method should do one thing, and everything inside it should sit at the same level of abstraction. High-level steps read like a summary; details live in well-named private methods below it (top-down "newspaper" order).
- Prefer guard clauses and early returns/throws over nested `if` blocks.
- Aim for methods short enough to read without scrolling (roughly 5–20 lines).

**Avoid** — vague name, flag argument, nested conditions, magic numbers, mixed abstraction levels, `null` return:

```java
public Invoice process(Order o, boolean applyDiscount) {
    if (o != null) {
        if (!o.getItems().isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            List<LineItem> items = new ArrayList<>();
            for (Item i : o.getItems()) {
                items.add(new LineItem(i.getSku(), i.getQty(), i.getPrice()));
                total = total.add(i.getPrice().multiply(BigDecimal.valueOf(i.getQty())));
            }
            if (applyDiscount && o.getCustomer().getType() == 2) {
                total = total.multiply(new BigDecimal("0.9"));
            }
            return new Invoice(o.getId(), items, total);
        }
    }
    return null;
}
```

**Prefer** — the public method reads as a list of steps; each helper does one thing:

```java
public Invoice createInvoice(Order order) {
    requireBillable(order);

    List<LineItem> lineItems = toLineItems(order.getItems());
    BigDecimal subtotal = sumOf(lineItems);
    BigDecimal discount = discountPolicy.discountFor(order.getCustomer(), subtotal);

    return new Invoice(order.getId(), lineItems, subtotal.subtract(discount));
}

private void requireBillable(Order order) {
    Objects.requireNonNull(order, "order must not be null");
    if (order.getItems().isEmpty()) {
        throw new EmptyOrderException(order.getId());
    }
}

private List<LineItem> toLineItems(List<Item> items) {
    return items.stream()
            .map(LineItem::from)
            .toList();
}

private BigDecimal sumOf(List<LineItem> lineItems) {
    return lineItems.stream()
            .map(LineItem::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

### CC2. Names reveal intent

- A name should tell why something exists and how it is used, without needing a comment. If a name needs a comment, rename it.
- Avoid noise words that add no meaning: `Data`, `Info`, `Manager`, `Processor`, `Helper`, `Util`.
- Booleans read as predicates: `isActive`, `hasPermission`, `canRetry`.
- Use one word per concept across the codebase (don't mix `get`, `fetch` and `retrieve` for the same kind of operation).
- Name length should grow with scope: `i` is fine in a 3-line lambda, not in a field.

### CC3. Keep method arguments few and honest

- Aim for 0–2 parameters; 3 is the practical maximum (see `S107`). Group related parameters into a parameter object.
- No boolean flag arguments — a flag means the method does two things. Split it: `render(true)` → `renderForPrint()` / `renderForScreen()`.
- No output arguments (mutating a parameter to return a result). Return a value instead.

### CC4. Command-Query Separation and no hidden side effects

- A method either changes state (command, returns `void`) or answers a question (query, returns a value) — not both.
- Getters and query methods must never modify state.
- If a method has a side effect, its name must say so. A method called `checkPassword` must not also start a session.

### CC5. Handle errors deliberately

- Use unchecked, domain-specific exceptions (`EmptyOrderException`, `PaymentDeclinedException`) instead of error codes or generic `RuntimeException`.
- Include context in the message (IDs, values) and always preserve the cause when wrapping: `throw new ReportGenerationException(reportId, e);`.
- Don't catch `Exception` or `Throwable` broadly; catch the most specific type you can actually handle.
- Don't use exceptions for normal control flow.
- Error handling is "one thing": extract the body of a non-trivial `try` block into its own method.

### CC6. Comments explain *why*, not *what*

- Make the code self-explanatory first (extract a method or variable with a good name) before reaching for a comment.
- Good comments: intent, non-obvious business rules, warnings of consequences, and Javadoc on public APIs.
- Remove redundant comments that restate the code, journal/changelog comments, and commented-out code (version control keeps history).

## SOLID and Design

### CC7. Apply the SOLID principles

- **Single Responsibility (SRP)**: a class has one reason to change. A `UserService` that validates input, saves to the database and sends emails should be split. Warning signs: names containing `And`/`Manager`, many unrelated dependencies, or many private helpers that use disjoint fields.
- **Open/Closed (OCP)**: add new behavior by adding new classes (a new `Strategy` implementation, a new permitted subtype of a sealed interface) rather than editing a growing `switch`/`if-else` chain.
- **Liskov Substitution (LSP)**: a subtype must be usable wherever its supertype is expected. Overrides must not throw `UnsupportedOperationException`, demand stricter inputs, or weaken guarantees of the parent contract.
- **Interface Segregation (ISP)**: prefer small, focused interfaces (`InvoiceReader`, `InvoiceWriter`) over one large one. Clients should not depend on methods they don't use.
- **Dependency Inversion (DIP)**: business logic depends on interfaces, not concrete infrastructure. Inject collaborators through the constructor (prefer constructor injection over field injection) and don't `new` up services inside business logic.

### CC8. Prefer composition over inheritance

- Use inheritance only for a true *is-a* relationship where the subtype honors the full parent contract (LSP).
- To reuse behavior, delegate to a collaborator instead of extending a class.
- Make classes `final` unless they are explicitly designed and documented for extension.
- Use `sealed` interfaces/classes when the set of subtypes is closed and known, and combine them with pattern-matching `switch`.

### CC9. Follow the Law of Demeter ("Tell, don't ask")

- A method should talk only to its own fields, its parameters, objects it creates, and their direct methods.
- Avoid chains that reach through other objects: `order.getCustomer().getAddress().getCity()`. Add a meaningful method instead, e.g. `order.shippingCity()`, or move the behavior to the object that owns the data.
- Fluent APIs (Streams, builders, `Optional`) are not violations — they return the same kind of object, not another object's internals.

### CC10. Use design patterns to remove a concrete smell, not speculatively

Introduce a pattern only when it solves a problem that exists in the code now. Don't add layers of abstraction "just in case".

| Smell / Situation                                                    | Pattern                                    | Notes                                                                                   |
| -------------------------------------------------------------------- | ------------------------------------------ | --------------------------------------------------------------------------------------- |
| `switch`/`if-else` on a type or enum to choose behavior              | **Strategy**                               | Inject a `Map<Type, Strategy>` or let the enum/sealed type carry the behavior.          |
| Constructor with many or optional parameters                         | **Builder**                                | Validate required fields in `build()`; keep the built object immutable.                 |
| Complex creation logic or choosing an implementation at runtime      | **Factory method / static factory**        | Name factories by intent: `Money.of(...)`, `Report.fromCsv(...)`.                       |
| Cross-cutting behavior (caching, retry, logging, metrics)            | **Decorator**                              | Wrap the interface; keep the core implementation free of these concerns.                |
| Third-party or legacy API with an awkward interface                  | **Adapter**                                | Keep external types at the boundary; the domain depends on your own interface.          |
| Same algorithm skeleton with varying steps                           | **Template Method** (or Strategy)          | Prefer Strategy when composition fits — it avoids inheritance.                          |
| Several components must react to an event without being coupled      | **Observer / domain events**               | Publishers don't know their subscribers.                                                |
| Need for a single shared instance                                    | **Dependency injection scope**, not a hand-written Singleton | Hand-written singletons hide dependencies and make testing hard.  |

## Build and Verification

- After adding or modifying code, verify the project continues to build successfully.
- If the project uses Maven, run `mvn clean install`.
- Ensure all tests pass as part of the build.