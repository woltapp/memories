# memories
Links and descriptions of tools together with a play project for exploring JVM/ART memory problems

## Included code

Code in this repository is explained [here](included_code.md)

## Vocabulary
- Working set: the amount of memory a program requires during some operation. For example, handling HTTP request headers might have working set of a few kB but then as a response, server creates in-memory a big image of 5 MB before sending writing it to response. Here working set of one response is ~5 MB.
- Garbage collection: Automated process of finding objects that are no longer in use and freeing their memory. **GC** from now on.
- Allocation rate: How much memory we're using by the new objects we're creating within some timeframe. Usually measured as MB/s.
- Heap: The amount of memory available for application data. This is strictly smaller than memory size of the whole process.
- JMX: Java Management Extensions is technology for both observing and commanding JVM.
- Reachable: Object that is still in use in the program and won't be collected by GC. Either by design or by bug.

Two ways to look at memory: rate of allocating data & largest working set.

## What is garbage collection?

- Automated way to reclaim one resource: memory
  - We might still run out of other resources, say, sockets or file descriptors
- TODO What are root sets, an image + explanation

## Basic error cases
Problems with memory on garbage-collected runtimes such as JVM and ART can be split into two:
 * We don't have enough memory to do all the things we want
 * We're creating and abandoning a lot of things and GC has to do more work to clean after us.

### Running out of memory

In theory, our largest possible _working set_ should be a big as the amount of memory we've given to the runtime. If we
go past that, we'll get the dreaded `java.lang.OutOfMemoryError`. Due to various issues related to actual implementation
choices (fragmentation, generation sizes, humongous allocations), we'll always get this earlier.

When does one get OOM on ART?

#### What to do?
Identify potential culprits using [jmap](#jmap) and [Eclipse Memory Analyzer](#eclipse-memory-analyzer). If objects that
keep the memory are legitimate, then it's a case of too big working set for the given heap and choices are to increase
the heap size or slim the objects

The other case is memory leak. We might be, for example, keeping some map in a thread-local variable even though it was
supposed to be cleared after each request. 

TODO: What is big object, what is shallow and deep size, what is dominator object?

### Too much time spent in GC 

Many of the garbage collection algorithms have parts where they need to stop execution of the application altogether.
During this stop-the-world pause nothing happens, no new frames get created, no input gets read and no response from a
web server is written back. Occasional pauses of a few milliseconds are rarely noticeable, but if this happens more often,
application suffers.

#### Causes

We might have case of [running out of memory](#running-out-of-memory) where GC is constantly trying to free memory
without much success. This will most often be fatal eventually application runs out of memory. 

The Other case is that  the application is allocating a lot of short-living objects that do get collected but cause GC
to be run often. This is not fatal, but if application drops frames or a service takes long time to respond, it doesn't
provide quality experience to users.

#### How do we verify this?
- Check GC logs (see [Garbage collection logs](#garbage-collection-logs) )
- Check JMX GC indicators (less accurate)
- Profile allocations using Java Flight Recorder / Android Profiler to discover allocation rate.

### Other causes
#### JVM
- Running out of Metaspace
- "Direct memory" allocated through [ByteBuffer#allocateDirect](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/ByteBuffer.html#allocateDirect(int)), which is used by some libraries like NIO.
- Native memory on JVM
- ART probably has various resources


#### What are the solutions?
- Allocate less
  - Find the biggest allocators through profiling and reduce the amount they allocate
- Add more memory for less frequent GC

## Tools

### Garbage collection logs

On JVM, first thing to do is to enable GC logs. On JVM 9+, use `-Xlog:gc*:file=<GC-FILE-PATH>` and with earlier JVM
versions tihs  dependent on the implementation. On OpenJDK < Java 9 use the following switches:
```
-verbose:gc
-Xlog:gc:<GC-FILE-PATH>
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-XX:+PrintGCDateStamps
-XX:+PrintGCApplicationStoppedTime`
```

On Android, you can view the GC logs using [Logcat](https://developer.android.com/studio/debug/am-logcat#memory-logs).
GC runs are only recorded when GC is seen slow: the application is not running on the background and there's over 5 ms
pause or whole GC takes more than 100 ms. Thus, very frequent but short GC runs won't show up in the logs.

### jmap

For looking into memory usage of a JVM, we can use `jmap` . I can give both a coarse-grained look at the heap, showing
what classes are taking the most memory and a more fine-grained look by taking snapshots of the heap.

For coarse-grained look, run `jmap -histo <PID>`. This can give you insights like "we have a lot of strings", but won't
tell you why that is. Sometimes seeing the classes with most instances is enough to explain why we're using more memory
than expected.

To be able to figure out why better, it's better to take snapshot of the heap (usually called heap dumps).

To dump all objects that are still reachable, that is objects that GC would not free:
```
jmap -dump:live,format=b,file=heap_dump_$(date "+%Y%m%d_%H%M%S").bin <PID>
```
Change `live` to `all` to dump all objects. This is rarely needed, but is useful when trying to figure out issues
related to [`Reference`s](#references).

This heap dump can then be analyzed using [Eclipse Memory Analyzer](#eclipse-memory-analyzer) to find culprits for
potential memory leaks.

### Java Mission Control

[Java Mission Control](https://docs.oracle.com/en/java/java-components/jdk-mission-control/8/user-guide/) is an
open-source tool for both profiling and for observing JMX metrics in a JVM instance.

This can be used to do both ad-hoc profiling and exploring profiles that have been recorded earlier. Both CPU and memory
metrics can be recorded.

Profiling can and usually needs to be run without JMC GUI. This feature is called Java Flight Recorder (JFR), and it 
used to require commercial license until Java 11 or so. So make sure that using it is fine with the JDK you're using.

RedHat has a very [brief tutorial](https://developers.redhat.com/blog/2020/08/25/get-started-with-jdk-flight-recorder-in-openjdk-8u#demo__profiling_gc_allocation
)
and [official documentation](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/toc.htm) has the relevant
command line switches. 

It's one of the best ways to find out what parts of the application are allocating most and causing too much time being
spent in GC.

### Eclipse Memory Analyzer

When you're trying to understand in more detail what actually is in your heap or where you might be leaking memory,
[Eclipse Memory Analyzer](https://www.eclipse.org/mat/) (EMA) is a handy tool. 
It will open any heap dump < 64 GB, but it might need large amount of heap for its initial processing of the dump.
Processing creates indices on the disk that EMA can later-on use to load and analyze the dump faster.

Main functionalities in it are leak detection and analyzing how much specific objects or classes hold memory. This can
help us find that one `Map` that just happens to keep half of all objects alive. A good, although old introduction for
the tool can be read [here](http://memoryanalyzer.blogspot.com/2008/05/automated-heap-dump-analysis-finding.html)

### Android Profiler
Android Profiler allows you to [capture and analyze heap dump](https://developer.android.com/studio/profile/memory-profiler#capture-heap-dump).
It has seemingly same functionality as Eclipse Memory Analyzer and Java Mission Control with a more modern UI.

In case _EMA_ is needed, Android Profiler can export heap dump and the dump [can be converted](https://developer.android.com/studio/profile/memory-profiler#save-hprof)
to a form that _EMA_ understands.

### JMH

[Java Microbenchmarking Harness](https://github.com/openjdk/jmh) is *the* way to do microbenchmarks. It allows you to
[accurately measure](https://shipilev.net/blog/2016/arrays-wisdom-ancients/#_not_an_allocation_pressure) how much the
piece of code you're benchmarking allocates. It's similar to what `Measurements.kt` in this repository does, but is
more heavy-weight and correct.

### JVM Object Layout
Java Object Layout can be used to figure out how much memory a specific object actually uses. Aleksey Shipilëv, who's
a great authority on JVM internals has a good blog post on [using it](https://shipilev.net/jvm/objects-inside-out/).

## References
JDK `Reference`-class' subclasses together with [ReferenceQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ref/ReferenceQueue.html) provide a way to implement caches and freeing of non-memory resources.

- [WeakReference](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ref/WeakReference.html) is a reference that does not prevent the referent from being collected.
- [SoftReference](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ref/SoftReference.html) is also a reference that does not prevent the referent from being collected. Difference to `WeakReference` is that GCs have some heuristics that make recently used or created and thus `SoftReference` can be used to implement in-memory caches that will be freed if there's need for more memory.   
- [PhantomReference](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ref/PhantomReference.html) is a reference that is only enqueued after the referred to object has been collected. It can be used to free other resources such a file descriptors or memory allocations after the object that has been using is gone.
- [ReferenceQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ref/ReferenceQueue.html) is a queue where the references will end up after the thing they refer to has been freed. This means that when `ReferenceQueue#poll` returns a reference, the object it refers to has been freed, and we're free to, say, close the file descriptor the object used.