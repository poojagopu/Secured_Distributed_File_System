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
    private SecretKeySpec secretKeySpec;
    private byte[] key;
    private String encryptionKey;

    public String myUserName, myIP, myPort, masterIP, masterport;
    public P2PMaster masterObj;

    P2PClient(String myUserName, String myIP, String myPort, String masterIP, String masterPort) {
        this.myUserName = myUserName;
        this.myIP = myIP;
        this.myPort = myPort;
        this.masterIP = masterIP;
        this.masterport = masterPort;
        this.masterObj = null;

        try {
            File myObj = new File("secret+" + this.myUserName + ".key");
            if (myObj.exists()) {
                Scanner myReader = new Scanner(myObj);
                if (myReader.hasNextLine()) {
                    this.encryptionKey = myReader.nextLine();
                    myReader.close();
                } else {
                    myReader.close();
                    throw new Exception("Unable to find encryption key");
                }
            } else {
                throw new Exception("Unable to find encryption key");
            }

            // lookup method to find reference of remote object
            this.masterObj = (P2PMaster) Naming.lookup("rmi://" + this.masterIP + ":" + this.masterport + "/master");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Key setter
    public void setKey(final String myKey) {
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
    public String encryption(final String strToEncode, final String key) {
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
    public String decryption(final String strToDecode, final String key) {
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

    private String signWithPrivateKey(String msg) {
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

    public String create(String filePath) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            String response = null;
            List<User> connectedServers = this.masterObj.getConnectedServers();
            for (User user : connectedServers) {
                RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
                response = peer.createFile(encryptedFilePath);
            }
            if (response != null) {
                this.masterObj.updateHashTable(encryptedFilePath, connectedServers, myUserName, "File");
                return response;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public String createDirectory(String dirName) {
        try {
            String encryptedFilePath = "";
            for (String part : dirName.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            String ans = null;
            List<User> connectedServers = this.masterObj.getConnectedServers();
            for (User user : connectedServers) {
                RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
                ans = peer.createDirectory(encryptedFilePath);
            }
            if (ans != null) {
                this.masterObj.updateHashTable(encryptedFilePath, connectedServers, myUserName, "Directory");
                return ans;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return "Failed to create directory.";
    }

    public String read(String filePath) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            List<User> users = this.masterObj.getPeerInfo(encryptedFilePath);
            User user = users.get(0);
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String fileData = peer.readFile(encryptedFilePath);
            if (fileData == null) {
                return "Notice: File is empty.";
            }
            return decryption(fileData, encryptionKey);
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public String read(String filePath, String groupName, String userName) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            String fileData = this.masterObj.readOthersFile(encryptedFilePath, myUserName, groupName, userName,
                    signWithPrivateKey(encryptedFilePath + myUserName + groupName + userName));
            if (fileData == null) {
                return "Notice: File is empty.";
            }
            return fileData;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public String addFileToGroup(String filePath, String groupName) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            String fileData = this.masterObj.addFileToGroup(encryptedFilePath, myUserName, groupName,
                    signWithPrivateKey(encryptedFilePath + myUserName + groupName));
            return fileData;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public String addDirectoryToGroup(String filePath, String groupName) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            String fileData = this.masterObj.addDirectoryToGroup(encryptedFilePath, myUserName, groupName,
                    signWithPrivateKey(encryptedFilePath + myUserName + groupName));
            return fileData;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public String removeDirectoryFromGroup(String filePath, String groupName) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            String fileData = this.masterObj.removeDirectoryFromGroup(encryptedFilePath, myUserName, groupName,
                    signWithPrivateKey(encryptedFilePath + myUserName + groupName));
            return fileData;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public String write(String filePath, String data) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            String fileData = null;
            List<User> connectedServers = this.masterObj.getConnectedServers();
            for (User user : connectedServers) {
                RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
                fileData = peer.writeFile(encryptedFilePath, encryption(data, encryptionKey));
            }
            return fileData;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public String delete(String filePath) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            List<User> users = this.masterObj.getPeerInfo(encryptedFilePath);
            User user = users.get(0);
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String response = peer.deleteFile(encryption(encryptedFilePath, encryptionKey));
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public String restore(String filePath) {
        try {
            String encryptedFilePath = "";
            for (String part : filePath.split("/")) {
                encryptedFilePath += "/" + encryption(part, encryptionKey);
            }
            List<User> users = this.masterObj.getPeerInfo(encryptedFilePath);
            User user = users.get(0);
            RMIFileSystem peer = (RMIFileSystem) Naming.lookup("rmi://" + user.ip + ":" + user.port + "/master");
            String response = peer.restoreFiles(encryption(encryptedFilePath, encryptionKey));
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public String addUserToGroup(String userToAdd, String groupName) {
        try {
            return this.masterObj.addUserToGroup(this.myUserName, userToAdd, groupName,
                    this.signWithPrivateKey(this.myUserName + userToAdd + groupName));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String removeUserFromGroup(String userToRemove, String groupName) {
        try {
            return this.masterObj.removeUserFromGroup(this.myUserName, userToRemove, groupName,
                    this.signWithPrivateKey(this.myUserName + userToRemove + groupName));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String args[]) {
        String userChoice;

        if (args.length != 5) {
            System.out.println(
                    "Please supply five arguments: (1) userName, (2) userIP, (3) userPort, (4) masterIP, and (5) masterPort");
            return;
        }

        P2PClient client = new P2PClient(args[0], args[1], args[2], args[3], args[4]);

        try {
            Scanner userScan = new Scanner(System.in);
            client.help();

            do {
                System.out.print("\n$ ");
                userChoice = userScan.nextLine();

                if (userChoice.equals("help")) {
                    client.help();
                } else if (userChoice.equals("create")) {
                    System.out.print("Enter filepath: ");
                    String fileName = userScan.nextLine();
                    System.out.println(client.create(fileName));
                } else if (userChoice.equals("createDirectory")) {
                    System.out.print("Enter directory path: ");
                    String dirName = userScan.nextLine();
                    System.out.println(client.createDirectory(dirName));
                } else if (userChoice.equals("write")) {
                    System.out.print("Enter filepath: ");
                    String fileName = userScan.nextLine();
                    System.out.print("Start writing: ");
                    String data = userScan.nextLine();
                    System.out.println(client.write(fileName, data));
                } else if (userChoice.equals("read")) {
                    System.out.print("Enter filepath: ");
                    String fileName = userScan.nextLine();
                    System.out.println(client.read(fileName));
                } else if (userChoice.equals("groupRead")) {
                    System.out.print("Enter groupname: ");
                    String groupName = userScan.nextLine();
                    System.out.print("Enter username: ");
                    String userName = userScan.nextLine();
                    System.out.print("Enter filepath: ");
                    String fileName = userScan.nextLine();
                    System.out.println(client.read(fileName, groupName, userName));
                } else if (userChoice.equals("addFileToGroup")) {
                    System.out.print("Enter groupname: ");
                    String groupName = userScan.nextLine();
                    System.out.print("Enter filepath: ");
                    String fileName = userScan.nextLine();
                    System.out.println(client.addFileToGroup(fileName, groupName));
                } else if (userChoice.equals("addDirectoryToGroup")) {
                    System.out.print("Enter groupname: ");
                    String groupName = userScan.nextLine();
                    System.out.print("Enter directory path: ");
                    String fileName = userScan.nextLine();
                    System.out.println(client.addDirectoryToGroup(fileName, groupName));
                } else if (userChoice.equals("addUserToGroup")) {
                    System.out.print("Enter user to add: ");
                    String userToAdd = userScan.nextLine();
                    System.out.print("Enter groupname: ");
                    String groupName = userScan.nextLine();
                    System.out.println(client.addUserToGroup(userToAdd, groupName));
                } else if (userChoice.equals("removeUserFromGroup")) {
                    System.out.print("Enter user to remove: ");
                    String userToRemove = userScan.nextLine();
                    System.out.print("Enter groupname: ");
                    String groupName = userScan.nextLine();
                    System.out.println(client.removeUserFromGroup(userToRemove, groupName));
                } else if (userChoice.equals("restore")) {
                    System.out.print("Enter filepath: ");
                    String fileName = userScan.nextLine();
                    System.out.println(client.restore(fileName));
                } else if (userChoice.equals("delete")) {
                    System.out.print("Enter filepath: ");
                    String fileName = userScan.nextLine();
                    System.out.println(client.delete(fileName));
                } else if (!userChoice.equals("exit")) {
                    System.out.println("Sorry, please enter valid command.");
                }
            } while (!userChoice.equals("exit"));

            userScan.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void help() {
        System.out.println(
                "            General  >>>  $ [help, exit]");
        System.out.println(
                "              Files  >>>  $ [createDirectory, create, write, read, groupRead, delete, restore]");
        System.out.println(
                "  Group Maintenance  >>>  $ [addUserToGroup, removeUserFromGroup, addFileToGroup, removeFileFromGroup");
        System.out.println(
                "                             addDirectoryToGroup, removeDirectoryFromGroup]");
    }
}
