package org.us_ignite.thingworx.jgit.extension;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;

/**
 * Refreshes the file sizes and timestamps the Git index keeps for unchanged files, like {@code git
 * update-index --refresh}.
 *
 * <p>Status trusts a file whose size and timestamp match its index entry. When a file is rewritten
 * with the same content, its timestamp no longer matches, so every later Status reads and hashes it
 * again. JGit's Status never writes the index back, so this cost stays until the index is
 * refreshed. The refresh changes no staged content: it re-adds only files whose working-tree
 * content already equals the index, which writes the same object IDs with new file stats.
 */
final class IndexStatRefresh {
    private IndexStatRefresh() {}

    /**
     * Refreshes the index entries of every tracked file that has no unstaged change or conflict.
     *
     * @return the number of index entries considered
     */
    static int refresh(Git git) throws Exception {
        Status status = git.status().call();
        Set<String> skip = new HashSet<>();
        skip.addAll(status.getModified());
        skip.addAll(status.getMissing());
        skip.addAll(status.getConflicting());
        DirCache index = git.getRepository().readDirCache();
        AddCommand add = git.add();
        int count = 0;
        for (int i = 0; i < index.getEntryCount(); i++) {
            var entry = index.getEntry(i);
            String path = entry.getPathString();
            if (entry.getStage() != DirCacheEntry.STAGE_0 || skip.contains(path)) continue;
            add.addFilepattern(path);
            count++;
        }
        if (count > 0) add.call();
        return count;
    }
}
