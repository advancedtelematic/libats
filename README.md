#  Ats scala lib

This library provides everything an ats service needs to run a simple
akka http server with a sql database.

## Usage

Choose the modules you need and add them to your `build.sbt`.

    libraryDependencies += "com.advancedtelematic" %% "libats" % "version"
    libraryDependencies += "com.advancedtelematic" %% "libats-http" % "version"
    libraryDependencies += "com.advancedtelematic" %% "libats-auth" % "version"
    libraryDependencies += "com.advancedtelematic" %% "libats-slick" % "version"
    libraryDependencies += "com.advancedtelematic" %% "libats-messaging" % "version"
    libraryDependencies += "com.advancedtelematic" %% "libats-messaging-datatype" % "version"
    
Check the [blueprint](https://github.com/advancedtelematic/service-blueprint) for more details.

### Further documentation

Further documentation is available at the module README:

- [libats-metrics](libats-metrics/README.md)
- [libats-slick](libats-slick/README.adoc)

## Sub-Module Dependencies

![libats dependencies](libats_dependencies.png)

Generated with:

    sbt -no-colors -batch libats_root/moduleDepDot | 
    grep -P -v "^\[.+\]"  | 
    dot -Tpng > libats_dependencies.png

## Development

It's useful to add this project as a local projects, so you can
develop without having to run `publishLocal` all the time.

You can add the following to build.sbt:

    lazy val libats = (ProjectRef(file("<absolute local path to this repo>"), "libats"))
    
    lazy val libats_messaging = (ProjectRef(file("<absolute local path to this repo>"), "libats-messaging"))

And then add `.dependsOn(libats)` to your project definition, for example:

    lazy val libtuf = (project in file("libtuf")).dependsOn(libats).dependsOn(libats_messaging)
    
## Publish

A new version is published to nexus for each push to master.

