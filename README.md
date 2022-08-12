# Namespace Export Tool

This tool will export a namespace's objects to a directory with each object type in seperate directories.  For instance, if you specified a namespace with two `Deployments` and two `ConfigMaps` the resulting directory would have two directories: `deployments` and `configmaps`. This tools removes most instance data such as all `status` fields, and `metadata` fields that are specific to the cluster.  Finally, this tool skips `Secrets` (use a vault!), `ReplicaSets`, and `Pods`.  

In order to build this tool, you'll need OpenJDK 11 (or equivalent).

*** This tool is not polished, it is a one-off tool right now and is not designed for external user ***

To use:

```
$ mvn clean package
$ cd target
$ java -jar ./k8s-export-1.0-SNAPSHOT-jar-with-dependencies.jar namespace /path/to/export/directory
```

