# miniGenerationalGC

## What it simulates

This project simulates a generational virtual heap with memory units. The heap is divided into Eden, two Survivor spaces, and Old. Each object has an identifier, a name, a size, an age, a current space, and a set of references to other objects.

The project does not use real physical memory: capacities represent units in the simulation.

## Requirements and execution

Java 17 or later is required because the project uses records, switch expressions, and other modern language features.

From the project root, in PowerShell:

```powershell
$sources = Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName }
New-Item -ItemType Directory -Force build\classes | Out-Null
javac -d build\classes $sources
```

Then run a case by passing its name as an argument:

```powershell
java -cp build\classes Main isolated-root
```

Available cases:

```text
isolated-root
cycle-no-roots
consecutive-young-gcs
old-to-young-promotion
rejected-young-gc
eden-force-young-gc
exact-fit
invalid-name
full-gc-report
metrics-old-occupancy
metrics-empty-heap
metrics-all-objects-die
metrics-recommend-full-gc
metrics-no-recommend-full-gc
metrics-recommendation-before-first-young-gc
```

If no argument is provided, `isolated-root` is executed.

## Configuration

The default configuration is:

| Space | Capacity |
| --- | ---: |
| Eden | 10 |
| Each Survivor | 6 |
| Old | 20 |
| Promotion age | 2 |

`VirtualHeapConfig` allows different capacities and promotion ages to be defined. Capacities and promotion age must be greater than zero. Some cases use custom configurations to force promotions, capacity failures, or Young GCs triggered by allocation.

## Code structure

`VirtualHeap` is the public entry point and coordinates the simulation. The internal responsibilities are split into focused package-private classes:

| Start here | Responsibility |
| --- | --- |
| `heap/VirtualHeap.java` | Public API for allocation, roots, references, heap status, Young GC, Full GC, and the last report. |
| `heap/HeapStorage.java` | Owns Eden, both Survivor spaces, and Old; handles lookups, snapshots, object movement, and removal. |
| `heap/ReachabilityAnalyzer.java` | Calculates full-heap and young-generation reachability from roots and the remembered set. |
| `heap/YoungGcPlanner.java` | Builds the Young GC plan, checks capacity, and decides between Survivor placement and promotion. |
| `heap/RememberedSetManager.java` | Applies the Old-to-Young write barrier and rebuilds or updates the remembered set. |
| `heap/GcReportBuilder.java` | Builds GC reports and owns the collection counter, including promotion statistics and sequential report numbering. |
| `space/Space.java` | Models one bounded heap space and its stored objects. |
| `objects/SimObject.java` | Models an allocated object, including size, age, current space, and references. |
| `records/` | Immutable snapshots, operation plans, and GC report types returned by the simulation. |
| `Main.java` | Command-line scenarios and assertion-based checks for the implemented behaviors. |

For a top-down reading, start with `VirtualHeap`, then follow `collectYoung()` or `collectFull()` into the corresponding helper classes. Read `HeapStorage` alongside the collection flow to understand how simulated spaces change.

## How it works

### Roots and reachability

Roots are object identifiers that act as entry points. Reachability is calculated by traversing references from those roots. A reachable object survives a Full GC; an unreachable object is removed.

Reachability is transitive. Therefore, a chain such as `A -> B -> C` keeps all three objects alive when `A` is a root. A cycle without any root, such as `A -> B -> A`, does not keep its objects alive: being part of a cycle does not make an object reachable.

### Young GC

A Young GC processes Eden and the source Survivor space. Reachable young objects are planned for the destination Survivor and have their age increased. The destination Survivor must be empty before the operation starts.

When an object reaches the promotion age, it moves to Old. Early promotion can also occur: if a young object does not fit in the destination Survivor but does fit in Old, it is promoted even if it has not reached the configured age.

Unreachable young objects are removed. Objects already in Old are not moved by a Young GC.

When an allocation does not fit in the available Eden space, the allocation attempts a Young GC before inserting the new object.

### Full GC

A Full GC calculates global reachability from roots, removes unreachable objects from Eden, both Survivors, and Old, and rebuilds the remembered set. It does not promote or move objects between spaces.

An important consequence is the difference between the two collection types: a reference from an Old object can keep a Young object alive during a Young GC even when that Old object is no longer reachable from roots. The remembered set provides that entry point for young reachability. A Full GC, however, starts only from roots; if neither the Old object nor the Young object is reachable from roots, it can reclaim both.

## Remembered set and write barrier

The remembered set contains the identifiers of Old objects that reference Young objects. It is updated by the write barrier when `addReference` creates a reference from Old to Eden or a Survivor.

During a Young GC, objects promoted to Old by the current plan are also inspected, because their references may point to Young objects. This records the entries needed by the next Young GC.

After a Full GC, the remembered set is rebuilt by traversing references from surviving Old objects. Removed references and references that no longer point to Young objects are no longer included.

## Capacity failures

Before moving objects, a Young GC builds a plan and logically reserves the required space in Survivor and Old. If an object does not fit in any permitted destination, the collection is rejected with an exception.

The plan prevents partial changes: when capacity is insufficient, no object is moved, no age is incremented, and the heap remains unchanged. The `rejected-young-gc` case verifies this behavior.

## Reports and snapshots

`HeapStatusRecord` provides a snapshot of the heap:

- used and available size for every space;
- an image of each object, including id, name, size, age, space, and references;
- roots;
- remembered set;
- source and destination Survivor spaces.

`GCReportRecord` reports the collection type and number, object and unit counts before and after the collection, reclaimed objects and units, and promoted objects and units.

Snapshots are taken after operations so that tests can verify locations, ages, references, and the contents of every space.

## Simplifications

- Execution is sequential and has no concurrency or mutator threads.
- There is no physical memory, real address space, or byte allocation.
- Objects are moved between structures that represent spaces; there is no real physical evacuation.
- Full GC does not move objects: it only removes unreachable objects and rebuilds metadata.
- Pauses, execution times, and concurrent collection are not modeled.

## Available tests

Each `Main` case runs assertions through `check(...)` and prints `test passed` only after all assertions succeed.

- `isolated-root`: keeps `A` and `B` because `A` is a root and references `B`; removes isolated object `C`, and validates the Full GC report.
- `cycle-no-roots`: removes the `A -> B -> A` cycle when there is no root, and validates that both objects and their units are reclaimed.
- `consecutive-young-gcs`: verifies that `A` moves from Eden to Survivor at age 1 and then to Old at age 2; `B` is reclaimed during the first Young GC.
- `old-to-young-promotion`: verifies early promotion of `B` because the Survivor lacks capacity, that `A` remains Young, and that `B` enters the remembered set because it references `A`.
- `rejected-young-gc`: verifies that a capacity overflow throws an exception and leaves the heap without partial changes.
- `eden-force-young-gc`: verifies that an allocation that does not fit in Eden triggers a Young GC before the new object is inserted.
- `exact-fit`: verifies that an allocation that exactly fills Eden does not trigger an early collection.
- `invalid-name`: verifies that an empty name is rejected and that the heap remains unchanged.
- `full-gc-report`: verifies the counters for a Full GC that keeps the root object and reclaims the other object.
- `metrics-old-occupancy`: verifies that Old occupancy is 15% when 3 of 20 units are occupied and that 3 of 5 Young units are promoted, producing a 60% promotion percentage.
- `metrics-empty-heap`: verifies that an empty heap reports 0% Old occupancy and 0% promotion after a Young GC.
- `metrics-all-objects-die`: verifies that a Young GC with no reachable objects reports 0% Old occupancy and 0% promotion.
- `metrics-recommend-full-gc`: uses Eden 20, Survivor 6, Old 20, promotion age 1, and a rooted object of size 14 to verify 70% Old occupancy, a `true` Full GC recommendation, and no extra collection when the recommendation is queried.
- `metrics-no-recommend-full-gc`: repeats the integration check with an object of size 13, verifying 65% Old occupancy, a `false` recommendation, and no extra collection.
- `metrics-recommendation-before-first-young-gc`: verifies that a new heap has no metrics or Full GC recommendation yet, and that querying it does not create a report or change the heap snapshot.

The scenarios require no JUnit or external dependencies: they are verified by running `Main` with each argument.
