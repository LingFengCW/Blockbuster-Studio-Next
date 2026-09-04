package lingfeng.bbsnext.update;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent auto-update preferences, stored in
 * {@code <gameDir>/bbs/updates.json}.
 *
 * <ul>
 *   <li>{@code autoUpdate} - proactively download new releases and stage them
 *       (the user still confirms the apply, which waits for MC to close).</li>
 *   <li>{@code updatePush} - show the bottom-left "new version" notification
 *       when a newer release is found.</li>
 * </ul>
 *
 * The source repository is intentionally a single constant so it can be wired
 * to the final release repo later (the version file lives at the repo root).
 */
public class UpdateConfig
{
    /** Repo that hosts the version file + live UI script (pulled over DoH). */
    public static final String REPO = "LingFengCW/Blockbuster-Studio-Next-Material";
    /** Repo that actually publishes the release jars (download URL pattern). */
    public static final String RELEASE_REPO = "LingFengCW/Blockbuster-Studio-Next";
    public static final String VERSION_BRANCH = "main";
    public static final String VERSION_FILE = "version.txt";
    /** Minecraft version baked into the released jar name, e.g.
     *  bbs-next-1.0.0-26.2.jar -> "26.2". */
    public static final String MC_VERSION = "26.2";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static UpdateConfig instance;

    public boolean autoUpdate = false;
    public boolean updatePush = true;
    /** Experimental: fetch a sandboxed UI-addon script from the repo and run
     *  it in the browser page (DOM-only, no native access). Lets users taste
     *  new UI without a full mod update. Off by default. */
    public boolean liveUi = false;
    public String lastCheckedVersion = "";
    public String dismissedVersion = "";

    private UpdateConfig()
    {
    }

    public static UpdateConfig get()
    {
        if (instance == null)
        {
            instance = load();
        }

        return instance;
    }

    private static Path file()
    {
        return FabricLoader.getInstance().getGameDir().resolve("bbs").resolve("updates.json");
    }

    private static UpdateConfig load()
    {
        Path f = file();

        try
        {
            if (Files.exists(f))
            {
                UpdateConfig c = GSON.fromJson(Files.readString(f), UpdateConfig.class);

                if (c != null)
                {
                    return c;
                }
            }
        }
        catch (Exception ignored)
        {
        }

        return new UpdateConfig();
    }

    public void save()
    {
        try
        {
            Path f = file();

            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(this));
        }
        catch (Exception ignored)
        {
        }
    }

    /** Build the raw URL for the version file at the repo root. */
    public static String versionFileUrl()
    {
        return "https://raw.githubusercontent.com/" + REPO + "/" + VERSION_BRANCH + "/" + VERSION_FILE;
    }

    /** Build the release download URL from a version number, following the
     *  repo convention:
     *  https://github.com/{RELEASE_REPO}/releases/download/v{VERSION}/bbs-next-{VERSION}-{MC_VERSION}.jar
     *  (the version.txt only needs to contain the bare version number). */
    public static String releaseDownloadUrl(String version)
    {
        return "https://github.com/" + RELEASE_REPO + "/releases/download/v" +
            version + "/bbs-next-" + version + "-" + MC_VERSION + ".jar";
    }
}
