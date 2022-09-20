package com.equiron.yns.fixture;

import org.springframework.stereotype.Component;

import com.equiron.yns.YnsDb;
import com.equiron.yns.YnsDbConfig;
import com.equiron.yns.YnsValidator;
import com.equiron.yns.annotation.YnsSecurity;
import com.equiron.yns.annotation.YnsSecurityPattern;
import com.equiron.yns.annotation.YnsValidateDocUpdate;

import lombok.Getter;

@YnsSecurity(admins = @YnsSecurityPattern(names = "admin"), members = @YnsSecurityPattern(roles = {"replicator", "reader"}))
@Getter
@Component
public class ProtectedDb extends YnsDb {
    @YnsValidateDocUpdate("if (!(userCtx.roles.indexOf('replicator') > -1)) throw({forbidden: 'Read only'});")
    private YnsValidator validator;
        
    public ProtectedDb(YnsDbConfig config) {
        super(config);
    }
}
