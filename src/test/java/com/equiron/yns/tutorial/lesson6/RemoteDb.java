package com.equiron.yns.tutorial.lesson6;

import org.springframework.stereotype.Component;

import com.equiron.yns.YnsDb;
import com.equiron.yns.YnsValidator;
import com.equiron.yns.annotation.YnsSecurity;
import com.equiron.yns.annotation.YnsSecurityPattern;
import com.equiron.yns.annotation.YnsValidateDocUpdate;

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
