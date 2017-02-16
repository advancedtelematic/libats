#  Ats scala lib

This library provides everything an ats service needs to run a simple
akka http server with a sql database.

## Usage

Add to build.sbt:

    libraryDependencies += "com.advancedtelematic" %% "libtuf" % "version"
    
Check the [blueprint](https://github.com/advancedtelematic/service-blueprint) for more details.


## Development

It's useful to add this project as a local projects, so you can
develop without having to run `publishLocal` all the time.

You can add the following to build.sbt:

    lazy val libats = (ProjectRef(file("<absolute local path to this repo>"), "libats"))

And then add `.dependsOn(libats)` to your project definition, for example:

    lazy val libtuf = (project in file("libtuf")).dependsOn(libats)
    
## Publish

A new version is published to nexus for each push to master.

