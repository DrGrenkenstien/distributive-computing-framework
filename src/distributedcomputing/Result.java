package distributedcomputing;

import java.io.Serializable;

public class Result implements Serializable {
    private int taskId;
    private Object output;

    public Result(int taskId, Object output) {
        this.taskId = taskId;
        this.output = output;
    }

    public int getTaskId() { return taskId; }
    public Object getOutput() { return output; }
}