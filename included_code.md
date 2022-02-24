# What's in code?

- `App.kt` can be launched with `./gradlew run` that simulates very active program that has both short- and
long-living objects.
- `Heap.kt` shows how to make a heap dump programmatically on OpenJDK-based JVMs
- `Measurements.kt` shows how to measure memory- and cpu footprint of some small piece of code. It has a lot of failure modes and should not be used in any production context. It's more about comparing footprint of `List<Byte>` to `Array<Byte>`.
