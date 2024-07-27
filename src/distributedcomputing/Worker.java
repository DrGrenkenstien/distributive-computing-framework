// Worker.java
package distributedcomputing;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Worker {
    private static final Map<String, TaskExecutor> taskExecutors = new HashMap<>();
    private static String workerId;
    private static WorkerRegistryInterface registry;
    private static MasterInterface master;
    private static boolean running = true;

    static {
        taskExecutors.put("genomic", new GenomicSequenceAnalysisTask());
    }

    public static void main(String[] args) {
        try {
            workerId = UUID.randomUUID().toString();
            Registry rmiRegistry = LocateRegistry.getRegistry("localhost", 1099);
            master = (MasterInterface) rmiRegistry.lookup("Master");
            registry = (WorkerRegistryInterface) rmiRegistry.lookup("WorkerRegistry");

            registry.registerWorker(workerId);
            System.out.println("Worker " + workerId + " connected to Master");

            Runtime.getRuntime().addShutdownHook(new Thread(Worker::shutdown));

            while (running) {
                Task task = master.getTask(workerId);
                if (task != null) {
                    System.out.println("Received task: " + task.getId() + " of type: " + task.getTaskType());
                    Result result = performTask(task);
                    master.submitResult(result, workerId);
                    System.out.println("Submitted result for task: " + task.getId());
                } else {
                    System.out.println("No task available, waiting...");
                }
                Thread.sleep(1000);  // Wait before asking for next task
            }
        } catch (Exception e) {
            System.err.println("Worker exception: " + e.toString());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private static void shutdown() {
        try {
            running = false;
            if (registry != null) {
                registry.unregisterWorker(workerId);
                System.out.println("Worker " + workerId + " unregistered");
            }
        } catch (Exception e) {
            System.err.println("Error during worker shutdown: " + e.toString());
        }
    }

    private static Result performTask(Task task) {
        String taskCategory = task.getTaskType().split(":")[0];
        TaskExecutor executor = taskExecutors.get(taskCategory);
        if (executor != null) {
            return executor.execute(task);
        } else {
            return new Result(task.getId(), "Unsupported task type: " + task.getTaskType());
        }
    }
}