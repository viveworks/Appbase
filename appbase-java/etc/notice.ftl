<#--
  NOTICE template for license-maven-plugin (internal work item 1384 / P-025 clause 4).

  Apache-2.0 section 4(d) requires the NOTICE file to carry the attribution notices of the works we
  redistribute. This renders that list from DECLARED POM METADATA — the only machine-readable source
  there is — and states its own limits in the generated text, because a NOTICE that looks complete is
  worse than one that says where it stops.

  🔴 ONE ENTRY PER DEPENDENCY, FROM dependencyMap — NOT licenseMap, AND THE DIFFERENCE IS SUBSTANTIVE.
  licenseMap is keyed by individual licence, so a dual-licensed dependency is emitted under EACH of
  its licences: logback would appear under "Eclipse Public License - v 1.0" AND under "GNU Lesser
  General Public License". Measured on this repo: 689 bullets for 525 dependencies, 57 of them
  repeated. That inflates the count and, far worse, renders a CHOICE as though it were a constraint —
  a reader scanning the LGPL heading would find dependencies that are not LGPL-encumbered at all.
  dependencyMap emits each dependency once and shows its options joined by "or", which is what the
  licence text actually grants.

  Context provided by the plugin:
    dependencyMap — Map.Entry<MavenProject, String[]>  (dependency -> its declared licences)
    licenseMap    — Map.Entry<String[], MavenProject>  (licence -> the dependencies under it)
-->
<#function gav p>
    <#return p.groupId + ":" + p.artifactId + ":" + p.version>
</#function>
<#function licenceList p licenses>
    <#-- These three legacy POMs omit usable licence metadata.  The coordinate-specific
         classifications are owner-ratified and backed by the upstream artifacts recorded in the
         N05 quality ledger.  Keep the match exact: it must never classify another version. -->
    <#if gav(p) == "commons-beanutils:commons-beanutils:1.7.0">
        <#return "Apache License 2.0 (Apache-2.0)">
    <#elseif gav(p) == "javax.servlet.jsp:jsp-api:2.1">
        <#return "CDDL 1.0 or GPLv2 with Classpath Exception (CDDL-1.0 OR GPL-2.0-only WITH Classpath-exception-2.0)">
    <#elseif gav(p) == "oro:oro:2.0.8">
        <#return "Apache Software License 1.1 (Apache-1.1)">
    </#if>
    <#assign out = ""/>
    <#list licenses as l>
        <#if out == "">
            <#assign out = l/>
        <#else>
            <#assign out = out + " or " + l/>
        </#if>
    </#list>
    <#return out>
</#function>
<#function artifactFormat p>
    <#if p.name?index_of('Unnamed') &gt; -1>
        <#return p.groupId + ":" + p.artifactId + ":" + p.version>
    <#else>
        <#return p.name + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + ")">
    </#if>
</#function>
Appbase
Copyright 2019 The original author or authors of Appbase

This product includes software developed by the author or authors of
Appbase (https://github.com/viveworks/Appbase).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at http://www.apache.org/licenses/LICENSE-2.0

================================================================================
THIRD-PARTY DEPENDENCIES
================================================================================

<#if dependencyMap?size == 0>
This product redistributes no third-party software.
<#else>
This product redistributes the ${dependencyMap?size} third-party works listed below, on a compile or
runtime path. Test- and provided-scope dependencies are excluded: they are not
redistributed, so Apache-2.0 section 4(d) attribution does not attach to them.

HOW THIS LIST IS PRODUCED, AND WHAT IT DOES NOT SAY

  - It is GENERATED. Do not edit it by hand -- run the command in the project's
    build documentation and commit the result. A check fails the build when this
    file no longer matches the dependencies actually resolved. Every dependency
    entry below begins with "  * "; the three notes in this section do not.

  - Each licence below is the one THAT DEPENDENCY'S OWN POM DECLARES, except for
    three exact-coordinate legacy overrides whose POMs omit usable metadata. Those
    overrides, the five exact-coordinate LGPL-as-used notices, and their
    upstream-artifact provenance are recorded in the N05 quality ledger. POM metadata is
    publisher metadata, not a reading of the licence text, and not a check
    against the upstream project. Where an entry reads "A or B" the dependency
    offers a CHOICE of licences; this file records no election between them.

  - It is ATTRIBUTION, NOT CLEARANCE. Listing a dependency here records that we
    redistribute it. It does not record that anyone has approved, accepted or
    cleared its licence position. Some entries carry open licence questions;
    those are tracked with their evidence in
    docs/spec/1.0.0/P05/decisions/E02-dependency-licences.md, and appearing in
    this file neither answers them nor implies they are answered.

--------------------------------------------------------------------------------

    <#list dependencyMap as e>
        <#assign project = e.getKey()/>
        <#assign licenses = e.getValue()/>
  * ${artifactFormat(project)}
      ${licenceList(project, licenses)}
        <#if gav(project) == "commons-beanutils:commons-beanutils:1.7.0">
      Upstream NOTICE: This product includes software developed by
      The Apache Software Foundation (http://www.apache.org/).
        <#elseif gav(project) == "javax.servlet.jsp:jsp-api:2.1">
      Upstream source attribution: Copyright 2005 Sun Microsystems, Inc. All rights reserved.
      Portions Copyright Apache Software Foundation.
        <#elseif gav(project) == "oro:oro:2.0.8">
      Upstream attribution: Copyright (c) 2000-2002 The Apache Software Foundation.
      This product includes software developed by the Apache Software Foundation
      (http://www.apache.org/).
        <#elseif gav(project) == "com.github.spotbugs:spotbugs-annotations:4.10.3">
      LGPL-as-used attribution: redistributed unmodified as a separate JAR under
      LGPL-2.1-only. Corresponding source:
      https://repo1.maven.org/maven2/com/github/spotbugs/spotbugs-annotations/4.10.3/spotbugs-annotations-4.10.3-sources.jar
        <#elseif gav(project) == "net.openhft:affinity:3.0.6">
      LGPL-as-used attribution: redistributed unmodified as a separate JAR under
      LGPL-3.0-only. Corresponding source:
      https://repo1.maven.org/maven2/net/openhft/affinity/3.0.6/affinity-3.0.6-sources.jar
        <#elseif gav(project) == "org.hibernate.common:hibernate-commons-annotations:6.0.6.Final">
      LGPL-as-used attribution: redistributed unmodified as a separate JAR under
      LGPL-2.1-or-later. Corresponding source:
      https://repo1.maven.org/maven2/org/hibernate/common/hibernate-commons-annotations/6.0.6.Final/hibernate-commons-annotations-6.0.6.Final-sources.jar
        <#elseif gav(project) == "org.hibernate.orm:hibernate-core:6.5.3.Final">
      LGPL-as-used attribution: redistributed unmodified as a separate JAR under
      LGPL-2.1-or-later. Corresponding source:
      https://repo1.maven.org/maven2/org/hibernate/orm/hibernate-core/6.5.3.Final/hibernate-core-6.5.3.Final-sources.jar
        <#elseif gav(project) == "org.mariadb.jdbc:mariadb-java-client:2.7.4">
      LGPL-as-used attribution: redistributed unmodified as a separate JAR under
      LGPL-2.1-only. Corresponding source:
      https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/2.7.4/mariadb-java-client-2.7.4-sources.jar
        </#if>
    </#list>
</#if>
