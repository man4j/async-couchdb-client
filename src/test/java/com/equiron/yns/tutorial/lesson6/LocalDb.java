package com.equiron.yns.tutorial.lesson6;

import org.springframework.stereotype.Component;

import com.equiron.yns.YnsDb;
import com.equiron.yns.annotation.YnsReplicated;

@Component
@YnsReplicated(targetHost = "${HOST}", 
			   targetPort = "${PORT}", 
			   targetUser = "oms1", 
			   targetPassword = "123456", 
			   targetDbName = "remote_db", 
			   selector = "{\"omsId\":\"oms1\"}")
public class LocalDb extends YnsDb {
    //empty
}
