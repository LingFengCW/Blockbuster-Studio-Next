package lingfeng.bbsnext.update;

import lingfeng.bbsnext.mcef.MCEFUI;
import mchorse.bbs_mod.BBSMod;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sandboxed, UI-only "live content" channel.
 *
 * <p>The mod periodically fetches a single JavaScript file ({@code live/ui.js})
 * from the project repo (resolved over DoH, like the updater) and injects it
 * into the active MCEF browser page. The script runs inside the page's own
 * JavaScript context - it can only manipulate the DOM and call the existing
 * {@code window.send(...)} action channel. It has <b>no</b> access to the
 * filesystem, processes, or any network beyond what the page itself can reach.
 * That browser sandbox is the security boundary: this is a content-delivery
 * feature so users can taste new UI without a full mod update, <b>not</b>
 * arbitrary native code execution.</p>
 *
 * <p>Disabled by default ({@link UpdateConfig#liveUi}); flip it on in settings
 * to start pulling the script. The script is re-injected on every page load
 * (editor and dashboard) via {@link #injectIfReady()}, called from MCEF's load
 * handler, so it survives tab navigation.</p>
 */
public class LiveUi
{
    /** Repo-relative path of the sandboxed UI-addon script. */
    public static final String LIVE_FILE = "live/ui.js";

    private static volatile String scriptText;
    private static volatile String scriptHash;
    private static final AtomicBoolean checking = new AtomicBoolean(false);
    private static long lastCheckMs;

    /** Throttled entry point, called from each bridge's getStateJson. */
    public static void ensureChecked()
    {
        long now = System.currentTimeMillis();

        if (now - lastCheckMs > 5 * 60 * 1000)
        {
            checkAsync();
        }
    }

    public static void checkAsync()
    {
        if (!checking.compareAndSet(false, true))
        {
            return;
        }

        Thread t = new Thread(() ->
        {
            try
            {
                lastCheckMs = System.currentTimeMillis();
                doCheck();
            }
            catch (Exception e)
            {
                BBSMod.LOGGER.warn("[LiveUi] check failed: {}", e.getMessage());
            }
            finally
            {
                checking.set(false);
            }
        }, "bbs-live-ui");

        /* Daemon: same rationale as UpdateChecker - never block JVM exit. */
        t.setDaemon(true);
        t.start();
    }

    private static void doCheck() throws Exception
    {
        if (!UpdateConfig.get().liveUi)
        {
            return;
        }

        String url = "https://raw.githubusercontent.com/" + UpdateConfig.REPO + "/" +
            UpdateConfig.VERSION_BRANCH + "/" + LIVE_FILE;
        String text = DoH.getText(url).trim();

        if (text.isEmpty())
        {
            return;
        }

        String hash = sha256(text);

        if (hash.equals(scriptHash))
        {
            return; // unchanged since last fetch
        }

        scriptText = text;
        scriptHash = hash;

        /* Push into whatever page is currently shown. */
        injectIfReady();
    }

    /** Inject the current live UI script into the active browser page (no-op
     *  until a script has been fetched). Safe: runs only in page JS context. */
    public static void injectIfReady()
    {
        if (scriptText == null)
        {
            return;
        }

        MCEFUI.injectScript(scriptText);
    }

    private static String sha256(String s) throws Exception
    {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        byte[] b = d.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();

        for (byte x : b)
        {
            sb.append(String.format("%02x", x));
        }

        return sb.toString();
    }
}
