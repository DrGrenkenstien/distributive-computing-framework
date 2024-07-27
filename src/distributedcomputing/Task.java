package distributedcomputing;

import java.io.Serializable;
import java.util.List;

public class Task implements Serializable {
    private int id;
    private String taskType;
    private Object data;

    public Task(int id, String taskType, Object data) {
        this.id = id;
        this.taskType = taskType;
        this.data = data;
    }

    public int getId() { return id; }
    public String getTaskType() { return taskType; }
    public Object getData() { return data; }
}