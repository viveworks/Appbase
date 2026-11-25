# Config Wizard LGPL source and relink guide

The Appbase Config Wizard executable is a shaded JAR. Unlike Appbase's thin library JARs, it
contains unrelocated classes from the six LGPL components listed below. This directory travels
inside the executable so its licence terms, corresponding source, published build POMs, and these
instructions remain with the covered object code.

| Component | Licence used | Corresponding source | Published build POM |
|---|---|---|---|
| `com.google.code.findbugs:annotations:3.0.1` | `LGPL-2.1-or-later` | `annotations-3.0.1-sources.jar` | `annotations-3.0.1.pom` |
| `com.github.spotbugs:spotbugs-annotations:4.10.3` | `LGPL-2.1-only` | `spotbugs-annotations-4.10.3-sources.jar` | `spotbugs-annotations-4.10.3.pom` |
| `net.openhft:affinity:3.0.6` | `LGPL-3.0-only` | `affinity-3.0.6-sources.jar` | `affinity-3.0.6.pom` |
| `org.hibernate.common:hibernate-commons-annotations:6.0.6.Final` | `LGPL-2.1-or-later` | `hibernate-commons-annotations-6.0.6.Final-sources.jar` | `hibernate-commons-annotations-6.0.6.Final.pom` |
| `org.hibernate.orm:hibernate-core:6.5.3.Final` | `LGPL-2.1-or-later` | `hibernate-core-6.5.3.Final-sources.jar` | `hibernate-core-6.5.3.Final.pom` |
| `org.mariadb.jdbc:mariadb-java-client:2.7.4` | `LGPL-2.1-only` | `mariadb-java-client-2.7.4-sources.jar` | `mariadb-java-client-2.7.4.pom` |

The complete LGPL 2.1 and LGPL 3.0 texts are `COPYING.LESSER-2.1.txt` and
`COPYING.LESSER-3.0.txt`. Because LGPL 3.0 is expressed as additional permissions on GPL 3.0,
`COPYING.GPL-3.0.txt` accompanies it. `source-manifest.json` binds every coordinate and SPDX
expression to its source filename and SHA-256 digest.

## Verify the distributed source payload

Extract only this directory and verify every nested archive against the embedded manifest:

```sh
jar xf appbase-config-wizard-1.0.0.jar META-INF/lgpl
python3 - <<'PY'
import hashlib, json, pathlib
root = pathlib.Path("META-INF/lgpl")
manifest = json.loads((root / "source-manifest.json").read_text())
for dependency in manifest["dependencies"]:
    source = root / "sources" / dependency["source"]
    actual = hashlib.sha256(source.read_bytes()).hexdigest()
    assert actual == dependency["sha256"], (source, actual)
    print(actual, dependency["gav"])
PY
```

## Rebuild with a modified LGPL component

The reliable relink path is a full shaded-JAR rebuild. It requires a network connection to clone the
upstream repository and initially resolve its pinned build dependencies, a compatible JDK for that
historical tag, Git, and the repository's Maven or Gradle wrapper (or Maven where no wrapper is
present). Start from the exact upstream release and build the owning module:

```sh
git clone https://github.com/findbugsproject/findbugs.git findbugs-3.0.1
git -C findbugs-3.0.1 checkout 3.0.1
(cd findbugs-3.0.1 && ./gradlew :findbugs-annotations:jar)

git clone https://github.com/spotbugs/spotbugs.git spotbugs-4.10.3
git -C spotbugs-4.10.3 checkout 4.10.3
(cd spotbugs-4.10.3 && ./gradlew :spotbugs-annotations:jar)

git clone https://github.com/OpenHFT/Java-Thread-Affinity.git affinity-3.0.6
git -C affinity-3.0.6 checkout affinity-3.0.6
(cd affinity-3.0.6 && mvn -DskipTests package)

git clone https://github.com/hibernate/hibernate-commons-annotations.git hibernate-commons-6.0.6.Final
git -C hibernate-commons-6.0.6.Final checkout 6.0.6.Final
(cd hibernate-commons-6.0.6.Final && ./gradlew jar)

git clone https://github.com/hibernate/hibernate-orm.git hibernate-orm-6.5.3
git -C hibernate-orm-6.5.3 checkout 6.5.3
(cd hibernate-orm-6.5.3 && ./gradlew :hibernate-core:jar)

git clone https://github.com/mariadb-corporation/mariadb-connector-j.git mariadb-connector-j-2.7.4
git -C mariadb-connector-j-2.7.4 checkout 2.7.4
(cd mariadb-connector-j-2.7.4 && mvn -DskipTests package)
```

Apply the intended change and give the modified artifact a distinct local version using that
project's version setting. Install or publish the modified JAR and its generated POM into an
isolated Maven repository. Update Appbase dependency management to the same distinct version. That
version update **is an Appbase source change** and must be retained with the rebuilt source payload;
do not overwrite a shared cache or present modified code under the original release version. Then
perform a clean Config Wizard build against the isolated repository:

```sh
export LGPL_M2="$PWD/.m2-lgpl-relink"
mvn -Dmaven.repo.local="$LGPL_M2" install:install-file \
  -Dfile=/absolute/path/to/replacement.jar \
  -DpomFile=/absolute/path/to/replacement.pom
mvn -Dmaven.repo.local="$LGPL_M2" -pl appbase-config-wizard -am \
  -Dappbase-config-wizard.skipFrontend=true clean package
```

Confirm the rebuilt JAR's `META-INF/lgpl/source-manifest.json` and nested source archive describe
the modified component before redistributing it. If the replacement changes licensing or
attribution, update the packaged licence/NOTICE material as well.

## Relink without recompiling Appbase

Do not try to place replacement FindBugs/SpotBugs JARs before the executable with `java -jar`.
Those two artifacts have 19 overlapping class paths, and the fat JAR already contains the classes;
classpath order is therefore ambiguous and can silently mix implementations. There is no supported
no-Appbase-source-change relink for this shaded executable. The distinct dependency version requires
the Appbase source change described above, and `clean package` deliberately recompiles the affected
reactor rather than reusing compiled outputs. The shade goal must recreate the entire executable and
its `META-INF/MANIFEST.MF`; editing class entries in place is not a supported or reproducible relink
method.

After rebuilding, run the repository's Config Wizard LGPL payload checker against the final JAR.
