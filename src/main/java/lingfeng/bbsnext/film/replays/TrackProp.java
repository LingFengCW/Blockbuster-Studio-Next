package lingfeng.bbsnext.film.replays;

import com.google.gson.JsonObject;

/**
 * Per-track visual properties that BBS's {@code Replay} does not model natively.
 *
 * <p>These live in the lingfeng layer (a side-table keyed by the stable replay
 * id) so that the original {@code mchorse.bbs_mod} {@code Replay} class is never
 * modified. Mirrors the professional NLE model: blend mode, opacity and an
 * After-Effects-style track matte (a source track + luma/alpha mode).</p>
 */
public class TrackProp
{
    public static final String BLEND_NORMAL = "normal";

    public static final String MATTE_ALPHA = "alpha";
    public static final String MATTE_ALPHA_INV = "alphaInv";
    public static final String MATTE_LUMA = "luma";
    public static final String MATTE_LUMA_INV = "lumaInv";

    /* F4.3: blend mode whitelist. Anything outside this set is rejected by
     * apply()/isSupportedBlendMode() so an unknown value degrades to normal. */
    private static final java.util.Set<String> SUPPORTED_BLEND_MODES = new java.util.HashSet<>(java.util.Arrays.asList(
        BLEND_NORMAL, "add", "subtract", "multiply", "screen", "overlay", "darken", "lighten", "difference", "exclusion"
    ));

    public static boolean isSupportedBlendMode(String mode)
    {
        return mode != null && SUPPORTED_BLEND_MODES.contains(mode);
    }

    public String blendMode = BLEND_NORMAL;
    public float opacity = 1.0F;
    /** Stable replay id of the matte source track, or "" for none. */
    public String matteSource = "";
    public String matteMode = MATTE_ALPHA;

    public JsonObject toJson()
    {
        JsonObject o = new JsonObject();

        o.addProperty("blendMode", blendMode);
        o.addProperty("opacity", opacity);
        o.addProperty("matteSource", matteSource);
        o.addProperty("matteMode", matteMode);

        return o;
    }

    public static TrackProp fromJson(JsonObject o)
    {
        TrackProp p = new TrackProp();

        if (o == null)
        {
            return p;
        }

        if (o.has("blendMode")) p.blendMode = o.get("blendMode").getAsString();
        if (o.has("opacity")) p.opacity = o.get("opacity").getAsFloat();
        if (o.has("matteSource")) p.matteSource = o.get("matteSource").getAsString();
        if (o.has("matteMode")) p.matteMode = o.get("matteMode").getAsString();

        return p;
    }

    public void apply(String prop, String value)
    {
        if (prop == null || value == null)
        {
            return;
        }

        switch (prop)
        {
            case "blendMode":
                /* F4.3: ignore unsupported blend modes, keep previous (defaults to normal). */
                if (isSupportedBlendMode(value))
                {
                    blendMode = value;
                }
                break;
            case "opacity":
                try
                {
                    opacity = Math.max(0F, Math.min(1F, Float.parseFloat(value)));
                }
                catch (NumberFormatException ignored)
                {
                }
                break;
            case "matteSource":
                matteSource = value;
                break;
            case "matteMode":
                matteMode = value;
                break;
            default:
                break;
        }
    }
}
