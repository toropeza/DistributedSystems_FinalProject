import java.util.LinkedList;

/**
 * WorkQueue maintains a thread pool for executing Runnables
 * */
public class WorkQueue {

    //The currently running threads
    private final PoolWorker[] workers;

    //Queue of Runnables
    private final LinkedList<Runnable> queue;

    private volatile boolean shutdown;

    //Default amount of Threads in thread pool
    public static final int DEFAULT = 10;

    public WorkQueue() {
        this(DEFAULT);
    }

    /**
     * @param threads the amount of threads to run in the WorkQueue
     */
    public WorkQueue(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("Only " + threads +
                    " worker threads specified. Must have at least" +
                    " one worker thread.");
        }

        this.queue = new LinkedList<Runnable>();
        this.workers = new PoolWorker[threads];

        shutdown = false;

        for (int i = 0; i < threads; i++) {
            workers[i] = new PoolWorker();
            workers[i].start();
        }
    }

    /**
     * Execute a runnable task to the pool of threads
     *
     * @param r the runnable task
     */
    public void execute(Runnable r) {
        synchronized (queue) {
            queue.addLast(r);
            queue.notifyAll();
        }
    }

    /**
     * Shut down the pool of threads
     */
    public void shutdown() {
        shutdown = true;

        synchronized (queue) {
            queue.notifyAll();
        }
    }

    /**
     * This is a working class to help the pool of threads run and stay alive
     */
    private class PoolWorker extends Thread {

        @Override
        public void run() {
            Runnable r = null;

            while (true) {
                synchronized (queue) {
                    while (queue.isEmpty() && !shutdown) {
                        try {
                            queue.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }

                    if (shutdown) {
                        break;
                    } else {
                        r = queue.removeFirst();
                    }
                }

                try {
                    r.run();
                } catch (RuntimeException ex) {
                    System.out.println("Warning: Encountered an unexpected exception while running.");
                }
            }
        }
    }
}