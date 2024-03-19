package org.example;


public class Main {
    public static void agentmain(String agentArgs) {
        premain(agentArgs);
    }

    public static void premain(String agentArgs) {
        Main main = new Main();
        main.run(new Options());
    }

    private void run(Options options) {
        Thread thread = new Thread(new Profiler(options));
        thread.setDaemon(true);
        thread.setName("Profiler");
        thread.start();
    }
}
