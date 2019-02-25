# libats logging support

This module provides default settings for logging in libats
services. Simply include this module in your service. It comes with a configuration file
called `logback-base.xml` which you can include in your service's logback.xml like this:

```xml
<configuration>
    <include resource="logback-libats.xml"/>
</configuration>
```

You can also extend it to a `logback.xml` file after changing the top-level `<included>` element
to `<configuration>`.

Logging can be configured through the `LOG_APPENDER` environment variable.

## Stdout Logging

Set the `LOG_APPENDER` environment variable to `stdout`.

## Json Logging

Set the `LOG_APPENDER` environment variable to `json`.

A different appender can be configured in a `logback.xml` using the
following settings:

```xml
<appender name="my-appender-json" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="com.advancedtelematic.libats.logging.JsonEncoder">
            <includeContext>false</includeContext>
            <includeThread>false</includeThread>
            <includeMdc>false</includeMdc>
            <includeHttpQuery>false</includeHttpQuery>
            <prettyPrint>false</prettyPrint>
            <loggerLength>36</loggerLength>
        </encoder>
</appender>
```

## Async Logging

Both the Stdout logger and Json logger can be used with an async
logger. Use `LOG_APPENDER=async_stdout` or
`LOG_APPENDER=async_json`. This is recommended for async services to
avoid blocking when logging.

## Logging Akka-Http Services

You need to include `libats-http` as dependency in your service and
use the `logResponseMetrics` directive when building the service routes.

All requests will be logged with relevant http metrics, in the case of
Json logging, this data can also be recorded to MDC.
