import java.io.IOException;
import java.rmi.*;

public interface FileSystem extends Remote {
    // Declaring the method prototypes
    public int registerUser(String userName, String publicKey) throws RemoteException;

    public String createFile(String fileName) throws IOException;

    public String readFile(String fileName) throws IOException;

    public String writeFile(String fileName, String data) throws RemoteException;

    public void restoreFiles(String fileName) throws RemoteException;

    public String deleteFile(String fileName) throws RemoteException;
}
