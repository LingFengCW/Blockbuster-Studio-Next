package lingfeng.bbsnext.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DNS-over-HTTPS resolved HTTP client.
 *
 * <p>GitHub's domains are occasionally subject to DNS poisoning in some
 * networks. To stay resilient we never trust the system resolver for the
 * update endpoints: hostnames are resolved through Cloudflare's DoH resolver
 * (1.1.1.1) over a pinned TLS connection, then the actual HTTPS request is
 * opened to the resolved IP while presenting the real hostname as SNI, so the
 * certificate still validates.</p>
 */
public class DoH
{
    /* Cloudflare DoH endpoint, reached by its literal IP so we don't need a
     * working system resolver to bootstrap the resolver itself. */
    private static final String DOH_URL = "https://1.1.1.1/dns-query?name=%s&type=%s";

    private static final SSLSocketFactory SSLF = (SSLSocketFactory) SSLSocketFactory.getDefault();

    public static final class Resolved
    {
        public final String host;
        public final Socket socket;

        Resolved(String host, Socket socket)
        {
            this.host = host;
            this.socket = socket;
        }
    }

    /** True when {@code host} is already a numeric IP literal (IPv4 or IPv6),
     *  so no DNS/DoH lookup is needed (and must not recurse into one). */
    private static boolean isIpLiteral(String host)
    {
        if (host == null || host.isEmpty())
        {
            return false;
        }

        String h = host;

        if (h.startsWith("[") && h.endsWith("]"))
        {
            h = h.substring(1, h.length() - 1);
        }

        /* IPv6 contains a colon. */
        if (h.indexOf(':') >= 0)
        {
            return true;
        }

        /* IPv4: four dot-separated 0-255 octets. */
        String[] oct = h.split("\\.", -1);

        if (oct.length != 4)
        {
            return false;
        }

        for (String o : oct)
        {
            if (o.isEmpty() || o.length() > 3)
            {
                return false;
            }

            for (char c : o.toCharArray())
            {
                if (c < '0' || c > '9')
                {
                    return false;
                }
            }

            int v = Integer.parseInt(o);

            if (v > 255)
            {
                return false;
            }
        }

        return true;
    }

    /** Resolve a hostname to IP addresses through Cloudflare DoH. Falls back
     *  to the system resolver if DoH fails. */
    public static List<String> resolve(String host)
    {
        List<String> out = new ArrayList<>();

        /* IP literals need no resolution (and resolving them would recurse
         * into DoH forever, since the DoH endpoint itself is a literal IP). */
        if (isIpLiteral(host))
        {
            out.add(host);

            return out;
        }

        try
        {
            String json = dohQuery(host, "A");
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray answers = root.getAsJsonArray("Answer");

            if (answers != null)
            {
                for (int i = 0; i < answers.size(); i++)
                {
                    JsonObject a = answers.get(i).getAsJsonObject();
                    int type = a.has("type") ? a.get("type").getAsInt() : 0;
                    String data = a.has("data") ? a.get("data").getAsString() : "";

                    if ((type == 1 || type == 28) && !data.isEmpty())
                    {
                        out.add(data);
                    }
                }
            }
        }
        catch (Exception e)
        {
            /* fall through to system resolver */
        }

        if (out.isEmpty())
        {
            try
            {
                /* System resolver has no SO_TIMEOUT and can hang for a long time
                 * on restricted networks. Bound it with a daemon worker + join
                 * timeout so a poisoned/slow DNS lookup can't stall the caller. */
                java.util.concurrent.FutureTask<java.util.List<String>> task =
                    new java.util.concurrent.FutureTask<>(() ->
                    {
                        java.util.List<String> r = new java.util.ArrayList<>();

                        for (InetAddress ia : InetAddress.getAllByName(host))
                        {
                            r.add(ia.getHostAddress());
                        }

                        return r;
                    });

                Thread dns = new Thread(task, "bbs-dns-" + host);
                dns.setDaemon(true);
                dns.start();
                out.addAll(task.get(8000, java.util.concurrent.TimeUnit.MILLISECONDS));
            }
            catch (Exception ignored)
            {
                /* timed out or failed - leave out empty, caller will warn */
            }
        }

        return out;
    }

    private static String dohQuery(String host, String type) throws IOException
    {
        String url = String.format(DOH_URL, host, type);
        ByteArrayOutputStream sink = new ByteArrayOutputStream();

        fetch(url, sink, 0);

        return new String(sink.toByteArray(), StandardCharsets.UTF_8);
    }

    /** Open a raw TCP socket to one of the resolved IPs of {@code host}. */
    private static Resolved openRaw(String host, int port) throws IOException
    {
        List<String> ips = isIpLiteral(host)
            ? java.util.Collections.singletonList(host)
            : resolve(host);

        if (ips.isEmpty())
        {
            throw new IOException("DoH could not resolve " + host);
        }

        IOException last = null;

        for (String ip : ips)
        {
            try
            {
                Socket sock = new Socket();
                sock.connect(new InetSocketAddress(ip, port), 8000);

                return new Resolved(host, sock);
            }
            catch (IOException e)
            {
                last = e;
            }
        }

        throw last != null ? last : new IOException("connect failed: " + host);
    }

    /** Wrap a connected plain socket in TLS, presenting {@code host} as SNI so
     *  the certificate matches even though we connected to a raw IP. */
    private static SSLSocket wrap(Socket plain, String host, int port) throws IOException
    {
        SSLSocket s = (SSLSocket) SSLF.createSocket(plain, host, port, true);

        /* Tight timeout so a blocked handshake (restricted networks) fails
         * fast instead of hanging for minutes. The owning thread is also a
         * daemon, so even if SO_TIMEOUT is not honored by the handshake this
         * can't keep the JVM alive. */
        s.setSoTimeout(10000);
        s.startHandshake();

        return s;
    }

    /** Fetch a URL body as text (DoH-resolved, follows redirects). */
    public static String getText(String url) throws IOException
    {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        fetch(url, sink, 0);
        return new String(sink.toByteArray(), StandardCharsets.UTF_8);
    }

    /** Download a URL to a file, streaming the body (DoH-resolved, follows
     *  redirects). Used for the release jar, whose GitHub URL 302-redirects to
     *  a CDN host that is also resolved through DoH. */
    public static void download(String url, Path out) throws IOException
    {
        Files.createDirectories(out.getParent());

        try (OutputStream fos = Files.newOutputStream(out))
        {
            fetch(url, fos, 0);
        }
    }

    /** GET a URL into {@code sink}, following up to 5 HTTPS redirects. Each hop
     *  re-resolves its host via DoH so a poisoned redirect still can't sneak a
     *  bad IP past us. */
    private static void fetch(String url, OutputStream sink, int depth) throws IOException
    {
        if (depth > 5)
        {
            throw new IOException("too many redirects: " + url);
        }

        Url u = new Url(url);
        Resolved r = openRaw(u.host, u.port);
        SSLSocket s = wrap(r.socket, u.host, u.port);

        sendRequest(s, "GET", u.path, u.host, null);

        InputStream in = s.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        /* status line */
        String statusLine = reader.readLine();
        int status = 0;

        if (statusLine != null)
        {
            String[] parts = statusLine.split(" ");

            if (parts.length >= 2)
            {
                try
                {
                    status = Integer.parseInt(parts[1]);
                }
                catch (NumberFormatException ignored)
                {
                }
            }
        }

        Map<String, String> headers = new LinkedHashMap<>();
        String line;

        while ((line = reader.readLine()) != null && !line.isEmpty())
        {
            int idx = line.indexOf(':');

            if (idx > 0)
            {
                headers.put(line.substring(0, idx).trim().toLowerCase(), line.substring(idx + 1).trim());
            }
        }

        if ((status == 301 || status == 302 || status == 303 || status == 307 || status == 308)
            && headers.containsKey("location"))
        {
            String loc = headers.get("location");

            try
            {
                s.close();
            }
            catch (IOException ignored)
            {
            }

            if (loc.startsWith("https://"))
            {
                fetch(loc, sink, depth + 1);
            }
            else if (loc.startsWith("/"))
            {
                fetch("https://" + u.host + loc, sink, depth + 1);
            }
            else
            {
                fetch(url, sink, depth + 1); // best effort
            }

            return;
        }

        String te = headers.get("transfer-encoding");
        String cl = headers.get("content-length");

        if (te != null && te.toLowerCase().contains("chunked"))
        {
            readChunked(in, sink);
        }
        else if (cl != null)
        {
            long len = Long.parseLong(cl);
            copyExact(in, sink, len);
        }
        else
        {
            byte[] buf = new byte[8192];
            int n;

            while ((n = in.read(buf)) != -1)
            {
                sink.write(buf, 0, n);
            }
        }

        try
        {
            s.close();
        }
        catch (IOException ignored)
        {
        }
    }

    private static void sendRequest(SSLSocket s, String method, String fullUrl, String host, Map<String, String> extra) throws IOException
    {
        String path = fullUrl;

        if (path.startsWith("https://"))
        {
            path = path.substring("https://".length());
            int slash = path.indexOf('/');

            path = slash >= 0 ? path.substring(slash) : "/";
        }

        StringBuilder req = new StringBuilder();
        req.append(method).append(' ').append(path).append(" HTTP/1.1\r\n");
        req.append("Host: ").append(host).append("\r\n");
        req.append("User-Agent: BBS-Updater/1.0\r\n");
        req.append("Accept: */*\r\n");
        req.append("Connection: close\r\n");

        if (extra != null)
        {
            for (Map.Entry<String, String> e : extra.entrySet())
            {
                req.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
            }
        }

        req.append("\r\n");

        s.getOutputStream().write(req.toString().getBytes(StandardCharsets.UTF_8));
        s.getOutputStream().flush();
    }

    /** Read a chunked or content-length delimited body into {@code sink}. */
    private static void readChunked(InputStream in, OutputStream sink) throws IOException
    {
        byte[] buf = new byte[8192];

        while (true)
        {
            /* chunk size line (hex) */
            StringBuilder sizeLine = new StringBuilder();
            int c;

            while ((c = in.read()) != -1 && c != '\r')
            {
                sizeLine.append((char) c);
            }

            in.read(); // consume '\n'

            int size;

            try
            {
                size = Integer.parseInt(sizeLine.toString().trim(), 16);
            }
            catch (NumberFormatException e)
            {
                break;
            }

            if (size == 0)
            {
                in.read(); // consume trailing '\r'
                in.read(); // consume trailing '\n'
                break;
            }

            copyExact(in, sink, size);
            in.read(); // consume '\r'
            in.read(); // consume '\n'
        }
    }

    private static void copyExact(InputStream in, OutputStream sink, long len) throws IOException
    {
        byte[] buf = new byte[8192];
        long remaining = len;

        while (remaining > 0)
        {
            int toRead = (int) Math.min(buf.length, remaining);
            int n = in.read(buf, 0, toRead);

            if (n < 0)
            {
                break;
            }

            sink.write(buf, 0, n);
            remaining -= n;
        }
    }

    /** Tiny URL splitter for https URLs. */
    private static class Url
    {
        final String host;
        final int port;
        final String path;

        Url(String url)
        {
            if (!url.startsWith("https://"))
            {
                throw new IllegalArgumentException("only https supported: " + url);
            }

            String rest = url.substring("https://".length());
            int slash = rest.indexOf('/');
            String authority = slash >= 0 ? rest.substring(0, slash) : rest;
            this.path = slash >= 0 ? rest.substring(slash) : "/";

            int colon = authority.indexOf(':');
            String h = colon >= 0 ? authority.substring(0, colon) : authority;
            int p = colon >= 0 ? Integer.parseInt(authority.substring(colon + 1)) : 443;

            this.host = h;
            this.port = p;
        }
    }
}
