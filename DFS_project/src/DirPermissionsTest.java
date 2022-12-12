public class DirPermissionsTest {
    public static void main(String[] args) {
        MasterServer master = new MasterServer();
        master.start();

        P2PServer server1 = new P2PServer("ray", "localhost", "5000", "localhost", "1234");
        P2PServer server2 = new P2PServer("bob", "localhost", "5001", "localhost", "1234");
        server1.start();
        server2.start();

        P2PClient ray = new P2PClient("ray", "localhost", "8000", "localhost", "1234");
        P2PClient bob = new P2PClient("bob", "localhost", "8001", "localhost", "1234");

        // Set up dir1/a, dir1/b, and dir1/c
        System.out.println("\n[+] Ray creates dir1/");
        System.out.println(ray.createDirectory("dir1"));
        System.out.println("\n[+] Ray creates & writes to dir1/a");
        System.out.println(ray.create("dir1/a"));
        System.out.println(ray.write("dir1/a", "FILE A"));
        System.out.println("\n[+] Ray creates & writes to dir1/b");
        System.out.println(ray.create("dir1/b"));
        System.out.println(ray.write("dir1/b", "FILE B"));
        System.out.println("\n[+] Ray creates & writes to dir1/c");
        System.out.println(ray.create("dir1/c"));
        System.out.println(ray.write("dir1/c", "FILE C"));

        // Bob fails to read any files in dir1/
        System.out.println("\n[+] Bob tries to read a, b, and c in Ray's dir1/ (UNAUTHORIZED)");
        System.out.println(bob.read("dir1/a", null, "ray"));
        System.out.println(bob.read("dir1/b", null, "ray"));
        System.out.println(bob.read("dir1/c", null, "ray"));

        // Ray grants Bob permission to file1.txt via group1
        System.out.println("\n[+] Ray adds Bob to group1");
        System.out.println(ray.addUserToGroup("bob", "group1"));
        System.out.println("\n[+] Ray adds dir1/ to group1");
        System.out.println(ray.addDirectoryToGroup("dir1", "group1"));

        // Bob fails to read all files in dir1/
        System.out.println("\n[+] Bob tries to read a, b, and c in Ray's dir1/ (AUTHORIZED)");
        System.out.println(bob.read("dir1/a", "group1", "ray"));
        System.out.println(bob.read("dir1/b", "group1", "ray"));
        System.out.println(bob.read("dir1/c", "group1", "ray"));

        // Ray updated file1.txt
        System.out.println("\n[+] Ray deletes dir1/c");
        System.out.println(ray.delete("dir1/c"));

        // Bob successfully reads the updated file1.txt
        System.out.println("\n[+] Bob tries to read a, b, and c in Ray's dir1/ (AUTHORIZED)");
        System.out.println(bob.read("dir1/a", "group1", "ray"));
        System.out.println(bob.read("dir1/b", "group1", "ray"));
        System.out.println(bob.read("dir1/c", "group1", "ray"));

        // Ray revokes Bob's permission to group1 files
        System.out.println("\n[+] Ray removes dir1/ from group1");
        System.out.println(ray.removeDirectoryFromGroup("dir1", "group1"));

        // Bob fails to read file1.txt
        System.out.println("\n[+] Bob tries to read a, b, and c in Ray's dir1/ (UNAUTHORIZED)");
        System.out.println(bob.read("dir1/a", "group1", "ray"));
        System.out.println(bob.read("dir1/b", "group1", "ray"));
        System.out.println(bob.read("dir1/c", "group1", "ray"));
    }
}
