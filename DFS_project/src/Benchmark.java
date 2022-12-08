public class Benchmark {
    public class ClientRunnable implements Runnable {
        public P2PClient client;

        ClientRunnable(P2PClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            System.out.println("Starting 100K work for " + Thread.currentThread().getName() + " (ID: "
                    + Thread.currentThread().getId() + ")");

            for (int i = 0; i < 99999; i++) {
                System.out.println(Thread.currentThread().getName() + " (ID: " + Thread.currentThread().getId() + ")\t"
                        + client.create("file" + i));
                System.out.println(Thread.currentThread().getName() + " (ID: " + Thread.currentThread().getId() + ")\t"
                        + client.write("file" + i, "This is the " + i + "th file."));
                System.out.println(Thread.currentThread().getName() + " (ID: " + Thread.currentThread().getId() + ")\t"
                        + client.read("file" + i));
            }

            System.out.println("Completed 100K work for " + Thread.currentThread().getName() + " (ID: "
                    + Thread.currentThread().getId() + ")");
        }
    }

    public void start() throws InterruptedException {
        MasterServer master = new MasterServer();
        master.start();

        P2PServer server1 = new P2PServer("ray", "localhost", "5000", "localhost", "1234");
        P2PServer server2 = new P2PServer("bob", "localhost", "5001", "localhost", "1234");
        P2PServer server3 = new P2PServer("bill", "localhost", "5002", "localhost", "1234");
        P2PServer server4 = new P2PServer("josh", "localhost", "5003", "localhost", "1234");
        server1.start();
        server2.start();
        server3.start();
        server4.start();

        P2PClient client1 = new P2PClient("ray", "localhost", "8000", "localhost", "1234");
        P2PClient client2 = new P2PClient("bob", "localhost", "8001", "localhost", "1234");
        P2PClient client3 = new P2PClient("bill", "localhost", "8002", "localhost", "1234");
        P2PClient client4 = new P2PClient("josh", "localhost", "8003", "localhost", "1234");

        final long startTime = System.nanoTime();

        Thread t1 = new Thread(new ClientRunnable(client1), "client1");
        Thread t2 = new Thread(new ClientRunnable(client2), "client2");
        Thread t3 = new Thread(new ClientRunnable(client3), "client3");
        Thread t4 = new Thread(new ClientRunnable(client4), "client4");

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        t1.join();
        t2.join();
        t3.join();
        t4.join();

        final long duration = System.nanoTime() - startTime;
        System.out.println((duration) / 1000000000 + "s  " + ((duration) % 1000000000) / 10000000 + "ms");
    }

    public static void main(String args[]) {
        try {
            new Benchmark().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
