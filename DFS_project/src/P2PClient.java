import java.io.File;
import java.nio.charset.StandardCharsets;
import java.rmi.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class P2PClient {

    // Security variables
    private static SecretKeySpec secretKeySpec;
    private static byte[] key;
    private static String encryptionKey;

    public static String myUserName = "ray";
    public String myIP = "localhost";
    public String myPort = "9876";

    // Key setter
    public static void setKey(final String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKeySpec = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // File encryption
    public static String encryption(final String strToEncode, final String key) {
        try {
            setKey(key);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // (or) AES/GCM/NoPadding
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return Base64.getUrlEncoder()
                    .encodeToString(cipher.doFinal(strToEncode.getBytes("UTF-8")));
        } catch (Exception e) {
            System.out.println("Something went wrong in encryption: " + e.toString());
        }
        return null;
    }

    // File decryption
    public static String decryption(final String strToDecode, final String key) {
        try {
            setKey(key);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            return new String(cipher.doFinal(Base64.getUrlDecoder().decode(strToDecode)));
        } catch (Exception e) {
            System.out.println("Something went wrong in decryption : " + e.toString());
        }
        return null;
    }

    public static void create(String filePath, P2PMaster masterObj) {
        try {
            User user = masterObj.getRandomPeer();
            System.out.println("Entering create");
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            System.out.println("Entering create second");
            String response = peer.createFile(encryption(filePath, encryptionKey));
            System.out.println("response: " + response);
            if (response != null) {
                masterObj.updateHashTable(filePath, user);
                List<User> userInfo = masterObj.getPeerInfo(response);
                System.out.println(userInfo);
            }
            System.out.println(response);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public static void createDirectory(String dirName, P2PMaster masterObj) {
        try {

            User user = masterObj.getRandomPeer();
            // System.out.println("Entering create");
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            // System.out.println("Entering create second");
            peer.createDirectory(encryption(dirName, encryptionKey));
            System.out.println("Directory created successfully.");
        } catch (Exception e) {
            System.out.println("Directory not created :(");
            System.out.println(e);
        }
    }

    public static void read(P2PMaster masterObj, String filePath) {
        try {
            List<User> users = masterObj.getPeerInfo(filePath);
            User user = users.get(0);
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String fileData = peer.readFile(encryption(filePath, encryptionKey));
            if (fileData == null) {
                System.out.println("Failed to read file......");
                return;
            }
            System.out.println("File Data : " + decryption(fileData, encryptionKey));
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void write(P2PMaster masterObj, String filePath, String data) {
        try {
            List<User> users = masterObj.getPeerInfo(filePath);
            User user = users.get(0);
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String fileData = peer.writeFile(encryption(filePath, encryptionKey), encryption(data, encryptionKey));
            if (fileData == null) {
                System.out.println("Failed to write file......");
                return;
            }
            System.out.println("Successfully written in the file");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void delete(P2PMaster masterObj, String filePath) {
        try {
            List<User> users = masterObj.getPeerInfo(filePath);
            User user = users.get(0);
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String response = peer.deleteFile(encryption(filePath, encryptionKey));
            if (response == null) {
                System.out.println("Failed to delete file......");
                return;
            }
            System.out.println(response);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void restore(P2PMaster masterObj, String filePath) {
        try {
            List<User> users = masterObj.getPeerInfo(filePath);
            User user = users.get(0);
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String response = peer.restoreFiles(encryption(filePath, encryptionKey));
            if (response == null) {
                System.out.println("Failed to restore file......");
                return;
            }
            System.out.println(response);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String args[]) {
        String masterIP = "localhost";
        String masterport = "1234";
        String serverAnswer;
        String userChoice;

        try {
            File myObj = new File("secret+" + myUserName + ".key");
            if (myObj.exists()) {
                Scanner myReader = new Scanner(myObj);
                if (myReader.hasNextLine()) {
                    encryptionKey = myReader.nextLine();
                    myReader.close();
                } else {
                    myReader.close();
                    throw new Exception("Unable to find encryption key");
                }
            } else {
                throw new Exception("Unable to find encryption key");
            }

            // lookup method to find reference of remote object
            P2PMaster masterObj = (P2PMaster) Naming.lookup("rmi://" + masterIP + ":" + masterport + "/master");

            Scanner userScan = new Scanner(System.in);
            help();

            do {
                System.out.print("Enter your choice: ");
                System.out.print("$ ");
                userChoice = userScan.nextLine();

                if (userChoice.equals("help")) {
                    help();
                } else if (userChoice.equals("create")) {
                    System.out.println("Enter filePath: ");
                    String fileName = userScan.nextLine();
                    create(fileName, masterObj);
                } else if (userChoice.equals("createDirectory")) {
                    System.out.println("Enter directory name: ");
                    String dirName = userScan.nextLine();
                    createDirectory(dirName, masterObj);
                } else if (userChoice.equals("write")) {
                    System.out.println("Enter filePath: ");
                    String fileName = userScan.nextLine();
                    System.out.println("Start Writing....");
                    String data = userScan.nextLine();
                    write(masterObj, fileName, data);
                } else if (userChoice.equals("read")) {
                    System.out.println("Enter filePath: ");
                    String fileName = userScan.nextLine();
                    read(masterObj, fileName);
                } else if (userChoice.equals("restore")) {
                    System.out.println("Enter filePath: ");
                    String fileName = userScan.nextLine();
                    restore(masterObj, fileName);
                } else if (userChoice.equals("delete")) {
                    System.out.println("Enter filePath: ");
                    String fileName = userScan.nextLine();
                    delete(masterObj, fileName);
                } else if (!userChoice.equals("exit")) {
                    System.out.println("Sorry, please enter valid command.");
                }
            } while (!userChoice.equals("exit"));

            userScan.close();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private static void help() {
        System.out.println("Enter a command:");
        System.out.println("  >>> $ [help, exit, createDirectory, create, write, read, delete, restore]");
    }
}
