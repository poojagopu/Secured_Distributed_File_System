import java.rmi.*;
import java.util.*;

public class Client {
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
