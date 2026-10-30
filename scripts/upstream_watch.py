"""Report what changed upstream since the last review: new commits, releases and published security advisories.

Usage:
  python scripts/upstream_watch.py                  print a Markdown report
  python scripts/upstream_watch.py --out report.md  also write it to a file
  python scripts/upstream_watch.py --mark-reviewed  set every source's reviewed commit to its current head (run after a review)

Reads scripts/upstream_sources.json. Uses GITHUB_TOKEN or GH_TOKEN when set, which raises the GitHub API rate limit.
"""
import argparse
import datetime
import json
import os
import re
import sys
import urllib.error
import urllib.request

SOURCES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "upstream_sources.json")
API = "https://api.github.com"
# Words in commit titles that usually mean a fix worth a closer look
FIX_WORDS = ("fix", "crash", "anr", "leak", "security", "harden", "overflow", "cve", "vulnerab", "hang", "regression")


def api(path):
    request = urllib.request.Request(API + path, headers={"Accept": "application/vnd.github+json", "User-Agent": "super-artemis-upstream-watch"})
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if token:
        request.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def head(source):
    return api("/repos/%s/commits/%s" % (source["repo"], source["branch"]))["sha"]


def new_commits(source):
    compare = api("/repos/%s/compare/%s...%s" % (source["repo"], source["reviewed"], source["branch"]))
    return compare.get("ahead_by", 0), compare.get("commits", [])


def new_releases(source):
    since = source["reviewedOn"]
    releases = api("/repos/%s/releases?per_page=10" % source["repo"])
    return [r for r in releases if (r.get("published_at") or "") >= since]


def advisories(source):
    try:
        found = api("/repos/%s/security-advisories?state=published&per_page=20" % source["repo"])
    except urllib.error.HTTPError:
        return []
    since = source["reviewedOn"]
    return [a for a in found if (a.get("published_at") or "") >= since]


def report(sources):
    lines = ["# Upstream watch", "", "Generated %s UTC. Review each item, port what fits with credit (CONTRIBUTING.md), then update `reviewed` in `scripts/upstream_sources.json` (see UPSTREAM.md)." % datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M"), ""]
    total = 0
    for source in sources:
        title = "## %s (%s, %s)" % (source["repo"], source["branch"], source["role"])
        try:
            ahead, commits = new_commits(source)
            releases = new_releases(source)
            found = advisories(source)
        except (urllib.error.HTTPError, urllib.error.URLError) as e:
            code = e.code if hasattr(e, "code") else "network error"
            lines += [title, "", "Could not read this repository: %s." % code, ""]
            continue
        if not ahead and not releases and not found:
            continue
        total += ahead + len(releases) + len(found)
        lines += [title, ""]
        for advisory in found:
            lines.append("- **Security advisory** [%s](%s) %s" % (advisory.get("ghsa_id"), advisory.get("html_url"), advisory.get("summary", "")))
        for release in releases:
            lines.append("- **Release** [%s](%s)" % (release.get("tag_name"), release.get("html_url")))
        if ahead:
            lines.append("- **%d new commits** since `%s`: [compare](https://github.com/%s/compare/%s...%s)" % (ahead, source["reviewed"], source["repo"], source["reviewed"], source["branch"]))
            # Translation updates are counted, not listed, so code changes stay visible
            translations = [c for c in commits if c["commit"]["message"].startswith("Translated using Weblate")]
            if translations:
                lines.append("  - %d translation updates (Weblate) not listed" % len(translations))
            for commit in [c for c in commits if c not in translations][-30:]:
                message = commit["commit"]["message"].split("\n")[0]
                flag = " **(fix?)**" if any(w in message.lower() for w in FIX_WORDS) else ""
                author = (commit.get("author") or {}).get("login") or commit["commit"]["author"]["name"]
                lines.append("  - [`%s`](%s) %s, by %s%s" % (commit["sha"][:9], commit["html_url"], message, author, flag))
        lines.append("")
    if total == 0:
        lines.append("Nothing new since the last review.")
    report_text = "\n".join(lines) + "\n"
    # Defang any @ mentions so weekly tracking issues never trigger user notifications or emails
    report_text = re.sub(r"@([a-zA-Z0-9_\-]+)", r"\1", report_text)
    return report_text, total


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", help="also write the report to this file")
    parser.add_argument("--mark-reviewed", action="store_true", help="record every source's current head as reviewed")
    args = parser.parse_args()
    # Commit titles can be in any language, and Windows consoles default to a legacy code page
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    with open(SOURCES, encoding="utf-8") as f:
        config = json.load(f)

    if args.mark_reviewed:
        today = datetime.date.today().isoformat()
        marked = 0
        for source in config["sources"]:
            try:
                source["reviewed"] = head(source)[:12]
                source["reviewedOn"] = today
                marked += 1
            except urllib.error.HTTPError as e:
                print(f"Skipping {source['repo']}: HTTP {e.code}")
        with open(SOURCES, "w", encoding="utf-8", newline="\n") as f:
            json.dump(config, f, indent=2)
            f.write("\n")
        print("Marked %d sources as reviewed on %s" % (marked, today))
        return 0

    text, total = report(config["sources"])
    if args.out:
        with open(args.out, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
    sys.stdout.write(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
