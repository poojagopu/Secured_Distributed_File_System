import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
@SuppressWarnings("unchecked")
public class RMI_DFS extends UnicastRemoteObject implements RMIFileSystem {
    private Map<String, String> users; //username -> userPublicKey
    String path;
    private HashMap<String, Boolean> fileDeletion; //filePath-> isDeleted
    @SuppressWarnings("unchecked")
    RMI_DFS() throws RemoteException, IOException {
        super();
        fileDeletion=new HashMap<>();
        path="myFiles/";//for mac users use reverse slash
    }

    public void createDirectory(String dirPath) throws IOException{
        dirPath=path+dirPath;
        File theDir = new File(dirPath);
        if (!theDir.exists()){
            theDir.mkdirs();
        }
        return;
    }
    @Override
    public String createFile(String filePath) throws IOException {
        filePath=path+filePath;
        File fileObject = new File(filePath);
        // Method createNewFile() method creates blank file
        try {
        if (fileObject.createNewFile()) {
            fileDeletion.put(filePath,false);
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
        filePath=path+filePath;
        StringBuilder str = new StringBuilder();
        // Creating an object of BufferedReader class
        try {
            if(fileDeletion.get(filePath)){
                return null;
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
            FilePath=path+FilePath;
            if(fileDeletion.get(FilePath)){
                return null;
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
        filePath=path+filePath;
        if(!fileDeletion.get(filePath)){
           return null;
        }else{
            fileDeletion.put(filePath,false);
            return "File restored successfully";
        }
    }

    @Override
    public String deleteFile(String filePath) throws RemoteException {
        filePath=path+filePath;
        if(fileDeletion.get(filePath)){
            return null;
        }else{
            fileDeletion.put(filePath,true);
            return "File Deleted Successfully";
        }

    }
}
