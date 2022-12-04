public class Benchmark {
    public static void main(String args[]) {
        MasterServer master = new MasterServer();
        master.start();

        P2PServer server1 = new P2PServer("ray", "localhost", "5000", "localhost", "1234");
        P2PServer server2 = new P2PServer("bob", "localhost", "5001", "localhost", "1234");
        P2PServer server3 = new P2PServer("bill", "localhost", "5002", "localhost", "1234");
        server1.start();
        server2.start();
        server3.start();

        P2PClient client1 = new P2PClient("ray", "localhost", "8000", "localhost", "1234");
        P2PClient client2 = new P2PClient("bob", "localhost", "8001", "localhost", "1234");
        P2PClient client3 = new P2PClient("bill", "localhost", "8002", "localhost", "1234");

        for (int i = 0; i < 999999; i++)
            client2.create("test" + i);
    }
}
