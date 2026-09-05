package mchorse.bbs_mod.film;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.utils.clips.Clip;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Map;

public class WorldFilmController extends BaseFilmController
{
    protected CameraClipContext context;
    protected Position position = new Position();

    public int tick;
    public int duration;

    public WorldFilmController(Film film)
    {
        super(film);

        this.createEntities();

        this.duration = film.camera.calculateDuration();
        this.context = new CameraClipContext();
        this.context.clips = film.camera;
    }

    @Override
    public Map<String, Integer> getActors()
    {
        return BBSModClient.getFilms().actors.get(this.film.getId());
    }

    @Override
    public int getTick()
    {
        return this.tick;
    }

    @Override
    public boolean hasFinished()
    {
        return this.tick >= this.duration;
    }

    @Override
    public void update()
    {
        if (!this.paused)
        {
            this.tick += 1;
        }

        super.update();
    }

    @Override
    public void render(LevelRenderContext context)
    {
        super.render(context);

        int tick = Math.max(this.tick, 0);
        List<Clip> clips = this.context.clips.getClips(tick);

        if (clips.isEmpty())
        {
            return;
        }

        this.context.clipData.clear();
        this.context.setup(tick, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));

        for (Clip clip : clips)
        {
            this.context.apply(clip, this.position);
        }

        this.context.currentLayer = 0;

        /* Camera material timeline: while the camera replay plays, apply its
         * skin/model/equipment clips to the first-person actor being filmed. */
        Replay fp = this.film.getFirstPersonReplay();

        if (fp != null)
        {
            Map<String, Integer> actors = this.getActors();
            Integer eid = actors != null ? actors.get(fp.getId()) : null;

            if (eid != null)
            {
                IEntity target = this.entities.get(eid);

                if (target != null)
                {
                    this.film.cameraMaterials.applyTo(target, tick);
                }
            }
        }

        AudioClientClip.manageSounds(this.context);
    }

    @Override
    public void shutdown()
    {
        this.context.shutdown();
    }
}
