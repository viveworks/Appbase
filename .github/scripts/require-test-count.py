#!/usr/bin/env python3
"""Fail unless JUnit/Surefire XML files report at least one executed test."""

# @author Navaneeth Mysore G
# @author Claude

import glob
import sys
import xml.etree.ElementTree as ET

reports = [path for pattern in sys.argv[1:] for path in glob.glob(pattern, recursive=True)]
total = 0
for report in reports:
    root = ET.parse(report).getroot()
    # Count testcase elements rather than suite totals: aggregate testsuites repeat their
    # children's totals, while a skipped testcase did not execute any test body.
    for testcase in root.iter():
        if testcase.tag.rsplit("}", 1)[-1] != "testcase":
            continue
        skipped = any(child.tag.rsplit("}", 1)[-1] == "skipped" for child in testcase)
        if not skipped:
            total += 1
if not reports or total == 0:
    print("::error::selected test tier executed zero tests")
    raise SystemExit(1)
print(f"tests executed: {total}")
