# Maven reactor root marker

This tracked directory marks the repository root for Maven invocations started from a nested module.
It keeps `${maven.multiModuleProjectDirectory}` anchored here, so repository-relative build inputs—
including the Checkstyle rules, raw-scan suppression file, exact baseline, and comparator script—resolve
to the same files whether Maven starts at the root or below `appbase-java/`.

Keep this marker tracked. An otherwise empty `.mvn/` directory is not preserved by Git, and removing it
can make module-local Maven invocations treat the module directory as the multi-module root.
