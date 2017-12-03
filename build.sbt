name := """labs-play-with-sangria"""
organization := "com.egrajeda"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += "org.sangria-graphql" %% "sangria" % "1.3.2"
libraryDependencies += "org.sangria-graphql" %% "sangria-play-json" % "1.0.4"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.egrajeda.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.egrajeda.binders._"
