import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class RMI_DFS extends UnicastRemoteObject implements RMIFileSystem {
    // private Map<String, String> users; // username -> userPublicKey
    String path;
    private HashMap<String, Boolean> fileDeletion; // filePath-> isDeleted
    String username;

    @SuppressWarnings("unchecked")
    RMI_DFS(String username) throws RemoteException, IOException {
        super();
        fileDeletion = new HashMap<>();
        path = "myFiles";
        this.username = username;

        // Set up config file for deletedFiles
        File deletedFilesDB = new File(username + "+configurations/deletedFiles");
        deletedFilesDB.getParentFile().mkdirs();
        if (deletedFilesDB.createNewFile())
            System.out.println("Created configuration file " + deletedFilesDB.getPath());
        if (deletedFilesDB.length() != 0) {
            try {
                FileInputStream fis = new FileInputStream(deletedFilesDB.getPath());
                ObjectInputStream ois = new ObjectInputStream(fis);
                fileDeletion = (HashMap<String, Boolean>) ois.readObject();
                fis.close();
                ois.close();
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
                return;
            } catch (ClassNotFoundException e) {
                System.out.println("Class not found.");
                e.printStackTrace();
                return;
            }
        }
        System.out.println("Loaded configuration file " + deletedFilesDB.getPath());
    }

    public String createDirectory(String dirPath) throws IOException {
        dirPath = path + dirPath;
        File theDir = new File(dirPath);
        theDir.mkdirs();
        return "Directory created successfully.";
    }

    @Override
    public String createFile(String filePath) throws IOException {
        filePath = path + filePath;
        File fileObject = new File(filePath);
        // Method createNewFile() method creates blank file
        fileObject.getParentFile().mkdirs();
        try {
            if (fileObject.createNewFile()) {
                fileDeletion.put(filePath, false);
                updateDeletedFiles();
                System.out.println("File created: " + fileObject.getName());
            } else {
                System.out.println("File already exists.");
                return "File already exists";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "File created successfully.";
    }

    @Override
    public String readFile(String filePath) throws IOException {
        String ans;
        filePath = path + filePath;
        StringBuilder str = new StringBuilder();
        // Creating an object of BufferedReader class
        try {
            if (fileDeletion.get(filePath)) {
                return null;
            } else {
                BufferedReader br = new BufferedReader(new FileReader(filePath));
                while ((ans = br.readLine()) != null)
                    str.append(ans);
                System.out.println("Read successfully");
                br.close();
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return str.toString();
    }

    @Override
    public String writeFile(String FilePath, String data) throws RemoteException {
        FilePath = path + FilePath;
        try {
            if (fileDeletion.get(FilePath)) {
                return null;
            } else {
                FileWriter fw = new FileWriter(FilePath);
                BufferedWriter bw = new BufferedWriter(fw);
                System.out.println("Started writing");
                fw.write(data);
                bw.close();
                System.out.println("Written successfully");
                return "Written successfully";
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return "An error occurred.";
        }
    }

    @Override
    public String restoreFiles(String filePath) throws RemoteException {
        filePath = path + filePath;
        if (!fileDeletion.get(filePath)) {
            return null;
        } else {
            fileDeletion.put(filePath, false);
            updateDeletedFiles();
            return "File restored successfully";
        }
    }

    @Override
    public String deleteFile(String filePath) throws RemoteException {
        filePath = path + filePath;
        if (fileDeletion.get(filePath)) {
            return null;
        } else {
            fileDeletion.put(filePath, true);
            updateDeletedFiles();
            return "File Deleted Successfully";
        }
    }

    private void updateDeletedFiles() {
        try {
            FileOutputStream myFileOutStream = new FileOutputStream(username + "+configurations/deletedFiles");
            ObjectOutputStream myObjectOutStream = new ObjectOutputStream(myFileOutStream);
            myObjectOutStream.writeObject(fileDeletion);
            myObjectOutStream.close();
            myFileOutStream.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
