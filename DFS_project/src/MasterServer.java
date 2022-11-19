import java.rmi.*;
import java.rmi.registry.*;
import java.util.HashMap;

public class MasterServer {
    public static void main(String args[]){
        try{
            // Create an object of the interface
            // implementation class

            FileSystem interface_obj = new DistributedFileSystem();

            // rmiregistry within the server JVM with
            // port number 1900
            LocateRegistry.createRegistry(1234);

            // Binds the remote object by the name
            // geeksforgeeks
            Naming.rebind("rmi://localhost:1234"+ "/usingRMI",interface_obj);
        }
        catch(Exception ae)
        {
            System.out.println(ae);
        }
    }

}

