import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class DistributedFileSystem extends UnicastRemoteObject implements FileSystem {
    private Map<Integer, List<String>> users;

    DistributedFileSystem() throws RemoteException, IOException {
        super();

        // Set up config file for storing authorized users
        users = new HashMap<Integer, List<String>>();
        File usersDB = new File("configurations/users");
        usersDB.getParentFile().mkdirs();
        if (usersDB.createNewFile()) {
            System.out.println("Created configuration file " + usersDB.getPath());
        } else {
            BufferedReader br = null;

            try {
                br = new BufferedReader(new FileReader(usersDB));
                String line = null;

                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(":");

                    int userID = Integer.parseInt(parts[0].trim());
                    String userName = parts[1].trim();
                    String userPublicKey = parts[2].trim();

                    if (userID > -1 && !userName.equals("") && !userPublicKey.equals("")) {
                        ArrayList<String> nameAndKey = new ArrayList<String>();
                        nameAndKey.add(userName);
                        nameAndKey.add(userPublicKey);
                        users.put(userID, nameAndKey);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception e) {
                        System.out.println("An error occurred.");
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Loaded configuration file " + usersDB.getPath());
        }
    }

    @Override
    public int registerUser(String userName, String publicKey) {
        return -1;
    }

    /**
     * @param fileName
     * @throws RemoteException
     */
    @Override
    public String createFile(String fileName) throws IOException {
        File fileObject = new File(fileName);
        // Method createNewFile() method creates blank file
        if (fileObject.createNewFile()) {
            System.out.println("File created: " + fileObject.getName());
        } else {
            System.out.println("File already exists.");
            return "File already exists";
        }
        return "File created successfully.";
    }

    /**
     * @param fileName
     * @throws RemoteException
     */
    @Override
    public String readFile(String filePath) throws IOException {
        String ans;
        StringBuilder str = new StringBuilder();
        // Creating an object of BufferedReader class
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            while ((ans = br.readLine()) != null)
                str.append(ans);
            System.out.println("Read successfully");
            br.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return str.toString();
    }

    /**
     * @param fileName
     * @throws RemoteException
     */
    @Override
    public String writeFile(String fileName, String data) throws RemoteException {
        try {
            FileWriter fw = new FileWriter(fileName);
            BufferedWriter bw = new BufferedWriter(fw);
            System.out.println("Started writing");
            fw.write(data);
            bw.close();
            System.out.println("Written successfully");
            return "Written successfully";
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return "An error occurred.";
        }
    }

    /**
     * @param fileName
     * @throws RemoteException
     */
    @Override
    public void restoreFiles(String fileName) throws RemoteException {

    }

    /**
     * @param fileName
     * @return
     * @throws RemoteException
     */
    @Override
    public String deleteFile(String fileName) throws RemoteException {
        String ans = "";
        try {
            File file = new File(fileName);

            if (file.delete()) {
                ans = "File deleted successfully";
            } else {
                ans = "Failed to delete the file";
            }
        } catch (SecurityException e) {
            ans = "An error occurred.";
            e.printStackTrace();
        }
        System.out.println(ans);
        return ans;
    }
}
