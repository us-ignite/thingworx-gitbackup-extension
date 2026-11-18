package org.us_ignite.thingworx.jgit.extension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Finds and reads the README at the root of the commit HEAD points at, as Git hosts show it on a
 * repository's front page. Reads the committed tree, not the working tree, so uncommitted edits are
 * not shown.
 */
public final class RepositoryReadme {
    /** Accepted names, most preferred first. Matching ignores case. */
    static final List<String> NAMES =
            List.of("readme.md", "readme.markdown", "readme.mdown", "readme", "readme.txt");

    /** The README's path, its text (empty if binary), full size, and whether it was cut short. */
    public record Readme(
            String path, String content, long size, boolean binary, boolean truncated) {}

    private RepositoryReadme() {}

    /**
     * Reads the README at HEAD.
     *
     * @return empty when HEAD has no commit yet or the root has no README
     */
    public static Optional<Readme> read(Repository repository, int maxBytes) throws IOException {
        ObjectId head = repository.resolve(Constants.HEAD);
        if (head == null) return Optional.empty();
        try (RevWalk walk = new RevWalk(repository);
                TreeWalk tree = new TreeWalk(repository)) {
            RevCommit commit = walk.parseCommit(head);
            tree.addTree(commit.getTree());
            tree.setRecursive(false);
            String bestPath = null;
            ObjectId bestId = null;
            int bestRank = NAMES.size();
            while (tree.next()) {
                FileMode mode = tree.getFileMode(0);
                if (!FileMode.REGULAR_FILE.equals(mode) && !FileMode.EXECUTABLE_FILE.equals(mode)) {
                    continue;
                }
                int rank = NAMES.indexOf(tree.getNameString().toLowerCase(Locale.ROOT));
                // Several case variants can coexist; take the first by byte order so the
                // choice does not depend on the platform.
                if (rank >= 0 && rank < bestRank) {
                    bestRank = rank;
                    bestPath = tree.getPathString();
                    bestId = tree.getObjectId(0);
                }
            }
            if (bestPath == null) return Optional.empty();
            ObjectLoader loader = repository.open(bestId);
            byte[] bytes;
            try (var stream = loader.openStream()) {
                bytes = stream.readNBytes(maxBytes);
            }
            boolean binary = RawText.isBinary(bytes);
            return Optional.of(
                    new Readme(
                            bestPath,
                            binary ? "" : new String(bytes, StandardCharsets.UTF_8),
                            loader.getSize(),
                            binary,
                            loader.getSize() > bytes.length));
        }
    }
}
