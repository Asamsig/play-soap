/*
 * Copyright © 2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form or
 * by any means without the express written permission of Typesafe, Inc.
 */

lazy val root = (project in file("."))
  .enablePlugins(NoPublish)
  .aggregate(client)

lazy val client = (project in file("client"))

lazy val plugin = (project in file("sbt-plugin"))
  .settings(scriptedSettings: _*)
  .settings(
    (resourceGenerators in Compile) <+= generateVersionFile,
    scriptedDependencies := {
      val () = publishLocal.value
      val () = (publishLocal in client).value
    },
    scriptedTask := scripted.toTask("").value,
    publishMavenStyle := false,
    publishTo := Some(Resolver.url("typesafe-rp",
        url(s"https://api.bintray.com/content/typesafe/for-subscribers-only/play-soap/${version.value}/AEE4D829FC38A3247F251ED25BA45ADD675D48EB/"))(Resolver.ivyStylePatterns))
  )

lazy val docs = (project in file("docs"))
  .enablePlugins(SbtTwirl)
  .enablePlugins(SbtWeb)
  .enablePlugins(NoPublish)
  .settings(
    WebKeys.pipeline ++= {
      val clientDocs = (mappings in (Compile, packageDoc) in client).value.map {
        case (file, name) => file -> ("api/client/" + name)
      }
      val pluginDocs = (mappings in (Compile, packageDoc) in plugin).value.map {
        case (file, name) => file -> ("api/sbtwsdl/" + name)
      }
      clientDocs ++ pluginDocs
    }
  )

def generateVersionFile = Def.task {
  val clientVersion = (version in client).value
  val pluginVersion = version.value
  val file = (resourceManaged in Compile).value / "play-soap.version.properties"
  val content =
    s"""play-soap-client.version=$clientVersion
       |play-soap-sbt.version=$pluginVersion
     """.stripMargin
  if (!file.exists() || !(IO.read(file) == content)) {
    IO.write(file, content)
  }
  Seq(file)
}

lazy val scriptedTask = TaskKey[Unit]("scripted-task")
