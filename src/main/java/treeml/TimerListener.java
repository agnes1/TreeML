package treeml;

public class TimerListener implements Listener {

    private long start;

    @Override
    public void onStart() {
        start = System.nanoTime();
    }

    @Override
    public void onEnd() {
        double time = (System.nanoTime() - start) / 1000000.0;
        System.out.println("Parse completed in " + time + " milliseconds.");
    }
}
