import config.VirtualHeapConfig;
import enums.GCTypeEnum;
import enums.SpaceEnum;
import exceptions.ObjectCreationException;
import exceptions.VirtualHeapException;
import heap.VirtualHeap;
import records.GCReportRecord;
import records.HeapStatusRecord;
import records.SimObjectRecord;

public class Main {
    public static void main(String[] args) {
        String caseName = (args.length > 0) ? args[0].toLowerCase() : "isolated-root";

        switch (caseName) {
            case "isolated-root" -> runIsolatedRootCase();
            case "cycle-no-roots" -> runCycleNoRootsCase();
            case "consecutive-young-gcs" -> runConsecutiveYoungGcsCase();
            case "old-to-young-promotion" -> runOldToYoungPromotionCase();
            case "rejected-young-gc" -> runRejectedYoungGcCase();
            case "eden-force-young-gc" -> runEdenForceYoungGC();
            case "exact-fit" -> runExactFitCase();
            case "invalid-name" -> runInvalidNameCase();
            case "full-gc-report" -> runFullGcReportCase();
            case "metrics-old-occupancy" -> runMetricsOldOccupancyCase();
            case "metrics-empty-heap" -> runMetricsEmptyHeapCase();
            case "metrics-all-objects-die" -> runMetricsAllObjectsDieCase();
            case "metrics-recommend-full-gc" -> runMetricsRecommendFullGcCase();
            case "metrics-no-recommend-full-gc" -> runMetricsNoRecommendFullGcCase();
            case "metrics-recommendation-before-first-young-gc" -> runMetricsRecommendationBeforeFirstYoungGcCase();
            default -> throw new IllegalArgumentException("Unknown case: " + caseName
                    + ". Valid cases: isolated-root, cycle-no-roots, consecutive-young-gcs, old-to-young-promotion, rejected-young-gc, eden-force-young-gc, exact-fit, invalid-name, full-gc-report, metrics-old-occupancy, metrics-empty-heap, metrics-all-objects-die, metrics-recommend-full-gc, metrics-no-recommend-full-gc, metrics-recommendation-before-first-young-gc");
        }
    }

    private static void runIsolatedRootCase() {
        VirtualHeapConfig config = new VirtualHeapConfig();
        VirtualHeap heap = new VirtualHeap(config);

        System.out.println("Case: Root A, A -> B, C isolated");

        long idA = heap.allocate("A", 2);
        long idB = heap.allocate("B", 3);
        long idC = heap.allocate("C", 1);

        heap.addRoot(idA);
        heap.addReference(idA, idB);

        System.out.println("Before collection:");
        printHeapStatus(heap.getHeapStatus());

        GCReportRecord report = heap.collectFull();

        HeapStatusRecord statusAfter = heap.getHeapStatus();
        System.out.println("\nAfter collection:");
        printHeapStatus(statusAfter);

        check(report.objectsBefore() == 3 && report.objectsAfter() == 2,
                "La Full GC debe conservar 2 de los 3 objetos");
        check(report.unitsBefore() == 6 && report.unitsAfter() == 5,
                "La Full GC debe conservar 5 unidades de las 6 iniciales");
        check(report.reclaimedObjects() == 1 && report.reclaimedUnits() == 1,
                "La Full GC debe recuperar C y sus 1 unidad");
        check(report.type() == GCTypeEnum.FULL && report.numCollection() == 1,
                "La Full GC debe ser el reporte número 1");
        check(heap.getLastGcReport().orElseThrow().equals(report),
                "El último reporte debe coincidir con el reporte de la Full GC");
        check(findObject(statusAfter, idA) != null, "A debe sobrevivir a la Full GC");
        check(findObject(statusAfter, idB) != null, "B debe sobrevivir a la Full GC");
        check(findObject(statusAfter, idC) == null, "C debe desaparecer en la Full GC");
        System.out.println("Isolated root test passed");
    }

    private static void runCycleNoRootsCase() {
        VirtualHeapConfig config = new VirtualHeapConfig();
        VirtualHeap heap = new VirtualHeap(config);

        System.out.println("Case: A -> B, B -> A, roots empty");

        long idA = heap.allocate("A", 2);
        long idB = heap.allocate("B", 3);

        heap.addReference(idA, idB);
        heap.addReference(idB, idA);

        System.out.println("Before collection:");
        printHeapStatus(heap.getHeapStatus());

        GCReportRecord report = heap.collectFull();

        HeapStatusRecord statusAfter = heap.getHeapStatus();
        System.out.println("\nAfter collection:");
        printHeapStatus(statusAfter);

        check(report.objectsBefore() == 2 && report.objectsAfter() == 0,
                "La Full GC debe recoger los dos objetos del ciclo sin raíces");
        check(report.unitsBefore() == 5 && report.unitsAfter() == 0,
                "La Full GC debe recuperar las 5 unidades del ciclo");
        check(report.reclaimedObjects() == 2 && report.reclaimedUnits() == 5,
                "La Full GC debe reportar los dos objetos y 5 unidades recuperadas");
        check(report.type() == GCTypeEnum.FULL && report.numCollection() == 1,
                "La Full GC debe ser el reporte número 1");
        check(heap.getLastGcReport().orElseThrow().equals(report),
                "El último reporte debe coincidir con el reporte de la Full GC");
        check(findObject(statusAfter, idA) == null, "A debe desaparecer al no existir raíces");
        check(findObject(statusAfter, idB) == null, "B debe desaparecer al no existir raíces");
        System.out.println("Cycle without roots test passed");
    }

    private static void runConsecutiveYoungGcsCase() {
        VirtualHeap heap = new VirtualHeap(new VirtualHeapConfig());

        System.out.println("Case: consecutive young GCs with rooted A and unreachable B");

        long idA = heap.allocate("A", 2);
        long idB = heap.allocate("B", 3);
        heap.addRoot(idA);

        printHeapStatus(heap.getHeapStatus());

        GCReportRecord firstReport = heap.collectYoung();
        HeapStatusRecord firstStatus = heap.getHeapStatus();
        printHeapStatus(firstStatus);

        check(findObject(firstStatus, idA) != null, "A debe sobrevivir a la primera Young GC");
        check(findObject(firstStatus, idA).space() != SpaceEnum.OLD, "A no debe estar en Old tras la primera Young GC");
        check(findObject(firstStatus, idA).age() == 1, "A debe tener edad 1 tras la primera Young GC");
        check(findObject(firstStatus, idB) == null, "B debe desaparecer tras la primera Young GC");
        check(firstReport.reclaimedUnits() == 3, "La primera Young GC debe recuperar 3 unidades");
        check(firstReport.type() == GCTypeEnum.YOUNG && firstReport.numCollection() == 1,
                "La primera Young GC debe ser el reporte número 1");
        check(heap.getLastGcReport().orElseThrow().equals(firstReport),
                "El último reporte debe ser la primera Young GC");
        check(firstReport.promotedObjects() == 0, "La primera Young GC no debe promocionar objetos");
        check(firstReport.promotedUnits() == 0, "La primera Young GC no debe promocionar unidades");

        GCReportRecord secondReport = heap.collectYoung();
        HeapStatusRecord secondStatus = heap.getHeapStatus();
        printHeapStatus(secondStatus);

        check(findObject(secondStatus, idA) != null, "A debe sobrevivir a la segunda Young GC");
        check(findObject(secondStatus, idA).space() == SpaceEnum.OLD, "A debe estar en Old tras la segunda Young GC");
        check(findObject(secondStatus, idA).age() == 2, "A debe tener edad 2 tras la segunda Young GC");
        check(findObject(secondStatus, idB) == null, "B debe seguir sin existir tras la segunda Young GC");
        check(secondReport.reclaimedUnits() == 0, "La segunda Young GC no debe recuperar unidades");
        check(secondReport.type() == GCTypeEnum.YOUNG && secondReport.numCollection() == 2,
                "La segunda Young GC debe ser el reporte número 2");
        check(heap.getLastGcReport().orElseThrow().equals(secondReport),
                "El último reporte debe ser la segunda Young GC");
        check(secondReport.promotedObjects() == 1, "La segunda Young GC debe promocionar 1 objeto");
        check(secondReport.promotedUnits() == 2, "La segunda Young GC debe promocionar 2 unidades");

        System.out.println("Consecutive young GC test passed");
    }

    private static SimObjectRecord findObject(HeapStatusRecord status, long id) {
        return java.util.stream.Stream.of(
                status.eden().storageImage(),
                status.survivor0().storageImage(),
                status.survivor1().storageImage(),
                status.old().storageImage())
                .flatMap(java.util.Collection::stream)
                .filter(object -> object.id() == id)
                .findFirst()
                .orElse(null);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void runOldToYoungPromotionCase() {
        VirtualHeapConfig config = new VirtualHeapConfig(10, 4, 20, 5);
        VirtualHeap heap = new VirtualHeap(config);

        System.out.println("Case: Old -> Young reference created by promotion");

        long idA = heap.allocate("A", 2);
        long idB = heap.allocate("B", 3);
        heap.addRoot(idB);
        heap.addReference(idB, idA);

        printHeapStatus(heap.getHeapStatus());

        GCReportRecord firstReport = heap.collectYoung();
        HeapStatusRecord firstStatus = heap.getHeapStatus();
        printHeapStatus(firstStatus);

        check(findObject(firstStatus, idA).space() == SpaceEnum.SURVIVOR_1,
                "A debe quedar en el survivor de destino");
        check(findObject(firstStatus, idA).age() == 1, "A debe tener edad 1");
        check(findObject(firstStatus, idB).space() == SpaceEnum.OLD,
                "B debe promocionarse a Old por falta de espacio en survivor");
        check(firstReport.promotedObjects() == 1 && firstReport.promotedUnits() == 3,
                "La primera Young GC debe promocionar B y sus 3 unidades");
        check(firstReport.type() == GCTypeEnum.YOUNG && firstReport.numCollection() == 1,
                "La primera Young GC debe ser el reporte número 1");
        check(heap.getLastGcReport().orElseThrow().equals(firstReport),
                "El último reporte debe ser la primera Young GC");
        check(firstStatus.rememberedSet().contains(idB),
                "B debe estar en el remembered set por apuntar a A");

        GCReportRecord secondReport = heap.collectYoung();
        HeapStatusRecord secondStatus = heap.getHeapStatus();
        printHeapStatus(secondStatus);

        check(findObject(secondStatus, idA).space() == SpaceEnum.SURVIVOR_0,
                "A debe cambiar al otro survivor");
        check(findObject(secondStatus, idA).age() == 2, "A debe tener edad 2");
        check(findObject(secondStatus, idB).space() == SpaceEnum.OLD,
                "B debe permanecer en Old");
        check(findObject(secondStatus, idB).references().contains(idA),
                "B debe conservar la referencia a A");
        check(secondReport.promotedObjects() == 0 && secondReport.promotedUnits() == 0,
                "La segunda Young GC no debe promocionar objetos");
        check(secondReport.type() == GCTypeEnum.YOUNG && secondReport.numCollection() == 2,
                "La segunda Young GC debe ser el reporte número 2");
        check(heap.getLastGcReport().orElseThrow().equals(secondReport),
                "El último reporte debe ser la segunda Young GC");
        check(secondStatus.rememberedSet().contains(idB),
                "B debe seguir en el remembered set");
        System.out.println("Old-to-young promotion test passed");
    }

    private static void runRejectedYoungGcCase() {
        VirtualHeapConfig config = new VirtualHeapConfig(10, 4, 3, 1);
        VirtualHeap heap = new VirtualHeap(config);

        System.out.println("Case: rejected young GC leaves the heap unchanged");

        long idA = heap.allocate("A", 2);
        long idB = heap.allocate("B", 2);
        heap.addRoot(idA);
        heap.addRoot(idB);

        System.out.println("Before rejected Young GC:");
        printHeapStatus(heap.getHeapStatus());

        HeapStatusRecord before = heap.getHeapStatus();
        boolean rejected = false;
        try {
            heap.collectYoung();
        } catch (VirtualHeapException exception) {
            rejected = true;
            System.out.println("Young GC rejected: " + exception.getMessage());
        }

        HeapStatusRecord after = heap.getHeapStatus();
        System.out.println("After rejected Young GC:");
        printHeapStatus(after);

        check(rejected, "La Young GC debe rechazarse por overflow de Old");
        check(heap.getLastGcReport().isEmpty(),
                "Una Young GC rechazada no debe crear un reporte");
        check(after.eden().actualSize() == before.eden().actualSize()
                && after.old().actualSize() == before.old().actualSize(),
                "El heap debe conservar sus tamaños tras rechazar la Young GC");
        check(findObject(after, idA) != null && findObject(after, idB) != null,
                "A y B deben permanecer en el heap tras el rechazo");
        check(findObject(after, idA).age() == 0 && findObject(after, idB).age() == 0,
                "A y B no deben cambiar de edad tras el rechazo");
        System.out.println("Rejected young GC test passed");
    }

    private static void runEdenForceYoungGC() {
        VirtualHeapConfig config = new VirtualHeapConfig();
        VirtualHeap heap = new VirtualHeap(config);

        System.out.println("Case: eden space forces a young GC");

        long idA = heap.allocate("A", 6);
        heap.addRoot(idA);
        System.out.println("Before adding B and forcing young GC:");
        printHeapStatus(heap.getHeapStatus());
        long idB = heap.allocate("B", 5);
        heap.addRoot(idB);

        System.out.println("After forcing young GC:");
        HeapStatusRecord statusAfter = heap.getHeapStatus();
        printHeapStatus(statusAfter);

        check(findObject(statusAfter, idA).space() == SpaceEnum.SURVIVOR_1,
                "A debe moverse al survivor al forzar la Young GC");
        check(findObject(statusAfter, idA).age() == 1, "A debe tener edad 1");
        check(findObject(statusAfter, idB).space() == SpaceEnum.EDEN,
                "B debe quedar en Eden después de ser asignado");
        check(statusAfter.eden().actualSize() == 5 && statusAfter.survivor1().actualSize() == 6,
                "Eden y survivor deben reflejar la asignación y la Young GC");
        check(heap.getLastGcReport().orElseThrow().type() == GCTypeEnum.YOUNG
                && heap.getLastGcReport().orElseThrow().numCollection() == 1,
                "La Young GC provocada por la asignación debe ser el reporte número 1");
        System.out.println("Eden force young GC test passed");
    }

    private static void runExactFitCase() {
        VirtualHeap heap = new VirtualHeap(new VirtualHeapConfig());

        System.out.println("Case: allocation fits exactly in the remaining Eden space");

        long idA = heap.allocate("A", 6);
        heap.addRoot(idA);
        System.out.println("After allocating A (6):");
        printHeapStatus(heap.getHeapStatus());

        long idB = heap.allocate("B", 4);
        heap.addRoot(idB);
        HeapStatusRecord statusAfter = heap.getHeapStatus();
        System.out.println("After allocating B (4):");
        printHeapStatus(statusAfter);

        check(statusAfter.eden().actualSize() == 10 && statusAfter.eden().availableSize() == 0,
                "Eden debe quedar exactamente lleno");
        check(findObject(statusAfter, idA).space() == SpaceEnum.EDEN
                && findObject(statusAfter, idB).space() == SpaceEnum.EDEN,
                "A y B deben permanecer en Eden en el ajuste exacto");
        check(findObject(statusAfter, idA).age() == 0 && findObject(statusAfter, idB).age() == 0,
                "El ajuste exacto no debe ejecutar una Young GC");
        System.out.println("Exact fit test passed");
    }

    private static void runInvalidNameCase() {
        VirtualHeap heap = new VirtualHeap(new VirtualHeapConfig());

        System.out.println("Case: invalid object name is rejected without collection");

        long idA = heap.allocate("A", 8);
        heap.addRoot(idA);
        System.out.println("Before invalid allocation:");
        printHeapStatus(heap.getHeapStatus());

        HeapStatusRecord before = heap.getHeapStatus();
        boolean rejected = false;
        try {
            heap.allocate("", 3);
        } catch (ObjectCreationException exception) {
            rejected = true;
            System.out.println("Allocation rejected: " + exception.getMessage());
        }

        HeapStatusRecord after = heap.getHeapStatus();
        System.out.println("After invalid allocation:");
        printHeapStatus(after);

        check(rejected, "El nombre vacío debe rechazar la asignación");
        check(after.eden().actualSize() == before.eden().actualSize()
                && after.eden().storageImage().size() == before.eden().storageImage().size(),
                "Una asignación inválida no debe modificar Eden");
        check(findObject(after, idA) != null && findObject(after, idA).age() == 0,
                "A debe permanecer intacto tras la asignación inválida");
        check(heap.getLastGcReport().isEmpty(),
                "Una asignación inválida no debe crear un reporte de GC");
        System.out.println("Invalid name test passed");
    }

    private static void runFullGcReportCase() {
        VirtualHeap heap = new VirtualHeap(new VirtualHeapConfig());

        System.out.println("Case: one Full GC report");

        long idA = heap.allocate("A", 2);
        heap.allocate("B", 3);
        heap.addRoot(idA);

        GCReportRecord report = heap.collectFull();
        printGCReport(report);

        HeapStatusRecord statusAfter = heap.getHeapStatus();
        check(report.objectsBefore() == 2 && report.objectsAfter() == 1,
                "La Full GC debe conservar únicamente el objeto raíz");
        check(report.unitsBefore() == 5 && report.unitsAfter() == 2,
                "La Full GC debe conservar 2 unidades");
        check(report.reclaimedObjects() == 1 && report.reclaimedUnits() == 3,
                "La Full GC debe recuperar B y sus 3 unidades");
        check(report.type() == GCTypeEnum.FULL && report.numCollection() == 1,
                "La Full GC debe ser el reporte número 1");
        check(heap.getLastGcReport().orElseThrow().equals(report),
                "El último reporte debe coincidir con el reporte de la Full GC");
        check(report.promotedObjects() == 0 && report.promotedUnits() == 0,
                "La Full GC no debe promocionar objetos");
        check(findObject(statusAfter, idA) != null && findObject(statusAfter, idA).space() == SpaceEnum.EDEN,
                "A debe permanecer en Eden tras la Full GC");
        System.out.println("Full GC report test passed");
    }

    private static void runMetricsOldOccupancyCase() {
        VirtualHeap heap = new VirtualHeap(new VirtualHeapConfig(10, 2, 20, 2));
        long[] ids = new long[5];
        for (int index = 0; index < ids.length; index++) {
            ids[index] = heap.allocate("Object-" + index, 1);
            heap.addRoot(ids[index]);
        }

        heap.collectYoung();

        var metrics = heap.getHeapMetrics().orElseThrow();
        check(metrics.oldOccupancyPercentage() == 15.0,
                "Old debe estar ocupado al 15 por ciento");
        check(metrics.promotionPercentage() == 60.0,
                "Debe promocionarse el 60 por ciento de las unidades Young");
        check(metrics.completedYoungGC() == 1,
                "Debe haberse completado una Young GC");
        System.out.println("Metrics Old occupancy test passed");
    }

    private static void runMetricsEmptyHeapCase() {
        VirtualHeap heap = new VirtualHeap(new VirtualHeapConfig());

        heap.collectYoung();

        var metrics = heap.getHeapMetrics().orElseThrow();
        check(metrics.oldOccupancyPercentage() == 0.0,
                "Un heap vacío debe tener Old ocupado al 0 por ciento");
        check(metrics.promotionPercentage() == 0.0,
                "Un heap vacío no debe promocionar unidades");
        check(metrics.completedYoungGC() == 1,
                "Debe contabilizarse la Young GC del heap vacío");
        System.out.println("Metrics empty heap test passed");
    }

    private static void runMetricsAllObjectsDieCase() {
        VirtualHeap heap = new VirtualHeap(new VirtualHeapConfig());
        heap.allocate("Unreachable-A", 2);
        heap.allocate("Unreachable-B", 3);

        heap.collectYoung();

        var metrics = heap.getHeapMetrics().orElseThrow();
        check(metrics.oldOccupancyPercentage() == 0.0,
                "Old debe estar vacío cuando mueren todos los objetos");
        check(metrics.promotionPercentage() == 0.0,
                "No debe haber promoción cuando mueren todos los objetos");
        check(metrics.completedYoungGC() == 1,
                "Debe contabilizarse la Young GC");
        System.out.println("Metrics all objects die test passed");
    }

    private static void runMetricsRecommendFullGcCase() {
        VirtualHeap heap = new VirtualHeap(new VirtualHeapConfig(20, 6, 20, 1));
        long objectId = heap.allocate("Old candidate", 14);
        heap.addRoot(objectId);

        GCReportRecord report = heap.collectYoung();
        var metrics = heap.getHeapMetrics().orElseThrow();
        int collectionsBeforeRecommendation = report.numCollection();
        boolean recommendation = heap.getLastFullGcRecommendation().orElseThrow();

        check(metrics.oldOccupancyPercentage() == 70.0,
                "Old debe estar ocupado al 70 por ciento");
        check(recommendation,
                "El 70 por ciento debe recomendar una Full GC");
        check(heap.getLastGcReport().orElseThrow().numCollection() == collectionsBeforeRecommendation,
                "Consultar la recomendación no debe incrementar el contador");
        System.out.println("Full GC recommendation for 70%: " + recommendation);
        System.out.println("Metrics Full GC recommendation test passed");
    }

    private static void runMetricsNoRecommendFullGcCase() {
        VirtualHeap heap = new VirtualHeap(new VirtualHeapConfig(20, 6, 20, 1));
        long objectId = heap.allocate("Old candidate", 13);
        heap.addRoot(objectId);

        GCReportRecord report = heap.collectYoung();
        var metrics = heap.getHeapMetrics().orElseThrow();
        int collectionsBeforeRecommendation = report.numCollection();
        boolean recommendation = heap.getLastFullGcRecommendation().orElseThrow();

        check(metrics.oldOccupancyPercentage() == 65.0,
                "Old debe estar ocupado al 65 por ciento");
        check(!recommendation,
                "El 65 por ciento no debe recomendar una Full GC");
        check(heap.getLastGcReport().orElseThrow().numCollection() == collectionsBeforeRecommendation,
                "Consultar la recomendación no debe incrementar el contador");
        System.out.println("Full GC recommendation for 65%: " + recommendation);
        System.out.println("Metrics no Full GC recommendation test passed");
    }

    private static void runMetricsRecommendationBeforeFirstYoungGcCase() {
        VirtualHeap heap = new VirtualHeap(new VirtualHeapConfig());
        HeapStatusRecord before = heap.getHeapStatus();

        boolean hasRecommendation = heap.getLastFullGcRecommendation().isPresent();
        HeapStatusRecord after = heap.getHeapStatus();

        check(!hasRecommendation,
                "Un heap nuevo no debe tener recomendación antes de la primera Young GC");
        check(heap.getHeapMetrics().isEmpty(),
                "Un heap nuevo no debe tener métricas antes de la primera Young GC");
        check(heap.getLastGcReport().isEmpty(),
                "Consultar la recomendación no debe crear un reporte");
        check(before.equals(after),
                "Consultar la recomendación no debe modificar el heap");
        System.out.println("Metrics recommendation before first Young GC test passed");
    }

    private static void printGCReport(GCReportRecord report) {
        System.out.println("Objects: " + report.objectsBefore() + " -> " + report.objectsAfter());
        System.out.println("Units: " + report.unitsBefore() + " -> " + report.unitsAfter());
        System.out.println("Reclaimed: " + report.reclaimedObjects() + " objects, "
                + report.reclaimedUnits() + " units");
        System.out.println("Promoted: " + report.promotedObjects() + " objects, "
                + report.promotedUnits() + " units");
    }

    private static void printHeapStatus(HeapStatusRecord status) {
        System.out.println("Eden: " + status.eden().actualSize() + "/" + status.eden().totalSize() + " used, "
                + status.eden().availableSize() + " available");
        for (SimObjectRecord obj : status.eden().storageImage()) {
            System.out.println("  " + obj.name() + ": id=" + obj.id() + ", size=" + obj.size() + ", age=" + obj.age()
                    + ", space=" + obj.space() + ", references=" + obj.references());
        }
        System.out.println();

        System.out.println("S0:  " + status.survivor0().actualSize() + "/" + status.survivor0().totalSize());
        for (SimObjectRecord obj : status.survivor0().storageImage()) {
            System.out.println("  " + obj.name() + ": id=" + obj.id() + ", size=" + obj.size() + ", age=" + obj.age()
                    + ", space=" + obj.space() + ", references=" + obj.references());
        }
        System.out.println();

        System.out.println("S1:  " + status.survivor1().actualSize() + "/" + status.survivor1().totalSize());
        for (SimObjectRecord obj : status.survivor1().storageImage()) {
            System.out.println("  " + obj.name() + ": id=" + obj.id() + ", size=" + obj.size() + ", age=" + obj.age()
                    + ", space=" + obj.space() + ", references=" + obj.references());
        }
        System.out.println();

        System.out.println("Old: " + status.old().actualSize() + "/" + status.old().totalSize());
        for (SimObjectRecord obj : status.old().storageImage()) {
            System.out.println("  " + obj.name() + ": id=" + obj.id() + ", size=" + obj.size() + ", age=" + obj.age()
                    + ", space=" + obj.space() + ", references=" + obj.references());
        }
        System.out.println();
        System.out.println("Roots: " + status.roots());
        System.out.println("Remembered set: " + status.rememberedSet());
        System.out.println("Source: " + status.source());
        System.out.println("Destination: " + status.destination());
    }
}
