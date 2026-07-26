package mchorse.bbs_mod.cubic.model;

import java.util.LinkedList;
import java.util.Queue;
import mchorse.bbs_mod.BBSModClient;

public class ModelBakery implements Runnable
{
    private ModelManager manager;
    private Thread thread;
    private Queue<String> queue = new LinkedList<>();

    public ModelBakery(ModelManager manager)
    {
        this.manager = manager;
    }

    public void add(String key)
    {
        this.queue.offer(key);

        if (this.thread == null)
        {
            this.thread = new Thread(this, "BBS model loader");
            this.thread.start();
        }
    }

    @Override
    public void run()
    {
        while (!this.queue.isEmpty())
        {
            String model = this.queue.poll();

            try
            {
                this.manager.loadModel(model);
            }
            catch (Exception e)
            {
                BBSModClient.LOGGER.error("Exception", e);
            }
        }

        this.thread = null;
    }
}


