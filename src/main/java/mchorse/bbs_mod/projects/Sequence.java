package mchorse.bbs_mod.projects;

import java.util.ArrayList;
import java.util.List;

/**
 * A reusable clip sequence (design doc: "序列").
 *
 * A sequence is a thin, non-destructive editor document: it references
 * existing assets (scenes, other sequences, .mcpr replays, audio) by id
 * with in/out trim ranges instead of copying their data. Sequences are
 * stored as independent {@code .seq.json} files and may nest into each
 * other (a clip can reference a child sequence), which the editor renders
 * recursively.
 */
public class Sequence
{
    public final String id;
    public String name;
    public final long createdAt;

    /** Asset references in play order: scene / sequence / mcpr / audio. */
    public final List<SequenceRef> refs = new ArrayList<>();

    public Sequence(String id, String name, long createdAt)
    {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    /** One asset reference with an optional trim window (in frames). */
    public static class SequenceRef
    {
        public static final String SCENE = "scene";
        public static final String SEQUENCE = "sequence";
        public static final String MCPR = "mcpr";
        public static final String AUDIO = "audio";

        public String type;
        public String id;
        public long in;
        public long out;

        public SequenceRef(String type, String id)
        {
            this(type, id, -1, -1);
        }

        public SequenceRef(String type, String id, long in, long out)
        {
            this.type = type;
            this.id = id;
            this.in = in;
            this.out = out;
        }

        /** True when this reference targets another sequence (nesting). */
        public boolean isSequence()
        {
            return SEQUENCE.equals(this.type);
        }
    }

    @Override
    public String toString()
    {
        return "Sequence{" + this.id + ", " + this.name + ", refs=" + this.refs.size() + "}";
    }
}
