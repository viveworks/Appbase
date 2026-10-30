#!/usr/bin/awk -f
# ---------------------------------------------------------------------------
# classify-exs-inventory.awk
#
# Deterministic classifier for the machine-derived EXS_EXCEPTION_SOFTENING_NO_
# CONSTRAINTS occurrence inventory (config/spotbugs/exs-inventory.tsv).
#
# Purpose: retain a per-occurrence classification that justifies the
# detector-wide suppression in config/spotbugs/exclude.xml (N03 internal work item 1205 / S01
# internal work item 1437). Reads the raw inventory (class<TAB>method<TAB>signature<TAB>
# longmessage) and emits the same rows plus three derived, machine-computed
# columns: layer, kind, surface_class. The surface_class keys map to the
# recorded rationale in config/spotbugs/EXS-suppression-inventory.md.
#
# Reproducible: the classification is a pure function of (class, method), so a
# reviewer regenerates the classified table with
#     awk -f config/spotbugs/classify-exs-inventory.awk config/spotbugs/exs-inventory.tsv
# and gets config/spotbugs/exs-inventory-classified.tsv byte-for-byte.
#
# NOTE ON CONSTRUCTORS: a boundary-constructor surface is softened by a
# deliberate design contract (the platform's service-construction/DI surface
# declares and handles no checked exception, so initialisation failures are
# wrapped as the platform's unchecked AppbaseRuntimeException). It is NOT because
# a Java constructor is unable to declare `throws` — it can. See the
# `boundary-constructor` rationale in the companion markdown
# (EXS-suppression-inventory.md).
#
# @author Navaneeth Mysore G
# @author Claude
# ---------------------------------------------------------------------------
BEGIN { FS = "\t"; OFS = "\t" }

NR == 1 && $1 == "class" {
    print $1, $2, $3, "layer", "kind", "surface_class", $4
    next
}

{
    cls = $1
    mth = $2

    # --- layer (package-derived) ---
    if (cls ~ /\.service\.web\./)                                 layer = "web-service"
    else if (cls ~ /^com\.viveworks\.appbase\.client\.service\./) layer = "client-integration-svc"
    else if (cls ~ /^com\.viveworks\.appbase\.client\.(core|cloud|util)/) layer = "client-core-cloud-util"
    else if (cls ~ /^com\.viveworks\.appbase\.core\./)            layer = "core"
    else if (cls ~ /^com\.viveworks\.appbase\.service\./)         layer = "service-util"
    else                                                          layer = "UNKNOWN"

    # --- kind ---
    kind = (mth == "<init>") ? "constructor" : "method"

    # --- surface_class (first match wins) ---
    if (mth == "<init>")                                          surface = "boundary-constructor"
    else if (cls ~ /\.service\.web\.endpoint\.jaxrs\./)           surface = "jaxrs-resource-method"
    else if (cls ~ /\.service\.web\.oauth\./)                     surface = "web-oauth-crypto-codec"
    else if (cls ~ /^com\.viveworks\.appbase\.client\.service\./) surface = "client-integration-service-method"
    else if (cls ~ /^com\.viveworks\.appbase\.client\.(core|cloud|util)/) surface = "client-core-cloud-util-method"
    else if (cls ~ /^com\.viveworks\.appbase\.core\./)            surface = "core-service-or-util-method"
    else if (cls ~ /^com\.viveworks\.appbase\.service\./)         surface = "service-util-method"
    else                                                          surface = "UNCLASSIFIED"

    print $1, $2, $3, layer, kind, surface, $4
}
