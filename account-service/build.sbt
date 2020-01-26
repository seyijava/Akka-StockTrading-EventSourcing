name := "account-service"
scalaVersion := "2.12.8"
lazy val akkaVersion = "2.6.0"
lazy val akkaHttpVersion = "10.1.5"
lazy val `account-service` = project

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.18",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % "2.6.0",
  "de.ruedigermoeller" % "fst" % "2.00",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.99" exclude("com.typesafe.akka", "*"),
  "com.google.guava"  % "guava" % "23.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.5",
  "com.datastax.cassandra" % "cassandra-driver-extras" % "3.1.4",
  "com.google.code.gson" % "gson" % "2.8.5",
  "org.pcollections" % "pcollections" % "2.1.2",
  "com.hazelcast" % "hazelcast" % "3.12.2",
  "com.typesafe.akka" %% "akka-remote" % akkaVersion)

version in Docker := "latest"
dockerExposedPorts in Docker := Seq(8000)
dockerRepository := Some("akka-stock-trading-system")
dockerBaseImage := "java"
enablePlugins(JavaAppPackaging)





