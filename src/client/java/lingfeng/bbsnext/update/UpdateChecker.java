package lingfeng.bbsnext.update;

import mchorse.bbs_mod.BBSMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background version checker + staged updater.
 *
 * <p>Flow: compare the running mod version against the {@code version.txt} at
 * the repository root (resolved over DoH). If a newer release exists, the
 * latest GitHub release asset is located. When the user confirms, the jar is
 * downloaded to a temp file and a PowerShell script is launched that waits for
 * Minecraft to quit, then swaps the jar in the mods folder.</p>
 */
public class UpdateChecker
{
    /** Non-null when a newer release is available (and not dismissed). */
    public static volatile UpdateInfo current;

    private static final AtomicBoolean checking = new AtomicBoolean(false);
    private static long lastCheckMs;

    /** Hold the discovered update so the HTML pages can render a notice. */
    public static class UpdateInfo
    {
        public final String version;
        public final String downloadUrl;
        public final String releaseName;
        public final String size;
        public volatile Path stagedJar; // filled after download

        UpdateInfo(String version, String downloadUrl, String releaseName, String size)
        {
            this.version = version;
            this.downloadUrl = downloadUrl;
            this.releaseName = releaseName;
            this.size = size;
        }
    }

    /** Called from the dashboard bridge once per session (throttled). */
    public static void ensureChecked()
    {
        long now = System.currentTimeMillis();

        if (now - lastCheckMs > 10 * 60 * 1000)
        {
            checkAsync();
        }
    }

    /** Run a version check on a background thread. */
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
                BBSMod.LOGGER.warn("[Update] check failed: {}", e.getMessage());
            }
            finally
            {
                checking.set(false);
            }
        }, "bbs-update-checker");

        /* Daemon: a slow/blocked network (e.g. DoH to GitHub on a restricted
         * network) must never keep the JVM alive and trip the shutdown watchdog
         * into a crash. The check fails gracefully via the catch above. */
        t.setDaemon(true);
        t.start();
    }

    private static void doCheck() throws IOException
    {
        UpdateConfig cfg = UpdateConfig.get();
        String local = localVersion();

        if (local == null)
        {
            return;
        }

        String raw = DoH.getText(UpdateConfig.versionFileUrl()).trim();
        String[] lines = raw.split("\\n");
        String remote = lines[0].trim();

        /* Optional line 2: a direct download URL for the release jar. When
         * present we skip the GitHub API lookup and use it verbatim (lets the
         * repo author point at any hosted jar). */
        String directUrl = lines.length > 1 ? lines[1].trim() : "";

        if (remote.isEmpty() || compareVersions(remote, local) <= 0)
        {
            current = null;

            return;
        }

        if (remote.equals(cfg.dismissedVersion))
        {
            current = null;

            return;
        }

        /* Newer release: the download URL is built from the version number
         * (repo convention). An explicit URL on line 2 of version.txt, if
         * present, overrides the pattern. */
        String url;

        if (directUrl.toLowerCase().startsWith("http") && directUrl.toLowerCase().endsWith(".jar"))
        {
            url = directUrl;
        }
        else
        {
            url = UpdateConfig.releaseDownloadUrl(remote);
        }

        UpdateInfo info = new UpdateInfo(remote, url, remote, "");

        if (info == null)
        {
            return;
        }

        current = info;

        if (cfg.autoUpdate)
        {
            stageDownload(info);
        }
    }

    /** Download the jar into the temp folder (idempotent). */
    public static void stageDownload(UpdateInfo info)
    {
        try
        {
            if (info.stagedJar != null && Files.exists(info.stagedJar))
            {
                return;
            }

            Path tmp = FabricLoader.getInstance().getGameDir().resolve("bbs").resolve("update");
            Files.createDirectories(tmp);

            String fileName = info.downloadUrl.substring(info.downloadUrl.lastIndexOf('/') + 1);
            Path out = tmp.resolve(fileName);

            DoH.download(info.downloadUrl, out);
            info.stagedJar = out;

            BBSMod.LOGGER.info("[Update] staged {}", out);
        }
        catch (Exception e)
        {
            BBSMod.LOGGER.warn("[Update] download failed: {}", e.getMessage());
        }
    }

    /** User confirmed: ensure the jar is downloaded, then launch the swap
     *  script (detached, so it survives MC exiting), and finally close the
     *  game. The script waits for the java process to leave, then swaps the
     *  jar in the mods folder - elevating to admin only if the plain copy is
     *  refused by permissions. */
    public static void applyUpdate()
    {
        UpdateInfo info = current;

        if (info == null)
        {
            return;
        }

        stageDownload(info);

        if (info.stagedJar == null || !Files.exists(info.stagedJar))
        {
            return;
        }

        try
        {
            Path mods = FabricLoader.getInstance().getGameDir().resolve("mods");
            Path ps1 = FabricLoader.getInstance().getGameDir().resolve("bbs").resolve("update").resolve("apply_update.ps1");

            Files.createDirectories(ps1.getParent());
            Files.writeString(ps1, buildPs1(mods.toString(), info.stagedJar.toString()));

            /* Launch DETACHED via cmd/start so the script keeps running after
             * this game process exits. */
            new ProcessBuilder("cmd", "/c", "start", "", "powershell", "-NoProfile",
                "-ExecutionPolicy", "Bypass", "-File", ps1.toString())
                .redirectErrorStream(true)
                .start();

            BBSMod.LOGGER.info("[Update] launched updater, Minecraft will now close");

            /* End the game; the detached script swaps the jar once java is gone. */
            Minecraft.getInstance().stop();
        }
        catch (Exception e)
        {
            BBSMod.LOGGER.warn("[Update] could not launch updater: {}", e.getMessage());
        }
    }

    /** PowerShell that waits for MC to quit, removes the old jar, and copies
     *  the new one. If the plain copy is denied by permissions it re-launches
     *  itself elevated (-Verb RunAs) once; the -Elevated guard stops any loop. */
    private static String buildPs1(String mods, String jar)
    {
        String newName = Path.of(jar).getFileName().toString();

        return
            "param([switch]$Elevated)\n" +
            "$ErrorActionPreference = 'Stop'\n" +
            "$mods = '" + mods.replace("'", "''") + "'\n" +
            "$newJar = '" + jar.replace("'", "''") + "'\n" +
            "$newName = Split-Path $newJar -Leaf\n" +
            "# Wait for Minecraft (java) to close before swapping the jar.\n" +
            "while (Get-Process -Name java -ErrorAction SilentlyContinue) { Start-Sleep -Seconds 2 }\n" +
            "function Copy-Jar {\n" +
            "  Get-ChildItem \"$mods\\bbs-next-*.jar\" | Where-Object { $_.Name -ne $newName } | Remove-Item -Force\n" +
            "  Copy-Item $newJar \"$mods\\$newName\" -Force\n" +
            "}\n" +
            "try { Copy-Jar } catch {\n" +
            "  if ($Elevated) { Write-Error \"替换失败: $_\"; exit 1 }\n" +
            "  Start-Process powershell -Verb RunAs -ArgumentList \"-NoProfile -ExecutionPolicy Bypass -File `\"$PSCommandPath`\" -Elevated\"\n" +
            "  exit\n" +
            "}\n";
    }

    public static void dismiss()
    {
        if (current != null)
        {
            UpdateConfig cfg = UpdateConfig.get();
            cfg.dismissedVersion = current.version;
            cfg.save();
            current = null;
        }
    }

    /** The currently running mod version, from Fabric Loader metadata. */
    public static String localVersion()
    {
        try
        {
            return FabricLoader.getInstance().getModContainer("bbs-next")
                .orElseThrow().getMetadata().getVersion().getFriendlyString();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** >0 if {@code a} is newer than {@code b}. Ignores any build suffix. */
    public static int compareVersions(String a, String b)
    {
        a = strip(a);
        b = strip(b);

        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int n = Math.max(pa.length, pb.length);

        for (int i = 0; i < n; i++)
        {
            int xa = i < pa.length ? parseInt(pa[i]) : 0;
            int xb = i < pb.length ? parseInt(pb[i]) : 0;

            if (xa != xb)
            {
                return Integer.compare(xa, xb);
            }
        }

        return 0;
    }

    private static String strip(String v)
    {
        if (v == null)
        {
            return "";
        }

        v = v.trim();

        if (v.toLowerCase().startsWith("v"))
        {
            v = v.substring(1);
        }

        int dash = v.indexOf('-');

        if (dash >= 0)
        {
            v = v.substring(0, dash);
        }

        return v;
    }

    private static int parseInt(String s)
    {
        StringBuilder digits = new StringBuilder();

        for (char c : s.toCharArray())
        {
            if (Character.isDigit(c))
            {
                digits.append(c);
            }
            else
            {
                break;
            }
        }

        return digits.length() == 0 ? 0 : Integer.parseInt(digits.toString());
    }
}
