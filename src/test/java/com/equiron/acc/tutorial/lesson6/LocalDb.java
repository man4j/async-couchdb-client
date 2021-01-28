package com.equiron.acc.tutorial.lesson6;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.annotation.Replicated;

@Component
@Replicated(targetHost = "${HOST}", targetPort = "${PORT}", targetUser = "oms1", targetPassword = "123456", targetDbName = "remote_db")
public class LocalDb extends CouchDb {

}