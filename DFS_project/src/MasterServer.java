import java.rmi.*;
import java.rmi.registry.*;

public class MasterServer {
    public void start() {
        try {
            // Create an object of the interface
            // implementation class
            P2PMaster interface_obj = new P2PMasterImpl();

            // rmiregistry within the server JVM with
            // port number 1234
            LocateRegistry.createRegistry(1234);

            // Binds the remote object by the name
            // usingRMI
            Naming.rebind("rmi://localhost:1234" + "/master", interface_obj);
        } catch (Exception ae) {
            System.out.println(ae);
        }
    }

    public static void main(String args[]) {
        MasterServer master = new MasterServer();
        master.start();
    }
}
