import java.io.*;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class P2PMasterImpl extends UnicastRemoteObject implements P2PMaster {
    public HashMap<String, P2PFile> filesystem;
    public Set<User> allUsers; // username -> userPublicKey
    public Set<Group> groups;
    public HashSet<User> connectedServers;
    public int replication = 3;
    private final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    protected P2PMasterImpl() throws IOException {
        super();
        allUsers = new HashSet<>();
        groups = new HashSet<>();
        filesystem = new HashMap<>();
        connectedServers = new HashSet<>();

        // Set up config file for storing authorized users
        File allUsersDB = new File("configurations/allUsers");
        allUsersDB.getParentFile().mkdirs();
        if (allUsersDB.createNewFile())
            System.out.println("Created configuration file " + allUsersDB.getPath());
        if (allUsersDB.length() != 0) {
            try {
                FileInputStream fis = new FileInputStream(allUsersDB.getPath());
                ObjectInputStream ois = new ObjectInputStream(fis);
                allUsers = (HashSet<User>) ois.readObject();
                fis.close();
                ois.close();
            } catch (ClassNotFoundException e) {
                System.out.println("Class not found.");
            } catch (Exception e) {
                System.out.println("An error occurred.");
            }
        }
        System.out.println("Loaded configuration file " + allUsersDB.getPath());

        // Set up config file for storing groups
        File groupsDB = new File("configurations/groups");
        groupsDB.getParentFile().mkdirs();
        if (groupsDB.createNewFile())
            System.out.println("Created configuration file " + groupsDB.getPath());
        if (groupsDB.length() != 0) {
            try {
                FileInputStream fis = new FileInputStream(groupsDB.getPath());
                ObjectInputStream ois = new ObjectInputStream(fis);
                groups = (HashSet<Group>) ois.readObject();
                fis.close();
                ois.close();
            } catch (ClassNotFoundException e) {
                System.out.println("Class not found.");
            } catch (Exception e) {
                System.out.println("An error occurred.");
            }
        }
        System.out.println("Loaded configuration file " + groupsDB.getPath());

        // Set up config file for filesystem
        File filesystemDB = new File("configurations/filesystem");
        filesystemDB.getParentFile().mkdirs();
        if (filesystemDB.createNewFile())
            System.out.println("Created configuration file " + filesystemDB.getPath());
        if (filesystemDB.length() != 0) {
            try {
                FileInputStream fis = new FileInputStream(filesystemDB.getPath());
                ObjectInputStream ois = new ObjectInputStream(fis);
                filesystem = (HashMap<String, P2PFile>) ois.readObject();
                fis.close();
                ois.close();
            } catch (ClassNotFoundException e) {
                System.out.println("Class not found.");
            } catch (Exception e) {
                System.out.println("An error occurred.");
            }
        }
        System.out.println("Loaded configuration file " + filesystemDB.getPath());
    }

    @Override
    public List<User> getPeerInfo(String filePath) throws RemoteException {
        if (filesystem.containsKey(filePath))
            return filesystem.get(filePath).getLocations();
        return null;
    }

    @Override
    public String registerUser(String userName, String userIP, String userPort, PublicKey userPublicKey)
            throws RemoteException {
        String ans = null;
        User newUser = new User(userName, userIP, userPort, userPublicKey, null);
        if (allUsers.contains(newUser)) {
            connectedServers.add(newUser);
            return ans;
        }
        newUser.setEKey(getSecureRandomKey("AES", 256));
        allUsers.add(newUser);
        connectedServers.add(newUser);
        updateAllUsers();
        ans = encryptWithPublicKey(newUser.getEKey(), newUser.getPKey());
        return ans;
    }

    @Override
    public String addUserToGroup(String currentUserName, String userToAddName, String group, String challenge)
            throws RemoteException {
        User currentUser = new User(currentUserName, null, null, null, null);
        User userToAdd = new User(userToAddName, null, null, null, null);
        if (allUsers.contains(currentUser) && allUsers.contains(userToAdd) && !currentUserName.equals(userToAddName)) {
            User userInCharge = null;
            for (User u : allUsers) {
                if (u.equals(currentUser)) {
                    userInCharge = u;
                }
            }
            if (userInCharge != null) {
                if (checkSignature((currentUserName + userToAddName + group), challenge, userInCharge.getPKey())) {
                    for (User userInProgress : allUsers) {
                        if (userInProgress.equals(userToAdd)) {
                            Group groupTmp = new Group(group, currentUserName);
                            if (!groups.contains(groupTmp)) {
                                groups.add(groupTmp);
                                userInProgress.addGroup(group);
                                updateAllUsers();
                                updateGroups();
                                return userToAddName + " was successfully added to " + group;
                            } else {
                                for (Group existingGroup : groups) {
                                    if (existingGroup.equals(groupTmp)
                                            && existingGroup.getOwner().equals(currentUserName)) {
                                        groups.add(groupTmp);
                                        userInProgress.addGroup(group);
                                        updateAllUsers();
                                        updateGroups();
                                        return userToAddName + " was successfully added to " + group;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return userToAddName + " could not be added to " + group;
    }

    @Override
    public String removeUserFromGroup(String currentUserName, String userToRemoveName, String group, String challenge)
            throws RemoteException {
        User currentUser = new User(currentUserName, null, null, null, null);
        User userToRemove = new User(userToRemoveName, null, null, null, null);
        if (allUsers.contains(currentUser) && allUsers.contains(userToRemove)
                && !currentUserName.equals(userToRemoveName)) {
            User userInCharge = null;
            for (User u : allUsers) {
                if (u.equals(currentUser)) {
                    userInCharge = u;
                }
            }
            if (userInCharge != null) {
                if (checkSignature((currentUserName + userToRemoveName + group), challenge,
                        userInCharge.getPKey())) {
                    for (User userInProgress : allUsers) {
                        if (userInProgress.equals(userToRemove)) {
                            Group groupTmp = new Group(group, null);
                            if (groups.contains(groupTmp)) {
                                for (Group existingGroup : groups) {
                                    if (existingGroup.equals(groupTmp)
                                            && existingGroup.getOwner().equals(currentUserName)) {
                                        userInProgress.removeGroup(group);
                                        updateAllUsers();
                                        updateGroups();

                                        for (String fileName : filesystem.keySet()) {
                                            if (filesystem.get(fileName).getOwner().equals(userToRemoveName)) {
                                                filesystem.get(fileName).removeGroup(group);
                                            }
                                        }
                                        updateFilesystem();

                                        return userToRemoveName + " was successfully removed from " + group;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return userToRemoveName + " could not be removed from " + group;
    }

    @Override
    public User getRandomPeer() throws RemoteException {
        User[] userArray = connectedServers.toArray(new User[connectedServers.size()]);
        // generate a random number
        Random random = new Random();
        // this will generate a random number between 0 and
        // HashSet.size - 1
        int randomNumber = random.nextInt(connectedServers.size());
        User selectedUser = userArray[randomNumber];
        User strippedUser = new User(selectedUser.getName(), selectedUser.getIp(), selectedUser.getPort(),
                selectedUser.getPKey(), null);
        return strippedUser;
    }

    @Override
    public List<User> getConnectedServers() throws RemoteException {
        List<User> strippedUsers = new ArrayList<>();
        int i = 0;
        for (User user : connectedServers) {
            if (i == this.replication)
                break;
            i++;
            strippedUsers.add(new User(user.getName(), user.getIp(), user.getPort(),
                    user.getPKey(), null));
        }
        return strippedUsers;
    }

    @Override
    public void updateHashTable(String filePath, User user, String owner, String type) {
        List<User> users;
        if (filesystem.containsKey(filePath)) {
            users = filesystem.get(filePath).getLocations();
            users.add(user);
        } else {
            users = new ArrayList<>();
            users.add(user);
        }
        filesystem.put(filePath, new P2PFile(filePath, owner, users, type));
        updateFilesystem();
    }

    @Override
    public void updateHashTable(String filePath, List<User> users, String owner, String type) {
        List<User> currentUsers;
        if (filesystem.containsKey(filePath)) {
            currentUsers = filesystem.get(filePath).getLocations();
            users.addAll(currentUsers);
        } else {
            currentUsers = new ArrayList<>();
            users.addAll(currentUsers);
        }
        filesystem.put(filePath, new P2PFile(filePath, owner, users, type));
        updateFilesystem();
    }

    private void updateAllUsers() {
        try {
            FileOutputStream myFileOutStream = new FileOutputStream("configurations/allUsers");
            ObjectOutputStream myObjectOutStream = new ObjectOutputStream(myFileOutStream);
            myObjectOutStream.writeObject(allUsers);
            myObjectOutStream.close();
            myFileOutStream.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private void updateGroups() {
        try {
            FileOutputStream myFileOutStream = new FileOutputStream("configurations/groups");
            ObjectOutputStream myObjectOutStream = new ObjectOutputStream(myFileOutStream);
            myObjectOutStream.writeObject(groups);
            myObjectOutStream.close();
            myFileOutStream.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private void updateFilesystem() {
        try {
            FileOutputStream myFileOutStream = new FileOutputStream("configurations/filesystem");
            ObjectOutputStream myObjectOutStream = new ObjectOutputStream(myFileOutStream);
            myObjectOutStream.writeObject(filesystem);
            myObjectOutStream.close();
            myFileOutStream.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static String getSecureRandomKey(String cipher, int keySize) {
        byte[] secureRandomKeyBytes = new byte[keySize / 8];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(secureRandomKeyBytes);
        return new String(Base64.getUrlEncoder().encode(new SecretKeySpec(secureRandomKeyBytes, cipher).getEncoded()));
    }

    private static String encryptWithPublicKey(String plain, PublicKey pkey) {
        try {
            Cipher encryptCipher = Cipher.getInstance("RSA");
            encryptCipher.init(Cipher.ENCRYPT_MODE, pkey);
            byte[] secretMessageBytes = plain.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);
            return Base64.getUrlEncoder().encodeToString(encryptedMessageBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static boolean checkSignature(String message, String signature, PublicKey pkey) {
        try {
            Signature sig = Signature.getInstance("SHA1WithRSA");
            sig.initVerify(pkey);
            sig.update(message.getBytes("UTF8"));
            return sig.verify(Base64.getUrlDecoder().decode(signature));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static SecretKeySpec convertKey(final String myKey) {
        MessageDigest sha = null;
        byte[] key;
        try {
            key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            return new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    // File encryption
    public static String encryption(final String strToEncode, final String key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // (or) AES/GCM/NoPadding
            cipher.init(Cipher.ENCRYPT_MODE, convertKey(key));
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
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, convertKey(key));
            return new String(cipher.doFinal(Base64.getUrlDecoder().decode(strToDecode)));
        } catch (Exception e) {
            System.out.println("Something went wrong in decryption : " + e.toString());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String readOthersFile(String encryptedFilePath, String userName, String groupName, String otherUser,
            String signature)
            throws IOException, RemoteException {
        for (Group targetGroup : groups) {
            if (targetGroup.getName().equals(groupName)) {
                for (User user : allUsers) {
                    if (!user.getName().equals(userName))
                        continue;

                    if (!checkSignature(encryptedFilePath + userName + groupName + otherUser, signature,
                            user.getPKey()))
                        continue;

                    if (!(user.getGroups().contains(groupName) || targetGroup.getOwner().equals(userName)))
                        continue;

                    for (User ownerUser : allUsers) {
                        if (!ownerUser.getName().equals(otherUser))
                            continue;

                        if (!ownerUser.getGroups().contains(groupName))
                            continue;

                        String ownerFilePath = "";
                        for (String part : encryptedFilePath.split("/")) {
                            if (part.equals(""))
                                continue;
                            ownerFilePath += "/" + encryption(decryption(part, user.getEKey()), ownerUser.getEKey());
                        }

                        if (!filesystem.containsKey(ownerFilePath))
                            continue;

                        if (!filesystem.get(ownerFilePath).getGroups().contains(groupName))
                            continue;

                        try {
                            List<User> users = getPeerInfo(ownerFilePath);
                            User userPeer = users.get(0);
                            RMIFileSystem peer = (RMIFileSystem) Naming
                                    .lookup("rmi://" + userPeer.ip + ":" + userPeer.port + "/master");
                            String fileData = peer.readFile(ownerFilePath);
                            if (fileData == null) {
                                return "Error: this is an empty file.";
                            }
                            return decryption(fileData, ownerUser.getEKey());
                        } catch (Exception e) {
                            return "Error: your permissions are not met.";
                        }
                    }
                }
            }
        }
        return "Unable to read file.";
    }

    @Override
    public String addFileToGroup(String encryptedFilePath, String userName, String groupName, String signature)
            throws RemoteException {
        Group groupTmp = new Group(groupName, userName);
        if (!groups.contains(groupTmp)) {
            for (User user : allUsers) {
                if (!user.getName().equals(userName))
                    continue;
                groups.add(groupTmp);
                user.addGroup(groupName);
                updateAllUsers();
                updateGroups();
            }
        }
        for (Group targetGroup : groups) {
            if (targetGroup.getName().equals(groupName)) {
                for (User user : allUsers) {
                    if (!user.getName().equals(userName))
                        continue;

                    if (!checkSignature(encryptedFilePath + userName + groupName, signature, user.getPKey()))
                        continue;

                    if (!(user.getGroups().contains(groupName) || targetGroup.getOwner().equals(userName)))
                        continue;

                    if (!filesystem.containsKey(encryptedFilePath))
                        continue;

                    if (!filesystem.get(encryptedFilePath).getType().equals("File"))
                        continue;

                    filesystem.get(encryptedFilePath).addGroup(groupName);
                    updateFilesystem();
                    return "Your file was successfully added to " + groupName;
                }
            }
        }

        return "Unable to add your file to " + groupName;
    }

    @Override
    public String removeFileFromGroup(String encryptedFilePath, String userName, String groupName, String signature)
            throws RemoteException {
        for (Group targetGroup : groups) {
            if (targetGroup.getName().equals(groupName)) {
                for (User user : allUsers) {
                    if (!user.getName().equals(userName))
                        continue;

                    if (!checkSignature(encryptedFilePath + userName + groupName, signature, user.getPKey()))
                        continue;

                    if (!(user.getGroups().contains(groupName) || targetGroup.getOwner().equals(userName)))
                        continue;

                    if (!filesystem.containsKey(encryptedFilePath))
                        continue;

                    if (!filesystem.get(encryptedFilePath).getType().equals("File"))
                        continue;

                    filesystem.get(encryptedFilePath).removeGroup(groupName);
                    updateFilesystem();
                    return "Your file was successfully removed from " + groupName;
                }
            }
        }

        return "Unable to remove your file from " + groupName;
    }

    @Override
    public String addDirectoryToGroup(String encryptedFilePath, String userName, String groupName, String signature)
            throws RemoteException {
        Group groupTmp = new Group(groupName, userName);
        if (!groups.contains(groupTmp)) {
            for (User user : allUsers) {
                if (!user.getName().equals(userName))
                    continue;
                groups.add(groupTmp);
                user.addGroup(groupName);
                updateAllUsers();
                updateGroups();
            }
        }
        for (Group targetGroup : groups) {
            if (targetGroup.getName().equals(groupName)) {
                for (User user : allUsers) {
                    if (!user.getName().equals(userName))
                        continue;

                    if (!checkSignature(encryptedFilePath + userName + groupName, signature, user.getPKey()))
                        continue;

                    if (!(user.getGroups().contains(groupName) || targetGroup.getOwner().equals(userName)))
                        continue;

                    for (String fileName : filesystem.keySet()) {
                        if (fileName.startsWith(encryptedFilePath)) {
                            filesystem.get(fileName).addGroup(groupName);
                        }
                    }
                    updateFilesystem();
                    return "Your directory was successfully added to " + groupName;
                }
            }
        }

        return "Unable to add your directory to " + groupName;
    }

    @Override
    public String removeDirectoryFromGroup(String encryptedFilePath, String userName, String groupName,
            String signature)
            throws RemoteException {
        for (Group targetGroup : groups) {
            if (targetGroup.getName().equals(groupName)) {
                for (User user : allUsers) {
                    if (!user.getName().equals(userName))
                        continue;

                    if (!checkSignature(encryptedFilePath + userName + groupName, signature, user.getPKey()))
                        continue;

                    if (!(user.getGroups().contains(groupName) || targetGroup.getOwner().equals(userName)))
                        continue;

                    for (String fileName : filesystem.keySet()) {
                        if (fileName.startsWith(encryptedFilePath)) {
                            filesystem.get(fileName).removeGroup(groupName);
                        }
                    }
                    updateFilesystem();
                    return "Your directory was successfully removed from " + groupName;
                }
            }
        }

        return "Unable to removed your directory from " + groupName;
    }


    @Override
    public void maliciousCheck() throws IOException {
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    for(String fileName : filesystem.keySet()){
                        P2PFile file = filesystem.get(fileName);
                        List<User> users = file.getLocations();
                        for(User user : users){
                            RMIFileSystem peerServer =
                                    (RMIFileSystem) Naming.lookup("rmi://"+user.getIp()+":"+user.getPort()+"/master");
                            String fileData = peerServer.readFile(fileName);
                            if(fileData==null){
                                System.out.println("Malicious activity detected");
                                System.out.println("Terminating");
                                System.exit(1);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
}
