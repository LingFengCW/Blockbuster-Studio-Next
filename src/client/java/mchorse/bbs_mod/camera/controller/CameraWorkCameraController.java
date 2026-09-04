package mchorse.bbs_mod.camera.controller;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;

public abstract class CameraWorkCameraController implements ICameraController
{
    protected CameraClipContext context;
    protected Position position = new Position();

    public CameraWorkCameraController()
    {
        this.context = new CameraClipContext();
    }

    public CameraWorkCameraController setWork(Clips clips)
    {
        this.context.clips = clips;

        return this;
    }

    public CameraClipContext getContext()
    {
        return this.context;
    }

    public Position getPosition()
    {
        return this.position;
    }

    /** Set when the current tick has no base camera covering it ("相机丢失"). */
    public boolean cameraLost = false;

    /** Set when the current tick is covered by two or more base cameras (重叠). */
    public boolean cameraOverlap = false;

    protected void apply(Camera camera, int ticks, float transition)
    {
        if (camera != null)
        {
            this.position.set(camera);
        }

        this.context.clipData.clear();
        this.context.setup(ticks, transition);

        this.cameraLost = false;
        this.cameraOverlap = false;

        /* Walk clips in insertion order so "earliest created" = first match. */
        Clip baseCamera = null;
        int baseCount = 0;

        for (Clip clip : this.context.clips.get())
        {
            if (!clip.isInside(ticks) || !clip.enabled.get())
            {
                continue;
            }

            if (isBaseCamera(clip))
            {
                baseCount++;

                if (baseCamera == null)
                {
                    baseCamera = clip;
                }
            }
        }

        if (baseCamera == null)
        {
            /* No base camera covers this tick: keep the last known position. */
            this.cameraLost = true;
        }
        else
        {
            if (baseCount > 1)
            {
                this.cameraOverlap = true;
            }

            this.context.apply(baseCamera, this.position);
        }

        /* Apply modifiers (and misc clips) on top of the chosen base camera. */
        for (Clip clip : this.context.clips.get())
        {
            if (!clip.isInside(ticks) || !clip.enabled.get())
            {
                continue;
            }

            if (!isBaseCamera(clip))
            {
                this.context.apply(clip, this.position);
            }
        }

        AudioClientClip.manageSounds(this.context);

        this.context.currentLayer = 0;

        if (camera != null)
        {
            this.position.apply(camera);
        }
    }

    /** A "base camera" is one that sets an absolute camera position (overwrite package). */
    private static boolean isBaseCamera(Clip clip)
    {
        return clip.getClass().getName().startsWith("mchorse.bbs_mod.camera.clips.overwrite.");
    }

    @Override
    public int getPriority()
    {
        return 10;
    }
}
