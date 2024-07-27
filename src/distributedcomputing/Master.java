package distributedcomputing;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.core.type.TypeReference;

public class Master extends UnicastRemoteObject implements MasterInterface, WorkerRegistryInterface {
    private Queue<Task> taskQueue;
    private Queue<Result> resultQueue;
    private Map<String, Integer> workerLoad;
    private Map<String, Long> workerLastTask;

    public Master() throws RemoteException {
        super();
        taskQueue = new LinkedList<>();
        resultQueue = new LinkedList<>();
        workerLoad = new ConcurrentHashMap<>();
        workerLastTask = new ConcurrentHashMap<>();
    }

    @Override
    public Task getTask(String workerId) throws RemoteException {
        if (taskQueue.isEmpty()) {
            return null;
        }

        // Simple load balancing: assign task to worker with least load
        if (workerLoad.getOrDefault(workerId, 0) > Collections.min(workerLoad.values())) {
            return null;
        }

        Task task = taskQueue.poll();
        if (task != null) {
            workerLoad.put(workerId, workerLoad.getOrDefault(workerId, 0) + 1);
            workerLastTask.put(workerId, System.currentTimeMillis());
            System.out.println("Sending task: " + task.getId() + " to worker: " + workerId);
        }
        return task;
    }

    @Override
    public void submitResult(Result result, String workerId) throws RemoteException {
        System.out.println("Received result for task: " + result.getTaskId() + " from worker: " + workerId);
        resultQueue.offer(result);
        workerLoad.put(workerId, workerLoad.get(workerId) - 1);
    }

    @Override
    public void registerWorker(String workerId) throws RemoteException {
        workerLoad.put(workerId, 0);
        workerLastTask.put(workerId, 0L);
        System.out.println("Worker " + workerId + " joined the network");
    }

    @Override
    public void unregisterWorker(String workerId) throws RemoteException {
        workerLoad.remove(workerId);
        workerLastTask.remove(workerId);
        System.out.println("Worker " + workerId + " left the network");
    }

    public void addTask(Task task) {
        taskQueue.offer(task);
        System.out.println("Added task: " + task.getId() + " to the queue");
    }

    public Result getResult() {
        return resultQueue.poll();
    }

    private void loadTasksFromJson(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            String json = jsonContent.toString();
            int sequencesStart = json.indexOf("[");
            int sequencesEnd = json.lastIndexOf("]");
            String sequencesJson = json.substring(sequencesStart + 1, sequencesEnd);

            String[] sequenceObjects = sequencesJson.split("\\},\\{");
            for (String sequenceObject : sequenceObjects) {
                sequenceObject = sequenceObject.replaceAll("[{}\"]", "");
                String[] parts = sequenceObject.split(",");
                int id = 0;
                List<String> reads = new ArrayList<>();

                for (String part : parts) {
                    String[] keyValue = part.split(":");
                    if (keyValue[0].trim().equals("id")) {
                        id = Integer.parseInt(keyValue[1].trim());
                    } else if (keyValue[0].trim().equals("reads")) {
                        String[] readArray = keyValue[1].trim().split("\\[|\\]")[1].split(",");
                        for (String read : readArray) {
                            reads.add(read.trim());
                        }
                    }
                }

                addTask(new Task(id, "genomic:denovo_assembly", reads));
            }
        } catch (Exception e) {
            System.err.println("Error loading tasks from JSON: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Master master = new Master();

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("Master", master);
            registry.rebind("WorkerRegistry", master);

            System.out.println("Master ready on port 1099");

            // Load tasks from JSON file
            master.loadTasksFromJson("genome_sequences.json");

            // Process results
            while (true) {
                Result result = master.getResult();
                if (result != null) {
                    System.out.println("Processed result for task: " + result.getTaskId());
                    System.out.println("Assembled genome: " + result.getOutput());
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.err.println("Master exception: " + e.toString());
            e.printStackTrace();
        }
    }
}