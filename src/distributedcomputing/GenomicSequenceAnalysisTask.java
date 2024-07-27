package distributedcomputing;

import java.util.*;

public class GenomicSequenceAnalysisTask implements TaskExecutor {
    @Override
    public Result execute(Task task) {
        if ("genomic:denovo_assembly".equals(task.getTaskType())) {
            List<String> reads = (List<String>) task.getData();
            String assembledGenome = performDeNovoAssembly(reads);
            return new Result(task.getId(), assembledGenome);
        } else {
            throw new IllegalArgumentException("Unsupported genomic analysis task: " + task.getTaskType());
        }
    }

    private String performDeNovoAssembly(List<String> reads) {
        // Step 1: Find overlaps between reads
        Map<String, List<String>> overlaps = findOverlaps(reads);

        // Step 2: Construct a graph from the overlaps
        Map<String, Set<String>> graph = constructGraph(overlaps);

        // Step 3: Find the Hamiltonian path (simplified)
        List<String> path = findPath(graph);

        // Step 4: Generate the consensus sequence
        return generateConsensus(path, overlaps);
    }

    private Map<String, List<String>> findOverlaps(List<String> reads) {
        Map<String, List<String>> overlaps = new HashMap<>();
        for (String read1 : reads) {
            for (String read2 : reads) {
                if (read1.equals(read2)) continue;
                int overlapLength = findOverlap(read1, read2);
                if (overlapLength > 0) {
                    overlaps.computeIfAbsent(read1, k -> new ArrayList<>()).add(read2);
                }
            }
        }
        return overlaps;
    }

    private int findOverlap(String read1, String read2) {
        for (int i = 1; i < read1.length(); i++) {
            if (read2.startsWith(read1.substring(i))) {
                return read1.length() - i;
            }
        }
        return 0;
    }

    private Map<String, Set<String>> constructGraph(Map<String, List<String>> overlaps) {
        Map<String, Set<String>> graph = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : overlaps.entrySet()) {
            graph.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return graph;
    }

    private List<String> findPath(Map<String, Set<String>> graph) {
        List<String> path = new ArrayList<>();
        String start = graph.keySet().iterator().next(); // Arbitrary starting point
        path.add(start);

        while (!graph.get(start).isEmpty()) {
            String next = graph.get(start).iterator().next();
            path.add(next);
            graph.get(start).remove(next);
            start = next;
        }

        return path;
    }

    private String generateConsensus(List<String> path, Map<String, List<String>> overlaps) {
        StringBuilder consensus = new StringBuilder(path.get(0));
        for (int i = 1; i < path.size(); i++) {
            String current = path.get(i);
            String previous = path.get(i - 1);
            int overlapLength = findOverlap(previous, current);
            consensus.append(current.substring(overlapLength));
        }
        return consensus.toString();
    }
}