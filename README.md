POI Fast Calc
======================

[![Build Status](https://ci.moparisthe.best/job/moparisthebest/job/poi/job/master/badge/icon%3Fstyle=plastic)](https://ci.moparisthe.best/job/moparisthebest/job/poi/job/master/)

A Java library to calculate Excel formulas quickly.

This is a fork of [Apache POI](https://poi.apache.org/) version [3.16](https://github.com/apache/poi/tree/REL_3_16_FINAL) 
that serves simply to calculate formulas quickly, it supports XLSX (Excel 2007) row/column limits in the HSSF engine for 
much faster evaluation than XML-backed XSSF is capable of, with the drawback that it can't read or write XLS/XLSX files 
from or to disk.  Read the [email thread](https://lists.apache.org/thread.html/0bc90a3ed386edddfcb9b93ce6c262ad145a6b0433d0fcfe70ef10a2@%3Cdev.poi.apache.org%3E)
with my original proposed patch to upstream poi for background.

To use, add this to your maven pom.xml:
```xml
<dependency>
  <groupId>com.moparisthebest.poi</groupId>
  <artifactId>poi-fast-calc</artifactId>
  <version>3.16-SNAPSHOT</version>
</dependency>
```

The `org.apache.poi` package has been renamed `com.moparisthebest.poi` and all dependencies removed,
so this can cleanly live aside modern/newer upstream poi forever, and shouldn't ever need to change.
