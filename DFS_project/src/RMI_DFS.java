import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
@SuppressWarnings("unchecked")
public class RMI_DFS extends UnicastRemoteObject implements RMIFileSystem {
    private Map<String, String> users; //username -> userPublicKey
    private HashMap<String, Boolean> fileDeletion; //filePath-> isDeleted
    @SuppressWarnings("unchecked")
    RMI_DFS() throws RemoteException, IOException {
        super();
        fileDeletion=new HashMap<>();
    }

    @Override
    public String createFile(String filePath) throws IOException {
        File fileObject = new File(filePath);
        // Method createNewFile() method creates blank file
        if (fileObject.createNewFile()) {
            fileDeletion.put(filePath,false);
            System.out.println("File created: " + fileObject.getName());
        } else {
            System.out.println("File already exists.");
            return "File already exists";
        }
        return "File created successfully.";
    }

    @Override
    public String readFile(String filePath) throws IOException {
        String ans;
        StringBuilder str = new StringBuilder();
        // Creating an object of BufferedReader class
        try {
            if(fileDeletion.get(filePath)){
                return "Can't read file as it is deleted";
            }else{
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
        try {
            if(fileDeletion.get(FilePath)){
                return "Can't write into file as it is deleted";
            }else{
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
        if(!fileDeletion.get(filePath)){
           return "File can't be restored as file is not deleted";
        }else{
            fileDeletion.put(filePath,false);
            return "File restored successfully";
        }
    }

    @Override
    public String deleteFile(String filePath) throws RemoteException {
        if(fileDeletion.get(filePath)){
            return "File already Deleted";
        }else{
            fileDeletion.put(filePath,true);
            return "File Deleted Successfully";
        }

    }
}
