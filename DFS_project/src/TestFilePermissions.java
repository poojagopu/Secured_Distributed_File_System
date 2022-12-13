public class TestFilePermissions {
    public static void main(String[] args) {
        MasterServer master = new MasterServer();
        master.start();

        P2PServer server1 = new P2PServer("ray", "localhost", "5000", "localhost", "1234");
        P2PServer server2 = new P2PServer("bob", "localhost", "5001", "localhost", "1234");
        server1.start();
        server2.start();

        P2PClient ray = new P2PClient("ray", "localhost", "8000", "localhost", "1234");
        P2PClient bob = new P2PClient("bob", "localhost", "8001", "localhost", "1234");

        try {
            // Set up file1.txt
            System.out.println("\n[+] Ray creates file1.txt");
            System.out.println(ray.create("file1.txt"));
            System.out.println("\n[+] Ray writes to file1.txt");
            System.out.println(ray.write("file1.txt", "Hello, this is (or at least will be) a shared file."));
            System.out.println("\n[+] Ray reads file1.txt");
            System.out.println(ray.read("file1.txt"));

            // Bob fails to read file1.txt
            System.out.println("\n[+] Bob tries to read Ray's file1.txt (UNAUTHORIZED)");
            System.out.println(bob.read("file1.txt", null, "ray"));

            // Ray grants Bob permission to file1.txt via group1
            System.out.println("\n[+] Ray adds file1 to group1");
            System.out.println(ray.addFileToGroup("file1.txt", "group1"));
            System.out.println("\n[+] Ray adds Bob to group1");
            System.out.println(ray.addUserToGroup("bob", "group1"));
            Thread.sleep(1 * 1000);

            // Bob successfully reads file1.txt
            System.out.println("\n[+] Bob tries to read Ray's file1.txt (AUTHORIZED)");
            System.out.println(bob.read("file1.txt", "group1", "ray"));

            // Ray updated file1.txt
            System.out.println("\n[+] Ray updates file1.txt");
            System.out.println(ray.write("file1.txt", "This is an UPDATED shared file!"));

            // Bob successfully reads the updated file1.txt
            System.out.println("\n[+] Bob tries to read Ray's file1.txt (AUTHORIZED)");
            System.out.println(bob.read("file1.txt", "group1", "ray"));

            // Ray revokes Bob's permission to group1 files
            System.out.println("\n[+] Ray removes Bob from group1");
            System.out.println(ray.removeUserFromGroup("bob", "group1"));
            Thread.sleep(1 * 1000);

            // Bob fails to read file1.txt
            System.out.println("\n[+] Bob tries to read Ray's file1.txt (UNAUTHORIZED)");
            System.out.println(bob.read("file1.txt", "group1", "ray"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
