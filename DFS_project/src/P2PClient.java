import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.rmi.*;
import java.util.*;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class P2PClient {
    // Security variables
    private static SecretKeySpec secretKeySpec;
    private static byte[] key;
    private static String encryptionKey;

    public static String myUserName = "ray";
    public static String myIP = "localhost";
    public static String myPort = "9876";
    public static String masterIP = "localhost";
    public static String masterport = "1234";

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
            System.out.println("Something went wrong in decryption: " + e.toString());
        }
        return null;
    }

    private static String signWithPrivateKey(String msg) {
        try {
            File privateKeyFile = new File("private+" + myUserName + ".key");
            byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());
            KeyFactory keyFactory2 = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey privateKey = keyFactory2.generatePrivate(privateKeySpec);

            Signature sig = Signature.getInstance("SHA1WithRSA");
            sig.initSign(privateKey);
            sig.update(msg.getBytes("UTF8"));
            byte[] signatureBytes = sig.sign();
            return Base64.getUrlEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            System.out.println(e);
        }

        return null;
    }

    public static void create(String filePath, P2PMaster masterObj) {
        try {
            User user = masterObj.getRandomPeer();
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            String response = peer.createFile(encryptedFilePath);
            System.out.println("response: " + response);
            if (response != null) {
                masterObj.updateHashTable(encryptedFilePath, user);
                List<User> userInfo = masterObj.getPeerInfo(encryptedFilePath);
                System.out.println(userInfo);
            }
            System.out.println(response);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void createDirectory(String dirName, P2PMaster masterObj) {
        try {

            User user = masterObj.getRandomPeer();
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String encryptedFilePath = "";
            for (String part : dirName.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            String ans = peer.createDirectory(encryptedFilePath);
            if (ans != null) {
                masterObj.updateHashTable(encryptedFilePath, user);
            }
            System.out.println("Directory created successfully.");
        } catch (Exception e) {
            System.out.println("Directory not created :(");
        }
    }

    public static void read(P2PMaster masterObj, String filePath) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            System.out.println(encryptedFilePath);
            List<User> users = masterObj.getPeerInfo(encryptedFilePath);
            User user = users.get(0);
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String fileData = peer.readFile(encryptedFilePath);
            if (fileData == null) {
                System.out.println("Error: file is empty.");
                return;
            }
            System.out.println("File data: " + decryption(fileData, encryptionKey));
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void read(P2PMaster masterObj, String filePath, String groupName) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            String fileData = masterObj.readOthersFile(encryptedFilePath, myUserName, groupName,
                    signWithPrivateKey(encryptedFilePath + myUserName + groupName));
            if (fileData == null) {
                System.out.println("Error: file is empty.");
                return;
            }
            System.out.println("File data: " + fileData);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void addFileToGroup(P2PMaster masterObj, String filePath, String groupName) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            String fileData = masterObj.addFileToGroup(encryptedFilePath, myUserName, groupName,
                    signWithPrivateKey(encryptedFilePath + myUserName + groupName));
            System.out.println(fileData);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void write(P2PMaster masterObj, String filePath, String data) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            List<User> users = masterObj.getPeerInfo(encryptedFilePath);
            User user = users.get(0);
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            System.out.println("write " + encryptedFilePath + " " + encryption(data, encryptionKey));
            String fileData = peer.writeFile(encryptedFilePath, encryption(data, encryptionKey));
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
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            List<User> users = masterObj.getPeerInfo(encryptedFilePath);
            User user = users.get(0);
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String response = peer.deleteFile(encryption(encryptedFilePath, encryptionKey));
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
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            List<User> users = masterObj.getPeerInfo(encryptedFilePath);
            User user = users.get(0);
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String response = peer.restoreFiles(encryption(encryptedFilePath, encryptionKey));
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
                } else if (userChoice.equals("groupRead")) {
                    System.out.println("Enter groupName: ");
                    String groupName = userScan.nextLine();
                    System.out.println("Enter filePath: ");
                    String fileName = userScan.nextLine();
                    read(masterObj, fileName, groupName);
                } else if (userChoice.equals("addFileToGroup")) {
                    System.out.println("Enter groupName: ");
                    String groupName = userScan.nextLine();
                    System.out.println("Enter filePath: ");
                    String fileName = userScan.nextLine();
                    addFileToGroup(masterObj, fileName, groupName);
                } else if (userChoice.equals("addUserToGroup")) {
                    System.out.println("Enter user to add: ");
                    String userToAdd = userScan.nextLine();
                    System.out.println("Enter groupName: ");
                    String groupName = userScan.nextLine();
                    System.out.println(masterObj.addUserToGroup(myUserName, userToAdd, groupName,
                            signWithPrivateKey(myUserName + userToAdd + groupName)));
                } else if (userChoice.equals("removeUserFromGroup")) {
                    System.out.println("Enter user to remove: ");
                    String userToRemove = userScan.nextLine();
                    System.out.println("Enter groupName: ");
                    String groupName = userScan.nextLine();
                    System.out.println(masterObj.removeUserFromGroup(myUserName, userToRemove, groupName,
                            signWithPrivateKey(myUserName + userToRemove + groupName)));
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
        }
    }

    private static void help() {
        System.out.println("Enter a command:");
        System.out.println("  General      >>> $ [help, exit]");
        System.out.println("  Modify Files >>> $ [createDirectory, create, write, read, delete, restore]");
        System.out.println("  Permissions  >>> $ [addUserToGroup, removeUserFromGroup, addFileToGroup]");
    }
}
