# memories
Links and description of tools together with a play project for exploring memory problems

Tools:
jmap
Android Profiler
Eclipse Memory Analyzer
Java Mission Control
GC logs
- Good settings for OpenJDK logs
-

## Vocabulary
- Working set: the amount of memory a program requires at some time interval. For example, handling HTTP rqeuest headers might have working set of a few kB but then as a response, server creates in-memory a big image of 5 MB before sending writing it to response. Here working set of one response is ~5 MB.
- Garbage collection: Automated process of finding objects that are no logner in use and freeing their memory.
- Allocation rate: How much memory we're using by the new objects we're creating within some timeframe. Usually measured as MB/s.
- JMX: Java Management Extensions is technology  

Two ways to look at memory: allocation rate & largest working set

## What is garbage collection?

- What are root sets, an image + explanation

## Basic error cases

### Running out of memory

In order to get the dreaded `java.lang.OutOfMemoryError`, our _working set_ 

#### Memory Leaks
- Leak Canary can help identifying leaks with Android

### Too much time spent in GC 

You can eventually get OOM with message "GC Overhead Limit Exceeded". On Android, you should get something similar (TODO message here).

#### How do we verify this?
- Check GC logs
  - How do we read these? TODO for a link to understanding JVM 9+ format 
  - How does ART 
- Check JMX GC indicators (less accurate)
- 

### Other causes
- Native memory on JVM
- ART probably has various resources


#### What are the solutions?
- Allocate less
  - Find the biggest allocators through profiling and reduce the amount they allocate
- Add more memory for less frequent GC

## Tools

### jmap

### Java Mission Control

[Java Mission Control](https://docs.oracle.com/en/java/java-components/jdk-mission-control/8/user-guide/) is an
open-source tool for both profiling and for observing JMX metrics in a JVM instance.

This can be used to do both ad-hoc profiling and exploring profiles that have been recorded earlier. Both CPU and memory
metrics can be recorded.

### Eclipse Memory Analyzer

When you're trying to understand in more detail what actually is in your heap or where you might be leaking memory,
[Eclipse Memory Analyzer](https://www.eclipse.org/mat/) is a handy tool.
It will open any heap dump < 64 GB, but it might need large amount of heap for its initial processing of the dump.
Processing creates indices on the disk that EMA can later-on use to load and analyze the dump faster.

https://plumbr.io/blog/memory-leaks/solving-outofmemoryerror-dump-is-not-a-waste

### JMH

[Java Microbenchmarking Harness](https://github.com/openjdk/jmh) is *the* way to do microbenchmarks. It allows you to
[accurately measure](https://shipilev.net/blog/2016/arrays-wisdom-ancients/#_not_an_allocation_pressure) how much the
piece of code you're benchmarking allocates. It's similar to what `Measurements.kt` in this repository does, but is
more heavy-weight and correct.

### JVM Object Layout

## References
Reference and ReferenceQueue

- [WeakReference](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ref/WeakReference.html) is a reference that does not prevent the referent from being collected.
- [SoftReference](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ref/SoftReference.html) is also a reference that does not prevent the referent from being collected. Difference to `WeakReference` is that GCs have some heuristics that make recently used or created and thus `SoftReference` can be used to implement in-memory caches that will be freed if there's need for more memory.   
- [PhantomReference](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ref/PhantomReference.html) is a reference that is only enqueued after the referred to object has been collected. It can be used to free other resources such a file descriptors or memory allocations after the object that has been using is gone.