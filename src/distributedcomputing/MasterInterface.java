package distributedcomputing;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MasterInterface extends Remote {
    Task getTask(String workerId) throws RemoteException;
    void submitResult(Result result, String workerId) throws RemoteException;
}