import java.io.IOException;
import java.rmi.*;
import java.util.concurrent.ExecutionException;

public interface RMIFileSystem extends Remote {
    // Declaring the method prototypes
    public String createFile(String fileName) throws IOException, ExecutionException, InterruptedException;

    public String createDirectory(String fileName) throws IOException, ExecutionException, InterruptedException;

    public String readFile(String fileName) throws IOException, ExecutionException, InterruptedException;

    public String writeFile(String fileName, String data) throws RemoteException, ExecutionException, InterruptedException;

    public String restoreFiles(String fileName) throws RemoteException, ExecutionException, InterruptedException;

    public String deleteFile(String fileName) throws RemoteException, ExecutionException, InterruptedException;
}
