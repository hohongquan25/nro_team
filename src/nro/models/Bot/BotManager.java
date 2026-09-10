package nro.models.Bot;

import java.util.ArrayList;
import java.util.List;
import nro.models.server.ServerManager;

public class BotManager implements Runnable {

    public static BotManager i;

    public List<Bot> bot = new ArrayList<>();

    public static BotManager gI() {
        if (i == null) {
            i = new BotManager();
        }
        return i;
    }

    @Override
    public void run() {
        while (ServerManager.isRunning) {
            long st = System.currentTimeMillis();
            try {
                for (Bot bot : new ArrayList<>(this.bot)) {
                    if (bot != null) {
                        bot.update();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                long timeLeft = 150 - (System.currentTimeMillis() - st);
                try {
                    Thread.sleep(Math.max(timeLeft, 50));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

}
