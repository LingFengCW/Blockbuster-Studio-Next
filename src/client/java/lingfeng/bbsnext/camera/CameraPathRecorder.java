package lingfeng.bbsnext.camera;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.MathUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * Records the player's walk into a camera {@link PathClip}.
 *
 * Triggered from the editor's "录制坐标" action on a camera clip. While active,
 * every client tick samples the local player's position/orientation and appends
 * it as a point on the target path. The user toggles recording off (click again)
 * to finalize. This is intentionally decoupled from the character {@code Recorder}
 * so a camera path can be captured independently of any actor.
 */
public class CameraPathRecorder
{
    private static boolean registered = false;

    private static PathClip target;
    private static UIFilmPanel panel;
    private static int tick;

    /** Sample every N ticks (2 → ~10 points/sec at 20 TPS). */
    private static final int SAMPLE_EVERY = 2;

    public static boolean isRecording(PathClip clip)
    {
        return target == clip;
    }

    public static boolean isActive()
    {
        return target != null;
    }

    private static void ensureRegistered()
    {
        if (registered)
        {
            return;
        }

        registered = true;

        ClientTickEvents.END_CLIENT_TICK.register(mc ->
        {
            if (target == null)
            {
                return;
            }

            Minecraft client = Minecraft.getInstance();

            if (client.player == null || client.level == null || panel == null)
            {
                return;
            }

            try
            {
                int duration = target.duration.get();

                if (tick >= duration)
                {
                    stop();

                    return;
                }

                if (tick % SAMPLE_EVERY == 0)
                {
                    Camera camera = new Camera();

                    camera.set(client.player, MathUtils.toRad(client.options.fov().get()));

                    Position position = new Position(camera);

                    if (target.size() == 0)
                    {
                        target.points.add(position);
                    }
                    else
                    {
                        Position last = target.getPoint(target.size() - 1);
                        double dx = position.point.x - last.point.x;
                        double dy = position.point.y - last.point.y;
                        double dz = position.point.z - last.point.z;

                        /* Skip near-duplicate samples to keep the path clean. */
                        if (dx * dx + dy * dy + dz * dz > 0.01)
                        {
                            target.points.add(position);
                        }
                    }
                }

                tick += 1;
            }
            catch (Throwable t)
            {
                /* Never let a sampling failure break the client tick loop. */
                stop();
            }
        });
    }

    /** Start (or toggle off, if already recording this clip). */
    public static void toggle(PathClip clip, UIFilmPanel panel)
    {
        ensureRegistered();

        if (target == clip)
        {
            stop();

            return;
        }

        target = clip;
        CameraPathRecorder.panel = panel;
        tick = 0;

        /* Clear any previous path so the new recording starts fresh. */
        clip.points.reset();
    }

    public static void stop()
    {
        target = null;
        panel = null;
        tick = 0;
    }
}
