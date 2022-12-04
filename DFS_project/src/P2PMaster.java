import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.security.*;

public interface P2PMaster extends Remote {
    // return all users which have that given file
    public List<User> getPeerInfo(String filePath) throws RemoteException;

    public String registerUser(String userName, String userIP, String userPort, PublicKey userPublicKey)
            throws RemoteException;

    // returns a random peer to create a file
    public User getRandomPeer() throws RemoteException;

    public String addUserToGroup(String currentUser, String userToAdd, String group, String challenge)
            throws RemoteException;

    public String addFileToGroup(String currentUser, String userToAdd, String group, String challenge)
            throws RemoteException;

    public String removeUserFromGroup(String currentUser, String userToAdd, String group, String challenge)
            throws RemoteException;

    public String readOthersFile(String encryptedFilePath, String userName, String groupName, String otherUser, String signature)
            throws IOException, RemoteException;

    public void updateHashTable(String filePath, User user) throws RemoteException;
}
