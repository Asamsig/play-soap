# Using sbt WSDL

## Installation

To install sbt WSDL into your Play project, add the following dependency to your `project/plugins.sbt`:

```scala
addSbtPlugin("com.typesafe.play" % "play-soap-sbt" % "1.0.0")
```

The plugin is automatically activated on install, nothing further needs to be done.

## Supplying WSDLs

There are two ways to supply WSDLs to the plugin.  One is to configure a URL or set of URLs to point the compiler at.  This can be done by using the `wsdlUrls` setting:

```scala
WsdlKeys.wsdlUrls in Compile += url("http://someservice.com/path?wsdl")
```

Notice that the setting must be in the compile scope - WSDL's can also be compiled just for the `Test` scope too.

The other is to place WSDLs in the `conf/wsdls` directory for a Play project, or in `src/main/wsdl` for an ordinary SBT project.  You can also use both local files and urls.

## Configuring WSDL compilation

### Selecting the future API

sbt WSDL can generate clients to either return `scala.concurrent.Future` for Scala projects, or `play.libs.F.Promise` for Java projects.  If your project is a Play project, the future API will automatically be selected based on whether you enabled the `PlayJava` or `PlayScala` plugin.  If it's an ordinary sbt project, it will default to `scala.concurrent.Future`.  This can be configured to use the Java promise using the `futureApi` setting:

```scala
WsdlKeys.futureApi := WsdlKeys.PlayJavaFutureApi
```

### Configuring the package name

By default, all WSDLs will be generated into a package structure that matches the namespace in the WSDL.  For example, if you have a WSDL that defines a `targetNamespace="http://example.com/"`, the package that the client will be generated into will be `com.example`.

The package can be configured in many ways.  One way is to override it globally, that is, for all WSDLs and namespaces, this can be done using the `packageName` setting:

```scala
WsdlKeys.packageName := Some("com.mycompany.example")
```

Additionally, individual namespaces can be configured using the `packageMappings` setting:

```scala
WsdlKeys.packageMappings += ("http://example.com/" -> "com.mycompany.example")
```

Both `packageName` and `packageMappings` can be used together, `packageMappings` will take precedence over `packageName`.

### Configuring the service name

The service name from the WSDL can be overridden if desired, using the `serviceName` setting:

```scala
WsdlKeys.serviceName := Some("MyService")
```

## The `play.plugins` file

sbt WSDL generates a number of Play plugins.  To register these with Play, it writes them out to a `play.plugins` file.  If your project defines its own `play.plugins` file, this will conflict.  There are two ways to solve this.

### Disabling the generation of `play.plugins`

Disabling the generation of `play.plugins` means you will have to add entries for your SOAP clients to your `play.plugins` file manually.  The disabling can be done using the `playPlugins` setting:

```scala
WsdlKeys.playPlugins in Compile := Nil
```

Then, for each service, you'll need to add an entry to your own plugins file.  For example, if you have a service called `MyService` in the namespace `http://example.com/`, you will need to add the following to your plugins file:

    900:com.example.MyService

### Adding plugins to the `play.plugins` file

You can also instruct sbt WSDL to add plugins to the `play.plugins` file that it generates.  This can be done using the `playPlugins` setting:

```scala
WsdlKeys.playPlugins in Compile += "800:com.example.MyPlugin"
```

## Advanced configuration

### Supplying additional arguments to wsdl2java

sbt WSDL is built on top of the Apache CXF wsdl2java tool.  Many of the options that it supports can also be used when you're using sbt WSDL.  To see a full list of options supported by Apache CXF, run `sbt wsdlHelp`.  These options can then be configured in your build by adding them to the `wsdlArgs` setting.

For example, if you the WSDL contains an operation called `sayHello`, and you want this to be generated using wrapper style (that is, all the arguments to the method are passed using a single object that wraps them), you could add the following configuration:

```scala
WsdlKeys.wsdlToCodeArgs += "-bareMethods=sayHello"
```

### Using different configurations for different WSDL files

In some cases you may want to have completely different configurations for different WSDL files - or you may want to use the same WSDL file to generate two different clients.  This can be done using the `wsdlTasks` task.  By default, this task is built by combining all the settings used above, but if you override it, then the settings above will be ignored.

Each wsdl generation task that is executed is defined by the following case class:

```scala
case class WsdlTask(
  url: URL, 
  futureApi: FutureApi = ScalaFutureApi,
  packageName: Option[String] = None, 
  packageMappings: Map[String, String] = Map.empty,
  serviceName: Option[String] = None,
  args: Seq[String] = Nil
)
```

Let's say you have one WSDL on the filesystem in `src/main/wsdl/foo.wsdl`, and you want it generated into the `com.example.foo` package using the Scala future API, and another WSDL at `http://example.com/bar?wsdl` that you want generated into the `com.example.bar` package using the Java future API, then you could configure that like this:

```scala
WsdlKeys.wsdlTasks in Compile := Seq(
  WsdlKeys.WsdlTask((sourceDirectory.value / "wsdl" / "foo.wsdl").toURI.toURL),
    packageName = Some("com.example.foo")
  ),
  WsdlKeys.WsdlTask(url("http://example.com/bar?wsdl"),
    futureApi = WsdlKeys.PlayJavaFutureApi,
    packageName = Some("com.example.bar")
  )
)
```