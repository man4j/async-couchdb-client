package com.equiron.acc.tutorial.lesson6;

import org.springframework.stereotype.Component;

import com.equiron.acc.CouchDb;
import com.equiron.acc.CouchDbConfig;
import com.equiron.acc.CouchDbValidator;
import com.equiron.acc.annotation.Security;
import com.equiron.acc.annotation.SecurityPattern;
import com.equiron.acc.annotation.ValidateDocUpdate;

@Component
@Security(admins = @SecurityPattern(names = "admin"), members = @SecurityPattern(roles = "oms"))
public class RemoteDb extends CouchDb {
    /**
     * Если авторство документа отсутствует или подделано или пользователь не администратор БД то ошибка сохранения документа 
     */
    @ValidateDocUpdate("if (!newDoc.omsId || (newDoc.omsId !== userCtx.name && !(userCtx.roles.indexOf('_admin') > -1))) throw({forbidden: 'Authorized user: ' + userCtx.name + ', but document author: ' + newDoc.omsId});")
    private CouchDbValidator securityValidator;
    
    public RemoteDb(CouchDbConfig config) {
        super(config);
    }
}
