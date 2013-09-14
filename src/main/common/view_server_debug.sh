#!/bin/sh
/home/man4j/bin/jdk1.7.0_02/bin/java -Duid=$$ -Dlogback.configurationFile=/home/man4j/tmp/logback-debug.xml -cp /home/man4j/projects/ngw/src/main/webapp/WEB-INF/lib/*:/home/man4j/projects/ngw/src/main/webapp/WEB-INF/classes:/home/man4j/projects/async-couchdb-client/target/test-classes com.n1global.acc.viewserver.CouchDbRequestProcessor
