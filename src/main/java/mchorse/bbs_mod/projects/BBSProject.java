package mchorse.bbs_mod.projects;

import java.nio.file.Path;

/**
 * A BBS Project - an isolated workspace for one creation (film, animation,
 * character, ...). Every project owns its own data directory under
 * <gameDir>/bbs/projects/<id>/ so different works never mix.
 */
public class BBSProject
{
    public final String id;
    public String name;
    public final long createdAt;

    /** Background world (singleplayer save folder name, "" = blank). */
    public String world = "";

    public BBSProject(String id, String name, long createdAt)
    {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public Path getDirectory()
    {
        return ProjectManager.getRoot().resolve(this.id);
    }
}
