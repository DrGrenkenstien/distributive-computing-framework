package distributedcomputing;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WorkerRegistryInterface extends Remote {
    void registerWorker(String workerId) throws RemoteException;
    void unregisterWorker(String workerId) throws RemoteException;
}