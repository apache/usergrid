package org.usergrid.security.shiro;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.PermissionResolver;

public class PathBasedWildcardPermissionResolver implements PermissionResolver {

	@Override
    public Permission resolvePermission(String permissionString) {
        return new PathBasedWildcardPermission(permissionString);
    }

}
