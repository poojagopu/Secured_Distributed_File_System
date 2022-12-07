import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class RMI_DFS extends UnicastRemoteObject implements RMIFileSystem {
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
            } catch (ClassNotFoundException e) {
                System.out.println("Class not found.");
                return;
            } catch (Exception e) {
                System.out.println("An error occurred.");
                return;
            }
        }
        System.out.println("Loaded configuration file " + deletedFilesDB.getPath());
    }

    public String createDirectory(String dirPath) throws IOException, ExecutionException, InterruptedException {
        FutureTask<String> cDir = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String newPath = path + dirPath;
                File theDir = new File(newPath);
                theDir.mkdirs();
                return "Directory created successfully.";
            }
        });
        new Thread(cDir).start();
        return (String) cDir.get();
    }

    @Override
    public String createFile(String filePath) throws IOException, ExecutionException, InterruptedException {
        FutureTask<String> create = new FutureTask<>(new Callable<String>() {
            public String call() throws Exception {
                String newPath = path + filePath;
                File fileObject = new File(newPath);
                // Method createNewFile() method creates blank file
                fileObject.getParentFile().mkdirs();
                try {
                    if (fileObject.createNewFile()) {
                        fileDeletion.put(newPath, false);
                        updateDeletedFiles();
                    } else {
                        return "Error: File already exists.";
                    }
                } catch (Exception e) {
                    return "Error: Unable to add file, check required directories.";
                }
                return "File created successfully.";
            }
        });
        new Thread(create).start();
        return (String) create.get();
    }

    @Override
    public String readFile(String filePath) throws IOException, ExecutionException, InterruptedException {
        FutureTask<String> read = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String ans;
                String newPath = path + filePath;
                StringBuilder str = new StringBuilder();
                // Creating an object of BufferedReader class
                try {
                    if (fileDeletion.get(newPath)) {
                        return null;
                    } else {
                        BufferedReader br = new BufferedReader(new FileReader(newPath));
                        while ((ans = br.readLine()) != null)
                            str.append(ans);
                        br.close();
                    }
                } catch (Exception e) {
                    return "Error: Failed to read the file.";
                }
                return str.toString();
            }
        });
        new Thread(read).start();
        return (String) read.get();
    }

    @Override
    public String writeFile(String FilePath, String data)
            throws RemoteException, ExecutionException, InterruptedException {
        FutureTask<String> write = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String newPath = path + FilePath;
                try {
                    if (fileDeletion.get(newPath)) {
                        return null;
                    } else {
                        FileWriter fw = new FileWriter(newPath);
                        BufferedWriter bw = new BufferedWriter(fw);
                        fw.write(data);
                        bw.close();
                        return "Successfully wrote to the file.";
                    }
                } catch (Exception e) {
                    return "Error: Failed to write to the file.";
                }
            }
        });
        new Thread(write).start();
        return (String) write.get();
    }

    @Override
    public String restoreFiles(String filePath) throws RemoteException, ExecutionException, InterruptedException {
        FutureTask<String> restore = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String newPath = path + filePath;
                if (!fileDeletion.get(newPath)) {
                    return "Error: Failed to restore file.";
                } else {
                    fileDeletion.put(newPath, false);
                    updateDeletedFiles();
                    return "File restored successfully.";
                }
            }
        });

        new Thread(restore).start();
        return (String) restore.get();
    }

    @Override
    public String deleteFile(String filePath) throws RemoteException, ExecutionException, InterruptedException {
        FutureTask<String> delete = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String newPath = path + filePath;
                if (fileDeletion.get(newPath)) {
                    return "Error: Failed to delete file.";
                } else {
                    fileDeletion.put(newPath, true);
                    updateDeletedFiles();
                    return "File deleted successfully.";
                }
            }
        });

        new Thread(delete).start();
        return (String) delete.get();
    }

    private void updateDeletedFiles() {
        try {
            FileOutputStream myFileOutStream = new FileOutputStream(username + "+configurations/deletedFiles");
            ObjectOutputStream myObjectOutStream = new ObjectOutputStream(myFileOutStream);
            myObjectOutStream.writeObject(fileDeletion);
            myObjectOutStream.close();
            myFileOutStream.close();
        } catch (IOException e) {
            System.out.println("An error occurred in updating deleted files.");
        }
    }
}
