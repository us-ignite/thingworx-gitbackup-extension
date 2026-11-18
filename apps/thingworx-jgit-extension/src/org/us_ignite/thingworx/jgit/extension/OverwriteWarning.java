package org.us_ignite.thingworx.jgit.extension;

import java.util.List;

/** The text that asks before Sync to ThingWorx overwrites entities changed in ThingWorx. */
final class OverwriteWarning {
    /** How many changed entities the text names before summarizing the rest. */
    static final int SHOWN = 10;

    private OverwriteWarning() {}

    /**
     * Describes the entities a Sync to ThingWorx would overwrite, each as {@code "<Type> <name>"},
     * or returns blank when there are none.
     */
    static String forChanges(List<String> changed) {
        if (changed.isEmpty()) return "";
        List<String> shown = changed.subList(0, Math.min(changed.size(), SHOWN));
        String names = String.join(", ", shown);
        if (changed.size() > shown.size()) {
            names += " and " + (changed.size() - shown.size()) + " more";
        }
        return changed.size()
                + (changed.size() == 1 ? " entity has" : " entities have")
                + " changed in ThingWorx since the last sync: "
                + names
                + ". Sync to ThingWorx replaces them with the repository versions. Use Sync from"
                + " ThingWorx first to keep those changes.";
    }
}
