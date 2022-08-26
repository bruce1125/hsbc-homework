package org.hsbc.homework;

import java.util.Set;
import java.util.UUID;
import org.hsbc.homework.config.AuthProperties;
import org.hsbc.homework.service.AuthService;
import org.hsbc.homework.service.Result;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author BruceSu
 */
public class AuthServiceTest {

    @Test
    public void createUser() {
        Assert.assertEquals(AuthService.SUCCESS, AuthService.getInstance().createUser("testUserName", "testPwd"));
        Assert.assertEquals(AuthService.USER_EXISTS, AuthService.getInstance().createUser("testUserName", "testPwd"));
        //clear data
        AuthService.getInstance().deleteUser("testUserName");
    }

    @Test
    public void deleteUser() {
        AuthService.getInstance().createUser("testUserName", "testPwd");
        Assert.assertEquals(AuthService.SUCCESS, AuthService.getInstance().deleteUser("testUserName"));
        Assert.assertEquals(AuthService.USER_NOT_EXIST, AuthService.getInstance().deleteUser("testUserName"));
    }

    @Test
    public void createRole() {
        Assert.assertEquals(AuthService.SUCCESS, AuthService.getInstance().createRole("testRole"));
        Assert.assertEquals(AuthService.ROLE_EXISTS, AuthService.getInstance().createRole("testRole"));

        //clear data
        AuthService.getInstance().deleteRole("testRole");
    }

    @Test
    public void deleteRole() {
        Assert.assertEquals(AuthService.ROLE_NOT_EXIST, AuthService.getInstance().deleteRole("testRole"));
        AuthService.getInstance().createRole("testRole");
        Assert.assertEquals(AuthService.SUCCESS, AuthService.getInstance().deleteRole("testRole"));
        Assert.assertEquals(AuthService.ROLE_NOT_EXIST, AuthService.getInstance().deleteRole("testRole"));
    }

    @Test
    public void addRoleToUser() {
        //user not exist
        Assert.assertEquals(AuthService.USER_NOT_EXIST,
            AuthService.getInstance().addRoleToUser("testUserName", "testRole"));
        //create user
        AuthService.getInstance().createUser("testUserName", "testPwd");
        //role not exist
        Assert.assertEquals(AuthService.ROLE_NOT_EXIST,
            AuthService.getInstance().addRoleToUser("testUserName", "testRole"));
        //create role
        AuthService.getInstance().createRole("testRole");
        Assert.assertEquals(AuthService.SUCCESS, AuthService.getInstance().addRoleToUser("testUserName", "testRole"));

        //clear data
        AuthService.getInstance().deleteUser("testUserName");
        AuthService.getInstance().deleteRole("testRole");
    }

    @Test
    public void authenticate() {
        Assert.assertEquals(AuthService.USER_NOT_EXIST,
            AuthService.getInstance().authenticate("testUserName", "testPwd").getStatus());
        AuthService.getInstance().createUser("testUserName", "testPwd");
        //wrong password
        Assert.assertEquals(AuthService.WRONG_PASSWORD,
            AuthService.getInstance().authenticate("testUserName", "errorPwd").getStatus());
        //correct password
        Result<String> ret = AuthService.getInstance().authenticate("testUserName", "testPwd");
        Assert.assertEquals(AuthService.SUCCESS, ret.getStatus());
        Assert.assertNotNull(UUID.fromString(ret.getRetObj()));

        //checkRole
        AuthService.getInstance().createRole("testRole");
        AuthService.getInstance().addRoleToUser("testUserName", "testRole");
        Assert.assertTrue(AuthService.getInstance().checkRole(ret.getRetObj(), "testRole").getRetObj());
        Assert.assertEquals(AuthService.INVALID_TOKEN,
            AuthService.getInstance().checkRole("another token", "testRole").getStatus());
        Assert.assertFalse(AuthService.getInstance().checkRole(ret.getRetObj(), "another role").getRetObj());

        //getAllRoles
        Result<Set<String>> allRolesRet = AuthService.getInstance().getAllRoles("invalid token");
        Assert.assertEquals(AuthService.INVALID_TOKEN, allRolesRet.getStatus());
        AuthService.getInstance().createRole("role2");
        AuthService.getInstance().addRoleToUser("testUserName", "role2");
        allRolesRet = AuthService.getInstance().getAllRoles(ret.getRetObj());
        Assert.assertEquals(2, allRolesRet.getRetObj().size());
        Assert.assertTrue(allRolesRet.getRetObj().contains("role2"));
        Assert.assertTrue(allRolesRet.getRetObj().contains("testRole"));

        //invalidate
        AuthService.getInstance().invalidate(ret.getRetObj());
        Assert.assertEquals(AuthService.INVALID_TOKEN,
            AuthService.getInstance().checkRole(ret.getRetObj(), "testRole").getStatus());

        //clear data
        AuthService.getInstance().deleteUser("testUserName");
        AuthService.getInstance().deleteRole("testRole");
        AuthService.getInstance().deleteRole("role2");
    }

    @Test
    public void tokenExpired() throws InterruptedException {
        AuthService.getInstance().createUser("testUserName", "testPwd");
        AuthService.getInstance().createRole("testRole");
        AuthService.getInstance().addRoleToUser("testUserName", "testRole");
        Result<String> ret = AuthService.getInstance().authenticate("testUserName", "testPwd");
        Assert.assertTrue(AuthService.getInstance().checkRole(ret.getRetObj(), "testRole").getRetObj());
        //wait for expired
        Thread.sleep(AuthProperties.getInstance().getTokenExpireSeconds() * 1000);
        Assert.assertEquals(AuthService.TOKEN_EXPIRED,
            AuthService.getInstance().checkRole(ret.getRetObj(), "testRole").getStatus());

        //clear data
        AuthService.getInstance().deleteUser("testUserName");
        AuthService.getInstance().deleteRole("testRole");
    }
}
