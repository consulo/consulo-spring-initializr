#!/usr/bin/env python3
# Copyright 2013-2026 consulo.io
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""
Fetches metadata from start.spring.io and Maven Central.
Generates:
  - plugin/src/main/dist/spring-initializr-metadata.json         (structure + localize IDs, read by the plugin and the runner)
  - plugin/src/main/resources/LOCALIZE-LIB/en_US/consulo.spring.initializr.SpringInitializrMetadataLocalize.yaml (English strings)

Only the GA Spring Boot versions offered by start.spring.io are exported. Dependency coordinates, BOM versions and the
Kotlin version are resolved for each of them, so offline generation matches start.spring.io for every exported version.

Usage: python3 update-metadata.py
"""

import json
import re
import urllib.request
import os


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PLUGIN_DIST_DIR = os.path.join(SCRIPT_DIR, "plugin", "src", "main", "dist")
LOCALIZE_DIR = os.path.join(SCRIPT_DIR, "plugin", "src", "main", "resources",
                            "LOCALIZE-LIB", "en_US")


def fetch_text(url, headers=None):
    req = urllib.request.Request(url)
    if headers:
        for k, v in headers.items():
            req.add_header(k, v)
    with urllib.request.urlopen(req) as resp:
        return resp.read().decode("utf-8")


def fetch_json(url, headers=None):
    return json.loads(fetch_text(url, headers))


def fetch_metadata():
    return fetch_json("https://start.spring.io", {"Accept": "application/json"})


def fetch_resolved_dependencies(boot_version):
    return fetch_json(f"https://start.spring.io/dependencies?bootVersion={boot_version}")


def fetch_kotlin_version(boot_version):
    """Kotlin version managed by the Spring Boot BOM of the given version."""
    pom = fetch_text("https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/"
                     f"{boot_version}/spring-boot-dependencies-{boot_version}.pom")
    match = re.search(r"<kotlin\.version>([^<]+)</kotlin\.version>", pom)
    return match.group(1) if match else None


def to_key(s):
    """Convert a string to a localization key segment: lowercase, replace non-alnum with dots."""
    return re.sub(r'[^a-z0-9]+', '.', s.lower()).strip('.')


def to_ga_version(version_id):
    """'4.1.1.RELEASE' or '4.1.1' -> '4.1.1'; snapshots and milestones -> None."""
    if version_id.endswith(".RELEASE"):
        version_id = version_id[:-len(".RELEASE")]
    return version_id if re.fullmatch(r"\d+\.\d+\.\d+", version_id) else None


def exact_range(version):
    return f"[{version},{version}]"


def single_select(metadata, key, localize, loc_prefix, name_of):
    data = metadata.get(key, {})
    items = []
    for value in data.get("values", []):
        item_id = value["id"]
        loc_id = f"{loc_prefix}.{to_key(item_id)}.name"
        localize[loc_id] = name_of(value)
        items.append({
            "id": item_id,
            "nameId": loc_id,
            "default": item_id == data.get("default", "")
        })
    return items


def build_dependency(dep, boot_versions, default_version, resolved):
    """Dependency entry with coordinates resolved per Boot version, or None if no exported version supports it."""
    dep_id = dep["id"]
    per_version = {v: resolved[v]["dependencies"][dep_id]
                   for v in boot_versions if dep_id in resolved[v].get("dependencies", {})}
    if not per_version:
        return None

    compatible = list(per_version.keys())
    base = per_version.get(default_version) or per_version[compatible[0]]

    entry = {
        "id": dep_id,
        "compatibleBootVersions": compatible,
        "groupId": base["groupId"],
        "artifactId": base["artifactId"],
    }
    if dep.get("versionRange"):
        entry["compatibilityRange"] = dep["versionRange"]
    if base.get("scope"):
        entry["scope"] = base["scope"]
    if base.get("bom"):
        entry["bom"] = base["bom"]
    if base.get("repository"):
        entry["repository"] = base["repository"]

    # the top level carries no version: a mapping without a version must fall back to the Boot managed one
    mappings = []
    for version, coordinates in per_version.items():
        mapping = {}
        for key in ("groupId", "artifactId", "bom", "repository"):
            if coordinates.get(key) and coordinates.get(key) != base.get(key):
                mapping[key] = coordinates[key]
        if coordinates.get("version"):
            mapping["version"] = coordinates["version"]
        if mapping:
            mapping["compatibilityRange"] = exact_range(version)
            mappings.append(mapping)
    if mappings:
        entry["mappings"] = mappings
    return entry


def build_env(boot_versions, default_version, resolved, kotlin_versions):
    boms = {}
    repositories = {}
    for version in boot_versions:
        for bom_id, bom in resolved[version].get("boms", {}).items():
            target = boms.setdefault(bom_id, {"groupId": bom["groupId"], "artifactId": bom["artifactId"], "mappings": []})
            mapping = {"compatibilityRange": exact_range(version), "version": bom["version"]}
            for key in ("groupId", "artifactId"):
                if bom[key] != target[key]:
                    mapping[key] = bom[key]
            if bom.get("repositories"):
                mapping["repositories"] = bom["repositories"]
            target["mappings"].append(mapping)

        for repo_id, repo in resolved[version].get("repositories", {}).items():
            repositories[repo_id] = {
                "name": repo["name"],
                "url": repo["url"],
                "snapshotsEnabled": repo.get("snapshotEnabled", False)
            }

    kotlin_mappings = [{"compatibilityRange": exact_range(v), "version": kotlin_versions[v]}
                       for v in boot_versions if kotlin_versions.get(v)]

    env = {"boms": boms, "repositories": repositories}
    if kotlin_mappings:
        env["kotlin"] = {
            "defaultVersion": kotlin_versions.get(default_version) or kotlin_mappings[0]["version"],
            "mappings": kotlin_mappings
        }
    return env


def build_metadata(metadata, boot_versions, default_version, resolved, kotlin_versions):
    """Build JSON and collect localization entries."""
    result = {}
    localize = {}

    result["bootVersions"] = []
    for version in boot_versions:
        loc_id = f"boot.version.{to_key(version)}.name"
        localize[loc_id] = version
        result["bootVersions"].append({
            "id": version,
            "nameId": loc_id,
            "default": version == default_version
        })

    result["javaVersions"] = single_select(metadata, "javaVersion", localize, "java.version", lambda v: v["id"])
    result["languages"] = single_select(metadata, "language", localize, "language", lambda v: v["name"])
    result["packagings"] = single_select(metadata, "packaging", localize, "packaging", lambda v: v["name"])

    # only full projects - 'maven-build'/'gradle-build' download a lone build file on start.spring.io
    type_data = metadata.get("type", {})
    result["types"] = []
    for t in type_data.get("values", []):
        if t.get("tags", {}).get("format") != "project":
            continue
        tid = t["id"]
        loc_id = f"type.{to_key(tid)}.name"
        localize[loc_id] = t["name"]
        result["types"].append({
            "id": tid,
            "nameId": loc_id,
            "default": tid == type_data.get("default", ""),
            "tags": t["tags"]
        })

    result["dependencies"] = []
    for group in metadata.get("dependencies", {}).get("values", []):
        deps = []
        for dep in group.get("values", []):
            entry = build_dependency(dep, boot_versions, default_version, resolved)
            if entry is None:
                continue

            dep_key = to_key(dep["id"])
            name_loc_id = f"dependency.{dep_key}.name"
            desc_loc_id = f"dependency.{dep_key}.description"
            localize[name_loc_id] = dep["name"]
            entry["nameId"] = name_loc_id
            if dep.get("description"):
                localize[desc_loc_id] = dep["description"]
                entry["descriptionId"] = desc_loc_id
            deps.append(entry)

        if not deps:
            continue

        group_loc_id = f"dependency.group.{to_key(group['name'])}.name"
        localize[group_loc_id] = group["name"]
        result["dependencies"].append({
            "nameId": group_loc_id,
            "content": deps
        })

    result["env"] = build_env(boot_versions, default_version, resolved, kotlin_versions)
    return result, localize


def yaml_key(s):
    """Quote YAML key if it could be misinterpreted (contains numeric-like segments)."""
    if any(seg.replace('.', '').isdigit() for seg in s.split('.')):
        return f'"{s}"'
    return s


def yaml_text_inner(s):
    """Escape text for use inside single quotes."""
    if s is None:
        return ""
    return s.replace("'", "''")


def write_localize_yaml(localize, path):
    with open(path, "w") as f:
        for key in sorted(localize.keys()):
            f.write(f"{yaml_key(key)}:\n")
            f.write(f"    text: '{yaml_text_inner(localize[key])}'\n")


def main():
    print("Fetching metadata from start.spring.io...")
    metadata = fetch_metadata()

    boot_versions = [v for v in (to_ga_version(b["id"]) for b in metadata["bootVersion"]["values"]) if v]
    if not boot_versions:
        raise SystemExit("start.spring.io offers no GA Spring Boot version")
    default_version = to_ga_version(metadata["bootVersion"].get("default", ""))
    if default_version not in boot_versions:
        default_version = boot_versions[0]
    print(f"GA Spring Boot versions: {', '.join(boot_versions)} (default {default_version})")

    resolved = {}
    kotlin_versions = {}
    for version in boot_versions:
        print(f"Fetching resolved dependencies for Boot {version}...")
        resolved[version] = fetch_resolved_dependencies(version)
        print(f"Fetching Kotlin version managed by Boot {version}...")
        kotlin_versions[version] = fetch_kotlin_version(version)

    metadata_json, localize = build_metadata(metadata, boot_versions, default_version, resolved, kotlin_versions)

    json_path = os.path.join(PLUGIN_DIST_DIR, "spring-initializr-metadata.json")
    print(f"Writing {json_path}...")
    with open(json_path, "w") as f:
        json.dump(metadata_json, f, indent=2)
        f.write("\n")

    yaml_path = os.path.join(LOCALIZE_DIR,
                             "consulo.spring.initializr.SpringInitializrMetadataLocalize.yaml")
    print(f"Writing {yaml_path} ({len(localize)} entries)...")
    write_localize_yaml(localize, yaml_path)

    print("Done.")


if __name__ == "__main__":
    main()
