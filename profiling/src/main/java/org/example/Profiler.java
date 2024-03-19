package org.example;

/*
这个类是用于做采样器的
 */


import java.time.Duration;

public class Profiler implements Runnable {
    private volatile boolean stop = false;
    private final Options options;
    private final Store store;


    public Profiler(Options options) {
        this.options = options;
        this.store = new Store(options.getFlamePath());
        Runtime.getRuntime().addShutdownHook(new Thread(this::onEnd)); //导致jvm在关闭的时候会调用Profiler::onEnd
    }

    public void sample() {
        Thread.getAllStackTraces().forEach(((thread, stackTraceElements) -> {
            if (!thread.isDaemon()) {
                store.addSample(stackTraceElements);
            }
        }));
    }


    @Override
    public void run() {
        //保持每10秒扫描一次
        while (true) {
            Duration start = Duration.ofNanos(System.nanoTime());
            sample();
            Duration duration = Duration.ofNanos(System.nanoTime()).minus(start);
            Duration sleep = options.getInterval().minus(duration);
            sleep(sleep);
        }
    }

    public void sleep(Duration duration) {

    }

    private void onEnd() {
        stop = true;
        while (stop);
    }
}
