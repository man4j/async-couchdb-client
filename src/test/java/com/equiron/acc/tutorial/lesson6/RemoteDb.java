package com.equiron.acc.tutorial.lesson6;

import org.springframework.stereotype.Component;

import com.equiron.acc.YnsDb;
import com.equiron.acc.YnsValidator;
import com.equiron.acc.annotation.YnsSecurity;
import com.equiron.acc.annotation.YnsSecurityPattern;
import com.equiron.acc.annotation.YnsValidateDocUpdate;

@Component
@YnsSecurity(admins = @YnsSecurityPattern(names = "admin"), members = @YnsSecurityPattern(roles = "oms"))
public class RemoteDb extends YnsDb {
    /**
     * Если авторство документа отсутствует или подделано или пользователь не администратор БД то ошибка сохранения документа 
     */
    @YnsValidateDocUpdate("""
                          if (!newDoc.omsId || (newDoc.omsId !== userCtx.name && !(userCtx.roles.indexOf('_admin') > -1))) {
                              throw({forbidden: 'Authorized user: ' + userCtx.name + ', but document author: ' + newDoc.omsId});
                          }                          
                          """)
    private YnsValidator securityValidator;
}
