# libats logging support

This module provides default settings for logging in libats
services. Simply include this module in your service and make sure
your service **does not ** contain any `logback.xml` file in it's
classpath, or make sure your settings are compatible with the settings
below.

Logging can be configured through the `LOG_APPENDER` environment variable.

## Stdout Logging

Set the `LOG_APPENDER` environment variable to `stdout`.

## Json Logging

Set the `LOG_APPENDER` environment variable to `json`. Additionally,
this logging could be overwritten and configued in a `logback.xml`
using the following settings:

```xml
<appender name="json" class="ch.qos.logback.core.ConsoleAppender">
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
