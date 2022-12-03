import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Array;
import java.util.*;
@SuppressWarnings("unchecked")
public class P2PMasterImpl extends UnicastRemoteObject implements P2PMaster {

    public HashMap<String,List<User> >fileUsers;
    public Set<User> allUsers; //username -> userPublicKey

    protected P2PMasterImpl() throws IOException {
        super();
        allUsers=new HashSet<>();
        fileUsers=new HashMap<>();

        // Set up config file for storing authorized users
        File usersDB = new File("configurations/users");
        usersDB.getParentFile().mkdirs();
        if (usersDB.createNewFile())
            System.out.println("Created configuration file " + usersDB.getPath());
        if (usersDB.length() != 0) {
            try {
                FileInputStream fis = new FileInputStream(usersDB.getPath());
                ObjectInputStream ois = new ObjectInputStream(fis);
                allUsers = (HashSet<User>) ois.readObject();
                fis.close();
                ois.close();
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
                return;
            } catch (ClassNotFoundException e) {
                System.out.println("Clasrs not found.");
                e.printStackTrace();
                return;
            }
        } else {
            allUsers = new HashSet<>();
        }
        System.out.println("Loaded configuration file " + usersDB.getPath());
    }

    @Override
    public List<User> getPeerInfo(String filePath) throws RemoteException {
        if(fileUsers.containsKey(filePath)){
            return fileUsers.get(filePath);
        }
        return null;
    }


    @Override
    public String registerUser(String userIP, String userPort, String userPublicKey) throws RemoteException {
        String ans;
        User newUser=new User(userIP,userPort, userPublicKey);
        if (allUsers.contains(newUser)) {
            ans = "User '" + newUser.ip + "' already exists";
            return ans;
        }

        allUsers.add(newUser);
        updateUsers();
        ans = "Registered new user '" + userIP + "'";
        System.out.println(ans);
        return ans;
    }

    @Override
    public User getRandomPeer() throws RemoteException {
        System.out.println("USers we have:");
        System.out.println(allUsers);
        User[] userArray = allUsers.toArray(new User[allUsers.size()]);
        // generate a random number
        Random random = new Random();
        // this will generate a random number between 0 and
        // HashSet.size - 1
        int randomNumber = random.nextInt(allUsers.size());
        return userArray[randomNumber];
    }

    @Override
    public void updateHashTable(String filePath, User user) {
        List<User>users;
        if(fileUsers.containsKey(filePath)){
            users = fileUsers.get(filePath);
            users.add(user);
        }
        else{
            users=new ArrayList<>();
            users.add(user);
        }
    }

    private void updateUsers() {
        try {
            FileOutputStream myFileOutStream = new FileOutputStream("configurations/users");
            ObjectOutputStream myObjectOutStream = new ObjectOutputStream(myFileOutStream);
            myObjectOutStream.writeObject(allUsers);
            myObjectOutStream.close();
            myFileOutStream.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}