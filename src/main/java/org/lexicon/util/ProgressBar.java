package org.lexicon.util;

public class ProgressBar {

    private int maxProgress = 100;
    private int currentProgress = 0;

    public ProgressBar() {}

    public ProgressBar(int maxProgress) {
        this.maxProgress = maxProgress;
    }

    public void step() {
        step(1);
    }

    public void step(int i) {
        currentProgress += i;
        printProgress();
    }

    private void printProgress() {
        double percent = 100.0 * currentProgress / maxProgress;
        StringBuilder bar = new StringBuilder("[");

        for (int i = 0; i < 50; i++) {
            if (i < (percent / 2)) {
                bar.append("=");
            }
            else if (i == (percent / 2)) {
                bar.append(">");
            }
            else {
                bar.append(" ");
            }
        }

        bar.append(String.format("] %.2f%% ", percent));
        if ((int) percent == 100) {
            System.out.println("\rDone...                                                      ");
        }
        else 
            System.out.print("\r" + bar.toString());
    }
    
    
}
