package mchorse.bbs_mod.projects;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the reusable {@link Sequence} documents of the current project.
 *
 * Layout per design doc:
 *   <project>/sequences/index.json       - id/name/createdAt list + current
 *   <project>/sequences/<id>.seq.json    - one sequence's reference list
 *
 * Sequences reference assets by id only; nothing here ever rewrites the
 * source .mcpr files or scene data (non-destructive editing).
 */
public class SequenceManager
{
    public static final String SEQUENCES_DIR = "sequences";
    public static final String INDEX_FILE = "index.json";
    public static final String SEQ_EXT = ".seq.json";

    /** Maximum nesting depth of sequence references (design doc: 8). */
    public static final int MAX_DEPTH = 8;

    private static SequenceManager instance;
    private static String boundProjectId;

    /** Lazy singleton bound to the current project, mirroring SceneManager. */
    public static SequenceManager get()
    {
        BBSProject project = ProjectManager.get() == null ? null : ProjectManager.get().getCurrent();

        if (project == null)
        {
            instance = null;
            boundProjectId = null;

            return null;
        }

        if (instance == null || !project.id.equals(boundProjectId))
        {
            instance = new SequenceManager(project);
            boundProjectId = project.id;

            instance.loadAll();
        }

        return instance;
    }

    public static void reset()
    {
        instance = null;
        boundProjectId = null;
    }

    private final BBSProject project;
    private final List<Sequence> sequences = new ArrayList<>();

    private SequenceManager(BBSProject project)
    {
        this.project = project;
    }

    public BBSProject getProject()
    {
        return this.project;
    }

    public Path getDirectory()
    {
        return this.project.getDirectory().resolve(SEQUENCES_DIR);
    }

    public List<Sequence> getSequences()
    {
        return this.sequences;
    }

    public Sequence getById(String id)
    {
        for (Sequence sequence : this.sequences)
        {
            if (sequence.id.equals(id))
            {
                return sequence;
            }
        }

        return null;
    }

    /* Creation / deletion */

    public Sequence create(String name)
    {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Sequence sequence = new Sequence(id, name.isEmpty() ? "Sequence" : name, System.currentTimeMillis());

        this.sequences.add(sequence);
        this.sort();
        this.save(sequence);
        this.saveIndex();

        return sequence;
    }

    public void rename(Sequence sequence, String name)
    {
        if (sequence == null || name == null || name.trim().isEmpty())
        {
            return;
        }

        sequence.name = name.trim();
        this.save(sequence);
        this.saveIndex();
    }

    public void delete(Sequence sequence)
    {
        this.sequences.remove(sequence);

        try
        {
            Files.deleteIfExists(this.fileOf(sequence.id));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        this.saveIndex();
    }

    /* References */

    public void addRef(Sequence sequence, String type, String id)
    {
        if (sequence == null || id == null || id.isEmpty())
        {
            return;
        }

        if (Sequence.SequenceRef.SEQUENCE.equals(type))
        {
            /* Sequences may nest inside other sequences, but never create a
             * cycle: A cannot contain A, and A -> B -> A is forbidden too.
             * hasCycle() walks forward from the target and reports true when
             * it can reach the source again. */
            if (this.hasCycle(sequence, id))
            {
                return;
            }
        }

        sequence.refs.add(new Sequence.SequenceRef(type, id));
        this.save(sequence);
    }

    public void removeRef(Sequence sequence, Sequence.SequenceRef ref)
    {
        if (sequence == null)
        {
            return;
        }

        sequence.refs.remove(ref);
        this.save(sequence);
    }

    /** True when adding {@code targetId} into {@code source} would create a cycle. */
    public boolean hasCycle(Sequence source, String targetId)
    {
        if (source == null)
        {
            return false;
        }

        /* If the target equals the source, obviously a cycle. */
        if (source.id.equals(targetId))
        {
            return true;
        }

        /* Walk forward from the target: if we can reach the source again
         * through nested sequence references, adding the link closes a loop. */
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();

        queue.add(targetId);

        while (!queue.isEmpty())
        {
            String currentId = queue.poll();

            if (source.id.equals(currentId))
            {
                return true;
            }

            if (!visited.add(currentId))
            {
                continue;
            }

            Sequence current = this.getById(currentId);

            if (current != null)
            {
                for (Sequence.SequenceRef ref : current.refs)
                {
                    if (ref.isSequence())
                    {
                        queue.add(ref.id);
                    }
                }
            }
        }

        return false;
    }

    /** Nesting depth of a sequence (1 = top level, MAX_DEPTH = deepest allowed). */
    public int depthOf(String id)
    {
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();

        queue.add(id);
        depths.add(1);

        int max = 1;

        while (!queue.isEmpty())
        {
            String currentId = queue.poll();
            int depth = depths.poll();

            max = Math.max(max, depth);

            if (!visited.add(currentId))
            {
                continue;
            }

            Sequence current = this.getById(currentId);

            if (current != null)
            {
                for (Sequence.SequenceRef ref : current.refs)
                {
                    if (ref.isSequence())
                    {
                        queue.add(ref.id);
                        depths.add(depth + 1);
                    }
                }
            }
        }

        return max;
    }

    /**
     * Export a sequence as a standalone document into the export folder.
     *
     * The exported file mirrors the on-disk {@code .seq.json} layout (a
     * "sequence" document with its reference list), wrapped with a type
     * marker so the importer can tell it apart from scene documents.
     *
     * @return the exported file, or null on failure
     */
    public File exportSequence(Sequence sequence)
    {
        if (sequence == null)
        {
            return null;
        }

        try
        {
            MapType data = this.exportSequenceData(sequence);

            File folder = mchorse.bbs_mod.BBSMod.getExportFolder();
            File file = new File(folder, this.sanitize(sequence.name) + ".seqbbs");

            Files.createDirectories(folder.toPath());
            Files.write(file.toPath(), DataStorageUtils.writeToBytes(data), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return file;
        }
        catch (Exception e)
        {
            e.printStackTrace();

            return null;
        }
    }

    /**
     * Serialize a sequence (descriptor + reference list) into a document.
     * Shared by the file exporter and the backpack.
     */
    public MapType exportSequenceData(Sequence sequence)
    {
        MapType data = new MapType();

        if (sequence == null)
        {
            return data;
        }

        data.putString("type", "sequence");
        data.putString("id", sequence.id);
        data.putString("name", sequence.name);
        data.putLong("createdAt", sequence.createdAt);

        ListType refs = new ListType();

        for (Sequence.SequenceRef ref : sequence.refs)
        {
            MapType entry = new MapType();

            entry.putString("type", ref.type);
            entry.putString("id", ref.id);
            entry.putLong("in", ref.in);
            entry.putLong("out", ref.out);

            refs.add(entry);
        }

        data.put("refs", refs);

        return data;
    }

    /** Import a previously exported sequence document back into the project. */
    public Sequence importSequence(File file)
    {
        if (file == null || !file.isFile())
        {
            return null;
        }

        try
        {
            BaseType data = DataStorageUtils.readFromBytes(Files.readAllBytes(file.toPath()));

            if (!(data instanceof MapType map))
            {
                return null;
            }

            return this.importSequenceData(map, file.getName().replace(".seqbbs", ""));
        }
        catch (Exception e)
        {
            e.printStackTrace();

            return null;
        }
    }

    /**
     * Create a new sequence in this project from a sequence document.
     *
     * References to scenes are kept as-is: a scene that does not exist in
     * this project simply renders as a dead link in the editor, which the
     * user can repoint - that is far less surprising than silently dropping
     * the clip.
     */
    public Sequence importSequenceData(MapType map, String fallbackName)
    {
        if (map == null)
        {
            return null;
        }

        try
        {
            String name = map.getString("name");

            if (name.isEmpty())
            {
                name = fallbackName == null || fallbackName.isEmpty() ? "Sequence" : fallbackName;
            }

            Sequence sequence = this.create(name);

            ListType refs = map.getList("refs");

            for (int i = 0; i < refs.size(); i++)
            {
                MapType entry = refs.getMap(i);
                String type = entry.getString("type");
                String refId = entry.getString("id");

                /* Cycle guard: refuse a link that would close a loop. */
                if (Sequence.SequenceRef.SEQUENCE.equals(type) && this.hasCycle(sequence, refId))
                {
                    continue;
                }

                sequence.refs.add(new Sequence.SequenceRef(
                    type, refId, entry.getLong("in", -1L), entry.getLong("out", -1L)
                ));
            }

            this.save(sequence);

            return sequence;
        }
        catch (Exception e)
        {
            e.printStackTrace();

            return null;
        }
    }

    /** Filesystem-safe name: strip path separators and control characters. */
    private String sanitize(String name)
    {
        return name.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
    }

    /* I/O */

    private void sort()
    {
        this.sequences.sort(Comparator.comparingLong((Sequence s) -> s.createdAt).reversed());
    }

    private Path fileOf(String id)
    {
        return this.getDirectory().resolve(id + SEQ_EXT);
    }

    public void save(Sequence sequence)
    {
        try
        {
            MapType data = new MapType();

            data.putString("id", sequence.id);
            data.putString("name", sequence.name);
            data.putLong("createdAt", sequence.createdAt);

            ListType refs = new ListType();

            for (Sequence.SequenceRef ref : sequence.refs)
            {
                MapType entry = new MapType();

                entry.putString("type", ref.type);
                entry.putString("id", ref.id);
                entry.putLong("in", ref.in);
                entry.putLong("out", ref.out);

                refs.add(entry);
            }

            data.put("refs", refs);

            Path file = this.fileOf(sequence.id);

            Files.createDirectories(file.getParent());
            Files.write(file, DataStorageUtils.writeToBytes(data), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void saveIndex()
    {
        try
        {
            MapType list = new MapType();

            for (Sequence sequence : this.sequences)
            {
                MapType entry = new MapType();

                entry.putString("name", sequence.name);
                entry.putLong("createdAt", sequence.createdAt);

                list.put(sequence.id, entry);
            }

            MapType data = new MapType();

            data.put("sequences", list);

            Path index = this.getDirectory().resolve(INDEX_FILE);

            Files.createDirectories(index.getParent());
            Files.write(index, DataStorageUtils.writeToBytes(data), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void loadAll()
    {
        this.sequences.clear();

        Path index = this.getDirectory().resolve(INDEX_FILE);

        if (!Files.isRegularFile(index))
        {
            return;
        }

        try
        {
            BaseType data = DataStorageUtils.readFromBytes(Files.readAllBytes(index));

            if (!(data instanceof MapType map))
            {
                return;
            }

            MapType list = map.getMap("sequences");

            for (String id : list.keys())
            {
                MapType entry = list.getMap(id);
                String name = entry.getString("name");

                this.sequences.add(new Sequence(id, name.isEmpty() ? id : name, entry.getLong("createdAt", 0L)));
            }

            this.sort();

            /* Load each sequence's reference list lazily. */
            for (Sequence sequence : this.sequences)
            {
                this.loadRefs(sequence);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void loadRefs(Sequence sequence)
    {
        Path file = this.fileOf(sequence.id);

        if (!Files.isRegularFile(file))
        {
            return;
        }

        try
        {
            BaseType data = DataStorageUtils.readFromBytes(Files.readAllBytes(file));

            if (!(data instanceof MapType map))
            {
                return;
            }

            ListType refs = map.getList("refs");

            for (int i = 0; i < refs.size(); i++)
            {
                MapType entry = refs.getMap(i);
                Sequence.SequenceRef ref = new Sequence.SequenceRef(
                    entry.getString("type"),
                    entry.getString("id"),
                    entry.getLong("in", -1L),
                    entry.getLong("out", -1L)
                );

                sequence.refs.add(ref);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
