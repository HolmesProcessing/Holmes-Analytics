# Contributing to Holmes-Analytics

## Technologies

Holmes-Analytics is build in Scala mainly using Akka. You should be somewhat familiar with the language and the actor model in generall as well as akkas implementation of it in particular. The respective documentations are a good place to start, altoigh to "just" submit a quick fix all of this might not be necessary.

## Actor Layout
This is the current actor layout. This layout is subject to changes in the future.

![Holmes-Analytics Actor Layout v0.1](http://i.imgur.com/vvlspr5.png)

## Debugging
If you need more debug output it is highly advised to have a look at `src/main/resources/application.conf` and `src/main/resources/logback.xml` respectively.
