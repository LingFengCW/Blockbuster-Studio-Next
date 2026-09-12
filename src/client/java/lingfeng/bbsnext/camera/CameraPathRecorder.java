package lingfeng.bbsnext.camera;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.overwrite.PathClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.MathUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

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
        if (target != null)
        {
            /* Clean up the raw player-walk capture: even-time resample + a light
             * Chaikin smoothing pass. Raw walking capture is jittery (uneven speed,
             * micro-stutter); pro camera tools always smooth captured motion, so do
             * the same so the recorded path plays back clean. */
            smoothRecordedPath(target);
        }

        target = null;
        panel = null;
        tick = 0;
    }

    /** Resample + smooth a recorded path in place. See stop(). */
    private static void smoothRecordedPath(PathClip clip)
    {
        int n = clip.size();

        if (n < 3)
        {
            return;
        }

        List<Position> src = new ArrayList<>(n);

        for (int i = 0; i < n; i++)
        {
            src.add(clip.getPoint(i).copy());
        }

        /* Even-time resample first (the dedup in the sampler can leave gaps),
         * then one Chaikin pass. Keep a reasonable point count for Hermite playback. */
        List<Position> resampled = resampleEven(src, Math.max(n, 12));
        List<Position> smoothed = chaikin(resampled, 1);

        clip.points.set(smoothed);
    }

    private static List<Position> resampleEven(List<Position> src, int count)
    {
        count = Math.max(count, src.size());

        List<Position> out = new ArrayList<>(count);
        int n = src.size();

        for (int i = 0; i < count; i++)
        {
            float t = (float) i / (count - 1) * (n - 1);
            int i0 = (int) Math.floor(t);
            int i1 = Math.min(i0 + 1, n - 1);
            float f = t - i0;

            out.add(lerpPos(src.get(i0), src.get(i1), f));
        }

        return out;
    }

    private static List<Position> chaikin(List<Position> src, int passes)
    {
        List<Position> cur = src;

        for (int p = 0; p < passes; p++)
        {
            if (cur.size() < 3)
            {
                break;
            }

            List<Position> next = new ArrayList<>();

            next.add(cur.get(0));

            for (int i = 0; i < cur.size() - 1; i++)
            {
                Position p0 = cur.get(i);
                Position p1 = cur.get(i + 1);
                Position q = new Position();
                Position r = new Position();

                q.copy(p0);
                q.interpolate(p1, 0.25F);
                r.copy(p1);
                r.interpolate(p0, 0.25F);

                next.add(q);
                next.add(r);
            }

            next.add(cur.get(cur.size() - 1));
            cur = next;
        }

        return cur;
    }

    private static Position lerpPos(Position a, Position b, float f)
    {
        Position m = new Position();

        m.point.x = a.point.x + (b.point.x - a.point.x) * f;
        m.point.y = a.point.y + (b.point.y - a.point.y) * f;
        m.point.z = a.point.z + (b.point.z - a.point.z) * f;
        m.angle.yaw = a.angle.yaw + (b.angle.yaw - a.angle.yaw) * f;
        m.angle.pitch = a.angle.pitch + (b.angle.pitch - a.angle.pitch) * f;
        m.angle.roll = a.angle.roll + (b.angle.roll - a.angle.roll) * f;
        m.angle.fov = a.angle.fov + (b.angle.fov - a.angle.fov) * f;

        return m;
    }
}
