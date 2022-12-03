import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class P2PServer {
    public static void main(String args[]) {
        String masterIP = "localhost";
        String masterPort = "1234";

        String userName = "ray";
        String userIP = "localhost";
        String userPort = "5678";

        PublicKey publicKey;
        PrivateKey privateKey;

        try {
            File publicKeyFile = new File("public+" + userName + ".key");
            File privateKeyFile = new File("private+" + userName + ".key");
            if (!publicKeyFile.exists() || !privateKeyFile.exists()) {
                // If user does not already has key pair saved
                System.out.println("Generating RSA key pair");
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair pair = generator.generateKeyPair();
                publicKey = pair.getPublic();
                privateKey = pair.getPrivate();

                try (FileOutputStream fos = new FileOutputStream("public+" + userName + ".key")) {
                    fos.write(publicKey.getEncoded());
                }
                try (FileOutputStream fos = new FileOutputStream("private+" + userName + ".key")) {
                    fos.write(privateKey.getEncoded());
                }
                System.out.println("Generated RSA key pair");
            } else {
                // Otherwise, read the saved key pair
                System.out.println("Reading saved RSA key pair");
                byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
                publicKey = keyFactory.generatePublic(publicKeySpec);

                byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());
                KeyFactory keyFactory2 = KeyFactory.getInstance("RSA");
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
                privateKey = keyFactory2.generatePrivate(privateKeySpec);
                System.out.println("Read saved RSA key pair");
            }

            P2PMaster masterObj = (P2PMaster) Naming.lookup("rmi://" + masterIP + ":" + masterPort + "/master");
            // Create an object of the interface
            // implementation class
            String encryptionKey = masterObj.registerUser(userName, userIP, userPort, publicKey);
            if (encryptionKey != null) {
                // Save in key file
                BufferedWriter writer = new BufferedWriter(new FileWriter("secret+" + userName + ".key"));
                writer.write(decryptWithPrivateKey(encryptionKey, privateKey));
                writer.close();
            } else {
                System.out.println("User already registered, looking for saved encryption key");
                try {
                    File myObj = new File("secret+" + userName + ".key");
                    if (myObj.exists()) {
                        Scanner myReader = new Scanner(myObj);
                        if (myReader.hasNextLine()) {
                            encryptionKey = myReader.nextLine();
                            System.out.println("Located encryption key");
                        } else {
                            System.out.println("Unable to find encryption key");
                        }
                        myReader.close();
                    } else {
                        System.out.println("Unable to find encryption key");
                    }
                } catch (FileNotFoundException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
            }
            RMIFileSystem interface_obj = new RMI_DFS(userName);

            // rmiregistry within the server JVM with
            // port number 1234
            LocateRegistry.createRegistry(Integer.parseInt(userPort));

            // Binds the remote object by the name
            // usingRMI
            Naming.rebind("rmi://" + userIP + ":" + userPort + "/master", interface_obj);
        } catch (Exception ae) {
            System.out.println(ae);
        }
    }

    private static String decryptWithPrivateKey(String crypt, PrivateKey pkey) {
        try {
            Cipher decryptCipher = Cipher.getInstance("RSA");
            decryptCipher.init(Cipher.DECRYPT_MODE, pkey);
            byte[] encryptedMessageBytes = Base64.getUrlDecoder().decode(crypt);
            byte[] decryptedMessageBytes = decryptCipher.doFinal(encryptedMessageBytes);
            String decryptedMessage = new String(decryptedMessageBytes, StandardCharsets.UTF_8);
            return decryptedMessage;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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
            return Base64.getEncoder()
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
        }
        return null;
    }
}
