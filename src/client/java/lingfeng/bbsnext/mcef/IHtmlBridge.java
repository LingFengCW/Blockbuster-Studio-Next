package lingfeng.bbsnext.mcef;

/**
 * Common contract for any HTML page hosted inside the MCEF browser overlay.
 *
 * <p>An implementation owns one logical page: it knows which HTML file to
 * load ({@link #pageUrl()}), how to serialize the page's data
 * ({@link #getStateJson()}, pushed into {@code window.bbsState}), and how to
 * react to user actions sent from the page ({@link #handle(String)}).</p>
 *
 * <p>The editor ({@link EditorBridge}) and the dashboard ({@link DashboardBridge})
 * both implement this, so {@link MCEFUI} can host either one without knowing
 * which is active.</p>
 */
public interface IHtmlBridge
{
    /** Serialized page state, pushed to the page as {@code window.bbsState}. */
    String getStateJson();

    /** Handle an action request coming from the page (JSON string). */
    String handle(String request);

    /** Absolute file:// URL of the HTML page to load into the browser. */
    String pageUrl();
}
