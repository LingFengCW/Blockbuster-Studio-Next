package mchorse.bbs_mod.projects;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.MapType;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Manages all BBS projects. Each project owns an isolated data directory
 * under <gameDir>/bbs/projects/<id>/, so works never share state.
 *
 * The currently active project is exposed through {@link #getCurrent()};
 * all BBS data writes should be scoped to it.
 */
public class ProjectManager
{
    private static ProjectManager instance;

    private static final String CURRENT_FILE = ".current";

    private final List<BBSProject> projects = new ArrayList<>();
    private BBSProject current;

    public static ProjectManager get()
    {
        if (instance == null)
        {
            instance = new ProjectManager();
            instance.loadAll();
        }

        return instance;
    }

    public static Path getRoot()
    {
        return FabricLoader.getInstance().getGameDir().resolve("bbs").resolve("projects");
    }

    public void loadAll()
    {
        this.projects.clear();

        Path root = getRoot();

        if (!Files.isDirectory(root))
        {
            return;
        }

        try (Stream<Path> dirs = Files.list(root))
        {
            dirs.filter(Files::isDirectory).forEach(dir ->
            {
                Path meta = dir.resolve("project.json");

                if (Files.exists(meta))
                {
                    try
                    {
                        Object data = DataStorageUtils.readFromBytes(Files.readAllBytes(meta));

                        if (data instanceof MapType map)
                        {
                            String id = dir.getFileName().toString();
                            String name = map.getString("name");
                            long createdAt = map.getLong("createdAt", 0L);
                            BBSProject project = new BBSProject(id, name.isEmpty() ? id : name, createdAt);

                            project.world = map.getString("world");
                            this.projects.add(project);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        this.projects.sort(Comparator.comparingLong((BBSProject p) -> p.createdAt).reversed());

        /* Restore the last used project so the workspace survives restarts. */
        this.current = null;

        try
        {
            Path currentFile = getRoot().resolve(CURRENT_FILE);

            if (Files.isRegularFile(currentFile))
            {
                String id = new String(Files.readAllBytes(currentFile)).trim();

                if (!id.isEmpty())
                {
                    for (BBSProject project : this.projects)
                    {
                        if (project.id.equals(id))
                        {
                            this.current = project;
                            break;
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public List<BBSProject> getProjects()
    {
        return this.projects;
    }

    public BBSProject getCurrent()
    {
        return this.current;
    }

    public void setCurrent(BBSProject project)
    {
        this.current = project;
        this.saveCurrent();
    }

    /**
     * Persist the id of the active project so it is restored on next launch.
     * Writing the id (or an empty string when none) keeps the file always
     * present and valid.
     */
    private void saveCurrent()
    {
        try
        {
            Path currentFile = getRoot().resolve(CURRENT_FILE);

            Files.createDirectories(currentFile.getParent());
            Files.write(currentFile, (this.current == null ? "" : this.current.id).getBytes());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public BBSProject create(String name)
    {
        return this.create(name, "");
    }

    public BBSProject create(String name, String world)
    {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        BBSProject project = new BBSProject(id, name, System.currentTimeMillis());

        project.world = world == null ? "" : world;

        try
        {
            Files.createDirectories(project.getDirectory());
            this.save(project);
            this.projects.add(project);
            this.projects.sort(Comparator.comparingLong((BBSProject p) -> p.createdAt).reversed());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return project;
    }

    /** Rename a project. The data directory is keyed by the stable id, so
     * renaming never touches the folder - only the stored name changes. */
    public void rename(BBSProject project, String name)
    {
        if (project == null || name == null || name.trim().isEmpty())
        {
            return;
        }

        project.name = name.trim();
        this.save(project);
    }

    public void delete(BBSProject project)
    {        try
        {
            Path dir = project.getDirectory();

            if (Files.isDirectory(dir))
            {
                try (Stream<Path> walk = Files.walk(dir))
                {
                    walk.sorted(Comparator.reverseOrder()).forEach(path ->
                    {
                        try
                        {
                            Files.delete(path);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        this.projects.remove(project);

        if (this.current == project)
        {
            this.current = null;
            this.saveCurrent();
        }
    }

    public void save(BBSProject project)
    {
        try
        {
            MapType data = new MapType();
            data.putString("name", project.name);
            data.putLong("createdAt", project.createdAt);
            data.putString("world", project.world);

            Path meta = project.getDirectory().resolve("project.json");

            Files.createDirectories(meta.getParent());
            Files.write(meta, DataStorageUtils.writeToBytes(data), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
