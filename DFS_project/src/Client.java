import java.nio.charset.StandardCharsets;
import java.rmi.*;
import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Client {

    // Security variables
    private static SecretKeySpec secretKey;
    private static byte[] key;

    // Key setter
    public static void setKey(final String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // File encryption
    public static String encryption(final String strToEncode, final String key) {
        try {
            setKey(key);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // AES/GCM/NoPadding AES/ECB/PKCS5Padding
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder()
                    .encodeToString(cipher.doFinal(strToEncode.getBytes("UTF-8")));
        } catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    // File decryption
    public static String decryption(final String strToDecode, final String key) {
        try {
            setKey(key);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecode)));
        } catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }


    public static void main(String args[]) {
        String IP = "127.0.0.1";
        String port = "1234";
        String serverAnswer;
        String userChoice;
        String[] userLine;

        try {
            // lookup method to find reference of remote object
            FileSystem access = (FileSystem) Naming.lookup("rmi://" + IP + ":" + port + "/usingRMI");

            Scanner userScan = new Scanner(System.in);
            help();

            do {
                System.out.print("$ ");
                userLine = userScan.nextLine().split(" ");
                userChoice = userLine[0];

                if (userChoice.equals("help")) {
                    help();
                } else if (userChoice.equals("create") && userLine.length > 1) {
                    serverAnswer = access.createFile(userLine[1]);
                    System.out.println(serverAnswer);
                } else if (userChoice.equals("write") && userLine.length > 1) {
                    System.out.println("Start Writing....");
                    String ans = userScan.nextLine();
                    serverAnswer = access.writeFile(userLine[1], ans);
                    System.out.println(serverAnswer);
                } else if (userChoice.equals("read") && userLine.length > 1) {
                    serverAnswer = access.readFile(userLine[1]);
                    System.out.println(serverAnswer);
                } else if (userChoice.equals("delete") && userLine.length > 1) {
                    serverAnswer = access.deleteFile(userLine[1]);
                    System.out.println(serverAnswer);
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
        System.out.println("Write a command in the form of:");
        System.out.println("  0-args >>> $ [help, exit]");
        System.out.println("  1-arg  >>> $ [create, write, read, delete] filename");
    }
}
