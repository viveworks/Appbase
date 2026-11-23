#!/usr/bin/env python3
"""Light contract test for the release Javadoc visibility used by AD-09.

@author Navaneeth Mysore G
@author Claude
"""

from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
ALLOWED = {"public", "protected"}
STRICT_DOCLINT = {"all", "all,-missing"}


def local(element):
    return element.tag.rsplit("}", 1)[-1]


def child_text(element, name):
    for child in element:
        if local(child) == name:
            return (child.text or "").strip()
    return None


def children(element, name):
    return [child for child in element if local(child) == name]


def official_javadoc_plugin(element):
    return (
        local(element) == "plugin"
        and child_text(element, "artifactId") == "maven-javadoc-plugin"
        and child_text(element, "groupId") in (None, "org.apache.maven.plugins")
    )


def visibility_problems(path):
    raw = path.read_bytes()
    if b"<!DOCTYPE" in raw.upper():
        return ["DOCTYPE is forbidden"]
    root = ET.fromstring(raw)
    profiles = [
        profile for profile in root.iter()
        if local(profile) == "profile" and child_text(profile, "id") == "release-profile"
    ]
    if len(profiles) != 1:
        return ["exactly one release-profile is required"]
    profile = profiles[0]
    plugins = [plugin for plugin in root.iter() if official_javadoc_plugin(plugin)]
    authoritative = []
    for build in children(profile, "build"):
        for management in children(build, "pluginManagement"):
            for container in children(management, "plugins"):
                for plugin in children(container, "plugin"):
                    if not official_javadoc_plugin(plugin):
                        continue
                    for configuration in children(plugin, "configuration"):
                        authoritative.append(child_text(configuration, "show"))
    if len(authoritative) != 1 or authoritative[0] is None:
        return ["release-profile pluginManagement needs exactly one explicit show setting"]
    configured = []
    for plugin in plugins:
        for configuration in plugin.iter():
            if local(configuration) == "configuration":
                value = child_text(configuration, "show")
                if value is not None:
                    configured.append(value.strip().lower())
    return ["non-public Javadoc visibility: %s" % value for value in configured if value not in ALLOWED]


def release_gate_problems(path):
    """Return unsafe settings from the authoritative release Javadoc configuration."""
    raw = path.read_bytes()
    if b"<!DOCTYPE" in raw.upper():
        return ["DOCTYPE is forbidden"]
    root = ET.fromstring(raw)
    profiles = [
        profile for profile in root.iter()
        if local(profile) == "profile" and child_text(profile, "id") == "release-profile"
    ]
    if len(profiles) != 1:
        return ["exactly one release-profile is required"]
    authoritative = []
    authoritative_plugins = []
    for build in children(profiles[0], "build"):
        for management in children(build, "pluginManagement"):
            for container in children(management, "plugins"):
                for plugin in children(container, "plugin"):
                    if official_javadoc_plugin(plugin):
                        authoritative_plugins.append(plugin)
                        authoritative.extend(children(plugin, "configuration"))
    if len(authoritative) != 1 or len(authoritative_plugins) != 1:
        return ["release-profile pluginManagement needs exactly one Javadoc configuration"]
    properties = {}
    for container in children(root, "properties") + children(profiles[0], "properties"):
        for setting in container:
            properties[local(setting)] = (setting.text or "").strip()

    def resolved(configuration, name):
        value = child_text(configuration, name)
        if value and value.startswith("${") and value.endswith("}"):
            return properties.get(value[2:-1])
        return value

    def resolve_value(value):
        if value and value.startswith("${") and value.endswith("}"):
            return properties.get(value[2:-1])
        return value

    def plugin_configurations(plugin):
        result = children(plugin, "configuration")
        for executions in children(plugin, "executions"):
            for execution in children(executions, "execution"):
                result.extend(children(execution, "configuration"))
        return result

    overrides = []
    active_builds = children(root, "build") + children(profiles[0], "build")
    for build in active_builds:
        for container in children(build, "plugins"):
            for plugin in children(container, "plugin"):
                if official_javadoc_plugin(plugin):
                    overrides.extend(plugin_configurations(plugin))

    problems = []
    configuration = authoritative[0]
    version = resolve_value(child_text(authoritative_plugins[0], "version")) or ""
    try:
        version_parts = tuple(int(part) for part in version.split("."))
    except ValueError:
        version_parts = ()
    if version_parts < (3, 12, 0):
        problems.append("maven-javadoc-plugin 3.12.0 or newer is required")
    additions = []
    for container in children(configuration, "additionalDependencies"):
        for dependency in children(container, "additionalDependency"):
            additions.append((child_text(dependency, "groupId"), child_text(dependency, "artifactId"),
                              resolved(dependency, "version")))
    required_spark = {("org.apache.spark", artifact, properties.get("spark.version")) for artifact in
                      ("spark-core_2.12", "spark-sql_2.12", "spark-streaming_2.12")}
    if not required_spark.issubset(set(additions)):
        problems.append("direct Spark aggregate-classpath dependencies are required")
    doclint = (resolved(configuration, "doclint") or "").lower()
    if doclint not in STRICT_DOCLINT:
        problems.append("strict doclint is required")
    if (resolved(configuration, "failOnError") or "").lower() != "true":
        problems.append("failOnError must be enabled")
    if (resolved(configuration, "validateLinks") or "").lower() != "true":
        problems.append("link validation must be enabled")
    if (resolved(configuration, "author") or "").lower() != "false":
        problems.append("author output must be explicitly disabled")
    for override in overrides:
        present = {name: child_text(override, name) is not None for name in
                   ("doclint", "failOnError", "validateLinks", "author")}
        values = {name: resolved(override, name) for name in present}
        if present["doclint"] and (values["doclint"] or "").lower() not in STRICT_DOCLINT:
            problems.append("strict doclint override is required")
        if present["failOnError"] and (values["failOnError"] or "").lower() != "true":
            problems.append("failOnError override must stay enabled")
        if present["validateLinks"] and (values["validateLinks"] or "").lower() != "true":
            problems.append("link validation override must stay enabled")
        if present["author"] and (values["author"] or "").lower() != "false":
            problems.append("author output override must stay disabled")
    return problems


def fixture(show):
    setting = "" if show is None else "<show>%s</show>" % show
    return (
        "<project><profiles><profile><id>release-profile</id><build><pluginManagement><plugins><plugin>"
        "<groupId>org.apache.maven.plugins</groupId><artifactId>maven-javadoc-plugin</artifactId>"
        "<configuration>%s</configuration>"
        "</plugin></plugins></pluginManagement></build></profile></profiles></project>" % setting
    )


def gate_fixture(settings, root_properties="", release_properties="", extra=""):
    return (
        "<project><properties>%s</properties>%s<profiles><profile><id>release-profile</id>"
        "<properties>%s</properties><build><pluginManagement><plugins><plugin>"
        "<groupId>org.apache.maven.plugins</groupId><artifactId>maven-javadoc-plugin</artifactId>"
        "<version>3.12.0</version><configuration>%s<additionalDependencies>"
        "<additionalDependency><groupId>org.apache.spark</groupId><artifactId>spark-core_2.12</artifactId>"
        "<version>${spark.version}</version></additionalDependency>"
        "<additionalDependency><groupId>org.apache.spark</groupId><artifactId>spark-sql_2.12</artifactId>"
        "<version>${spark.version}</version></additionalDependency>"
        "<additionalDependency><groupId>org.apache.spark</groupId><artifactId>spark-streaming_2.12</artifactId>"
        "<version>${spark.version}</version></additionalDependency></additionalDependencies>"
        "</configuration></plugin></plugins></pluginManagement>"
        "</build></profile></profiles></project>"
        % ("<spark.version>3.0.0</spark.version>" + root_properties, extra, release_properties, settings)
    )


class JavadocVisibilityTest(unittest.TestCase):
    def check_fixture(self, show):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pom.xml"
            path.write_text(fixture(show), encoding="utf-8")
            return visibility_problems(path)

    def test_repository_release_visibility_is_public_api_only(self):
        self.assertEqual([], visibility_problems(ROOT / "pom.xml"))

    def test_repository_release_gate_is_strict_and_suppresses_authors(self):
        self.assertEqual([], release_gate_problems(ROOT / "pom.xml"))

    def test_legacy_plugin_or_missing_direct_spark_dependency_is_rejected(self):
        safe = ("<doclint>all</doclint><failOnError>true</failOnError>"
                "<validateLinks>true</validateLinks><author>false</author>")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pom.xml"
            path.write_text(gate_fixture(safe).replace("<version>3.12.0</version>",
                                                       "<version>3.1.1</version>"), encoding="utf-8")
            self.assertIn("maven-javadoc-plugin 3.12.0 or newer is required", release_gate_problems(path))
            path.write_text(gate_fixture(safe).replace(
                "<additionalDependency><groupId>org.apache.spark</groupId>"
                "<artifactId>spark-sql_2.12</artifactId><version>${spark.version}</version>"
                "</additionalDependency>", ""), encoding="utf-8")
            self.assertIn("direct Spark aggregate-classpath dependencies are required",
                          release_gate_problems(path))

    def test_permissive_doclint_and_author_output_are_rejected(self):
        cases = (
            ("<doclint>none</doclint><failOnError>true</failOnError>"
             "<validateLinks>true</validateLinks><author>false</author>", "strict doclint is required"),
            ("<doclint>all</doclint><failOnError>false</failOnError>"
             "<validateLinks>true</validateLinks><author>false</author>", "failOnError must be enabled"),
            ("<doclint>all</doclint><failOnError>true</failOnError>"
             "<validateLinks>false</validateLinks><author>false</author>", "link validation must be enabled"),
            ("<doclint>all</doclint><failOnError>true</failOnError>"
             "<validateLinks>true</validateLinks><author>true</author>", "author output must be explicitly disabled"),
        )
        for settings, expected in cases:
            with self.subTest(expected=expected), tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / "pom.xml"
                path.write_text(gate_fixture(settings), encoding="utf-8")
                self.assertIn(expected, release_gate_problems(path))

    def test_properties_resolve_with_release_overlay(self):
        settings = ("<doclint>${gate.doclint}</doclint><failOnError>${gate.fail}</failOnError>"
                    "<validateLinks>${gate.links}</validateLinks><author>${gate.author}</author>")
        root_props = ("<gate.doclint>none</gate.doclint><gate.fail>false</gate.fail>"
                      "<gate.links>false</gate.links><gate.author>true</gate.author>")
        release_props = ("<gate.doclint>all</gate.doclint><gate.fail>true</gate.fail>"
                         "<gate.links>true</gate.links><gate.author>false</gate.author>")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pom.xml"
            path.write_text(gate_fixture(settings, root_props, release_props), encoding="utf-8")
            self.assertEqual([], release_gate_problems(path))

    def test_root_properties_work_but_missing_or_decoy_properties_fail(self):
        settings = ("<doclint>${gate.doclint}</doclint><failOnError>${gate.fail}</failOnError>"
                    "<validateLinks>${gate.links}</validateLinks><author>${gate.author}</author>")
        good = ("<gate.doclint>all,-missing</gate.doclint><gate.fail>true</gate.fail>"
                "<gate.links>true</gate.links><gate.author>false</gate.author>")
        decoy = good.replace("gate.author", "gate.author.decoy")
        for properties, expected in ((good, None), (decoy, "author output must be explicitly disabled"),
                                     ("", "strict doclint is required")):
            with self.subTest(expected=expected), tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / "pom.xml"
                path.write_text(gate_fixture(settings, properties), encoding="utf-8")
                problems = release_gate_problems(path)
                self.assertEqual([], problems) if expected is None else self.assertIn(expected, problems)

    def test_active_plugin_and_execution_overrides_cannot_weaken_gate(self):
        safe = ("<doclint>all</doclint><failOnError>true</failOnError>"
                "<validateLinks>true</validateLinks><author>false</author>")
        layers = (
            ("root plugin", "<build><plugins><plugin><artifactId>maven-javadoc-plugin</artifactId>"
             "<configuration><doclint>none</doclint></configuration></plugin></plugins></build>"),
            ("root execution", "<build><plugins><plugin><artifactId>maven-javadoc-plugin</artifactId>"
             "<executions><execution><configuration><author>true</author></configuration></execution>"
             "</executions></plugin></plugins></build>"),
            ("release plugin", "<plugins><plugin><artifactId>maven-javadoc-plugin</artifactId>"
             "<configuration><failOnError>false</failOnError></configuration></plugin></plugins>"),
            ("release execution", "<plugins><plugin><artifactId>maven-javadoc-plugin</artifactId>"
             "<executions><execution><configuration><validateLinks>false</validateLinks></configuration>"
             "</execution></executions></plugin></plugins>"),
        )
        for label, override in layers:
            with self.subTest(layer=label), tempfile.TemporaryDirectory() as directory:
                path = Path(directory) / "pom.xml"
                body = gate_fixture(safe)
                if label.startswith("release"):
                    body = body.replace("</build></profile>", override + "</build></profile>")
                else:
                    body = body.replace("<profiles>", override + "<profiles>")
                path.write_text(body, encoding="utf-8")
                self.assertTrue(release_gate_problems(path))

    def test_inactive_wrong_group_and_comments_do_not_poison_gate(self):
        safe = ("<doclint>all</doclint><failOnError>true</failOnError>"
                "<validateLinks>true</validateLinks><author>false</author>")
        decoys = ("<!-- <author>true</author> -->"
                  "<build><plugins><plugin><groupId>example.invalid</groupId>"
                  "<artifactId>maven-javadoc-plugin</artifactId><configuration><doclint>none</doclint>"
                  "</configuration></plugin></plugins></build>")
        inactive = ("<profile><id>inactive</id><build><plugins><plugin>"
                    "<artifactId>maven-javadoc-plugin</artifactId><configuration><author>true</author>"
                    "</configuration></plugin></plugins></build></profile>")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pom.xml"
            body = gate_fixture(safe, extra=decoys).replace("<profiles>", "<profiles>" + inactive)
            path.write_text(body, encoding="utf-8")
            self.assertEqual([], release_gate_problems(path))

    def test_public_and_protected_are_accepted(self):
        self.assertEqual([], self.check_fixture("public"))
        self.assertEqual([], self.check_fixture("protected"))

    def test_private_package_unknown_and_missing_are_rejected(self):
        for value in ("private", "package", "internal", None):
            with self.subTest(value=value):
                self.assertTrue(self.check_fixture(value))

    def test_comment_or_other_plugin_cannot_satisfy_the_guard(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pom.xml"
            path.write_text(
                "<project><!-- <show>protected</show> --><build><plugins><plugin>"
                "<artifactId>not-javadoc</artifactId><configuration><show>protected</show></configuration>"
                "</plugin></plugins></build></project>",
                encoding="utf-8",
            )
            self.assertTrue(visibility_problems(path))

    def test_inactive_profile_and_wrong_group_cannot_satisfy_release(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pom.xml"
            path.write_text(
                "<project><profiles>"
                "<profile><id>inactive</id><build><pluginManagement><plugins><plugin>"
                "<groupId>org.apache.maven.plugins</groupId><artifactId>maven-javadoc-plugin</artifactId>"
                "<configuration><show>protected</show></configuration>"
                "</plugin></plugins></pluginManagement></build></profile>"
                "<profile><id>release-profile</id><build><pluginManagement><plugins><plugin>"
                "<groupId>example.invalid</groupId><artifactId>maven-javadoc-plugin</artifactId>"
                "<configuration><show>protected</show></configuration>"
                "</plugin></plugins></pluginManagement></build></profile>"
                "</profiles></project>",
                encoding="utf-8",
            )
            self.assertTrue(visibility_problems(path))

    def test_execution_override_cannot_broaden_visibility(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pom.xml"
            body = fixture("protected").replace(
                "</plugin>",
                "<executions><execution><configuration><show>private</show></configuration>"
                "</execution></executions></plugin>",
            )
            path.write_text(body, encoding="utf-8")
            self.assertIn("non-public Javadoc visibility: private", visibility_problems(path))

    def test_root_execution_override_cannot_broaden_release_visibility(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pom.xml"
            body = fixture("protected").replace(
                "</project>",
                "<build><plugins><plugin><groupId>org.apache.maven.plugins</groupId>"
                "<artifactId>maven-javadoc-plugin</artifactId><executions><execution>"
                "<configuration><show>private</show></configuration>"
                "</execution></executions></plugin></plugins></build></project>",
            )
            path.write_text(body, encoding="utf-8")
            self.assertIn("non-public Javadoc visibility: private", visibility_problems(path))


if __name__ == "__main__":
    unittest.main()
