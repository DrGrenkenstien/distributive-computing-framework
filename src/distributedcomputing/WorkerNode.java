package distributedcomputing;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorkerNode {
    private static final Map<String, TaskExecutor> taskExecutors = new HashMap<>();
    private String workerId;
    private WorkerRegistryInterface registry;
    private MasterInterface master;
    private boolean running = true;
    private String masterHost;
    private int masterPort;

    static {
        taskExecutors.put("genomic", new GenomicSequenceAnalysisTask());
    }

    public WorkerNode(String masterHost, int masterPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.workerId = UUID.randomUUID().toString();
    }

    public void start() {
        try {
            Registry rmiRegistry = LocateRegistry.getRegistry(masterHost, masterPort);
            master = (MasterInterface) rmiRegistry.lookup("Master");
            registry = (WorkerRegistryInterface) rmiRegistry.lookup("WorkerRegistry");

            registry.registerWorker(workerId);
            System.out.println("Worker " + workerId + " connected to Master at " + masterHost + ":" + masterPort);

            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            while (running) {
                Task task = master.getTask(workerId);  // Pass workerId here
                if (task != null) {
                    System.out.println("Received task: " + task.getId() + " of type: " + task.getTaskType());
                    Result result = performTask(task);
                    master.submitResult(result, workerId);  // Pass workerId here
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

    private void shutdown() {
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

    private Result performTask(Task task) {
        String taskCategory = task.getTaskType().split(":")[0];
        TaskExecutor executor = taskExecutors.get(taskCategory);
        if (executor != null) {
            return executor.execute(task);
        } else {
            return new Result(task.getId(), "Unsupported task type: " + task.getTaskType());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java WorkerNode <masterHost> <masterPort>");
            System.exit(1);
        }

        String masterHost = args[0];
        int masterPort = Integer.parseInt(args[1]);

        WorkerNode workerNode = new WorkerNode(masterHost, masterPort);
        workerNode.start();
    }
}