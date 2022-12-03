import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class P2PServer {
    public static void main(String args[]) {
        String masterIP="192.168.56.1";
        String masterPort="1234";
        String userIP="192.168.56.1";
        String userPort="5678";
        String userPublicKey="abcd";
        try {

            P2PMaster masterObj= (P2PMaster) Naming.lookup("rmi://"+masterIP+":"+masterPort+"/master");
            // Create an object of the interface
            // implementation class
            String response = masterObj.registerUser(userIP, userPort, userPublicKey);
            if(response!=null){
               System.out.println(response);
            }else{
                System.out.println("Error: User not registered");
            }
            RMIFileSystem interface_obj = new RMI_DFS();

            // rmiregistry within the server JVM with
            // port number 1234
            LocateRegistry.createRegistry(Integer.parseInt(userPort));

            // Binds the remote object by the name
            // usingRMI
            Naming.rebind("rmi://"+userIP+":"+userPort + "/master", interface_obj);
        } catch (Exception ae) {
            System.out.println(ae);
        }
    }
}
