package org.hsbc.homework.service;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.hsbc.homework.config.AuthProperties;
import org.hsbc.homework.entity.Role;
import org.hsbc.homework.entity.Token;
import org.hsbc.homework.entity.User;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * the service that controls user creation/deletion,role creation/deletion, and authentication ,etc
 *
 * @author BruceSu
 */
public class AuthService {

    /**
     * indicates the operation is ok
     */
    public static final int SUCCESS = 0;
    /**
     * unknown error such as exception, communicate the administrator of the system to check
     */
    public static final int INNER_ERROR = 10000;
    /**
     * the user already exists
     */
    public static final int USER_EXISTS = 10001;
    /**
     * illegal parameter
     */
    public static final int PARAMS_ERROR = 10002;
    /**
     * the user doesn't exist
     */
    public static final int USER_NOT_EXIST = 10003;
    /**
     * the role already exists
     */
    public static final int ROLE_EXISTS = 10004;
    /**
     * the role doesn't exist
     */
    public static final int ROLE_NOT_EXIST = 10005;
    /**
     * wrong password
     */
    public static final int WRONG_PASSWORD = 10006;
    /**
     * invalid token
     */
    public static final int INVALID_TOKEN = 10007;
    /**
     * expired token
     */
    public static final int TOKEN_EXPIRED = 10008;

    private static Logger log = LogManager.getLogManager().getLogger("global");

    /**
     * key=userName
     */
    private ConcurrentHashMap<String, User> userMap = new ConcurrentHashMap<>();
    /**
     * user data sync controller
     */
    private ReentrantReadWriteLock userLock = new ReentrantReadWriteLock();
    /**
     * key=roleName
     */
    private ConcurrentHashMap<String, Role> roleMap = new ConcurrentHashMap<>();
    /**
     * role data sync controller
     */
    private ReentrantReadWriteLock roleLock = new ReentrantReadWriteLock();
    /**
     * key=userName
     */
    private ConcurrentHashMap<String, Set<String>> authMap = new ConcurrentHashMap<>();
    /**
     * key=token string
     */
    private ConcurrentHashMap<String, Token> tokenMap = new ConcurrentHashMap<>();
    /**
     * auth data sync controller
     */
    private ReentrantReadWriteLock authLock = new ReentrantReadWriteLock();

    private static AuthService instance = new AuthService();

    private AuthService() {
    }

    public static AuthService getInstance() {
        return instance;
    }

    /**
     * create User use the given userName and pwd
     *
     * @param userName
     * @param originalPwd
     * @return returns 0 if success,otherwise returns an error code.
     */
    public int createUser(String userName, String originalPwd) {
        if (this.userMap.containsKey(userName)) {
            return USER_EXISTS;
        }
        ReentrantReadWriteLock.WriteLock lock = userLock.writeLock();
        try {
            lock.lock();
            if (this.userMap.containsKey(userName)) {
                return USER_EXISTS;
            }

            User user = new User();
            user.setUserName(userName);
            user.setPwd(encodePwd(originalPwd));
            this.userMap.put(userName, user);
            return SUCCESS;
        } catch (UnsupportedEncodingException e) {
            log.log(Level.WARNING, String.format("userName=%s", userName), e);
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.WARNING, String.format("userName=%s", userName), e);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }

        return INNER_ERROR;
    }

    /**
     * delete User while exist
     *
     * @param userName
     * @return 0 or an error code.
     */
    public int deleteUser(String userName) {
        if (userName == null) {
            return PARAMS_ERROR;
        }
        //fail if not exist
        ReentrantReadWriteLock.WriteLock userLock = this.userLock.writeLock();
        ReentrantReadWriteLock.WriteLock authLock = this.authLock.writeLock();
        try {
            userLock.lock();
            authLock.lock();
            if (!this.userMap.containsKey(userName)) {
                return USER_NOT_EXIST;
            }

            this.userMap.remove(userName);
            this.authMap.remove(userName);
            return SUCCESS;
        } catch (Exception e) {
            log.log(Level.WARNING, String.format("userName=%s", userName), e);
        } finally {
            if (userLock != null) {
                userLock.unlock();
            }
            if (authLock != null) {
                authLock.unlock();
            }
        }

        return INNER_ERROR;
    }

    /**
     * create Role use the given roleName
     *
     * @param roleName
     * @return 0 or an error code.
     */
    public int createRole(String roleName) {
        if (this.roleMap.containsKey(roleName)) {
            return ROLE_EXISTS;
        }
        ReentrantReadWriteLock.WriteLock lock = this.roleLock.writeLock();
        try {
            lock.lock();
            if (this.roleMap.containsKey(roleName)) {
                return ROLE_EXISTS;
            }

            Role role = new Role();
            role.setRoleName(roleName);
            this.roleMap.put(role.getRoleName(), role);
            return SUCCESS;
        } catch (Exception e) {
            log.log(Level.WARNING, String.format("roleName=%s", roleName), e);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }

        return INNER_ERROR;
    }

    /**
     * delete Role while exist
     *
     * @param roleName
     * @return 0 or an error code.
     */
    public int deleteRole(String roleName) {
        if (roleName == null) {
            return PARAMS_ERROR;
        }
        ReentrantReadWriteLock.WriteLock roleLock = this.roleLock.writeLock();
        ReentrantReadWriteLock.WriteLock authLock = this.authLock.writeLock();
        try {
            roleLock.lock();
            authLock.lock();
            //fail if not exist
            if (!this.roleMap.containsKey(roleName)) {
                return ROLE_NOT_EXIST;
            }
            this.roleMap.remove(roleName);
            this.authMap.values().forEach(set -> set.remove(roleName));
            return SUCCESS;
        } catch (Exception e) {
            log.log(Level.WARNING, String.format("roleName=%s", roleName), e);
        } finally {
            if (roleLock != null) {
                roleLock.unlock();
            }
            if (authLock != null) {
                authLock.unlock();
            }
        }

        return INNER_ERROR;
    }

    /**
     * add roleName to userName
     * if the roleName is already associated with the userName,nothing should happen
     *
     * @param userName
     * @param roleName
     * @return 0 or an error code.
     */
    public int addRoleToUser(String userName, String roleName) {
        ReentrantReadWriteLock.ReadLock userLock = this.userLock.readLock();
        ReentrantReadWriteLock.ReadLock roleLock = this.roleLock.readLock();
        ReentrantReadWriteLock.WriteLock authLock = this.authLock.writeLock();

        try {
            userLock.lock();
            roleLock.lock();
            authLock.lock();

            if (userName == null || !this.userMap.containsKey(userName)) {
                return USER_NOT_EXIST;
            }
            if (roleName == null || !this.roleMap.containsKey(roleName)) {
                return ROLE_NOT_EXIST;
            }

            Set<String> set = this.authMap.get(userName);
            if (set == null) {
                set = new HashSet<>();
                this.authMap.put(userName, set);
            }
            set.add(roleName);
            return SUCCESS;
        } catch (Exception e) {
            log.log(Level.WARNING, String.format("userName=%s, roleName=%s", userName, roleName), e);
            return INNER_ERROR;
        } finally {
            if (userLock != null) {
                userLock.unlock();
            }
            if (roleLock != null) {
                roleLock.unlock();
            }
            if (authLock != null) {
                authLock.unlock();
            }
        }
    }

    /**
     * receive a token that has an expiry time which is defined by token_expire
     *
     * @param userName
     * @param pwd
     * @return an object contains an int code,its value is 0 while success,otherwise an error code
     */
    public Result<String> authenticate(String userName, String pwd) {
        if (userName == null || pwd == null) {
            return Result.fail(PARAMS_ERROR);
        }

        //error if not found
        ReentrantReadWriteLock.ReadLock userLock = this.userLock.readLock();
        ReentrantReadWriteLock.WriteLock authLock = this.authLock.writeLock();
        try {
            userLock.lock();
            authLock.lock();

            if (!this.userMap.containsKey(userName)) {
                return Result.fail(USER_NOT_EXIST);
            }
            if (!encodePwd(pwd).equals(this.userMap.get(userName).getPwd())) {
                return Result.fail(WRONG_PASSWORD);
            }

            //success
            Token token = new Token();
            token.setToken(UUID.randomUUID().toString());
            token.setUserName(userName);
            token.setCreateTime(System.currentTimeMillis());
            this.tokenMap.put(token.getToken(), token);

            //resize token map
            if (this.tokenMap.size() > AuthProperties.getInstance().getTokenResizeTrigger()) {
                long now = System.currentTimeMillis();
                List<String> expiredList = this.tokenMap.values().stream().filter(
                        t -> (now - t.getCreateTime()) > AuthProperties.getInstance().getTokenExpireSeconds() * 1000)
                    .map(Token::getToken).collect(Collectors.toList());
                expiredList.forEach(k -> this.tokenMap.remove(k));
            }

            return Result.success(token.getToken());
        } catch (Exception e) {
            log.log(Level.WARNING, String.format("userName=%s", userName), e);
            return Result.fail(INNER_ERROR);
        } finally {
            if (userLock != null) {
                userLock.unlock();
            }
            if (authLock != null) {
                authLock.unlock();
            }
        }
    }

    /**
     * unregister the given token
     *
     * @param token
     */
    public void invalidate(String token) {
        if (token == null) {
            return;
        }

        ReentrantReadWriteLock.WriteLock authLock = this.authLock.writeLock();
        try {
            authLock.lock();
            this.tokenMap.remove(token);
        } catch (Exception e) {
            log.log(Level.WARNING, String.format("token=%s", token), e);
        } finally {
            if (authLock != null) {
                authLock.unlock();
            }
        }
    }

    /**
     * check user authentication
     *
     * @param token
     * @param roleName
     * @return returns true if the user identified by the token,belongs to the roleName, otherwise returns false
     */
    public Result<Boolean> checkRole(String token, String roleName) {
        //error if token is invalid,expired etc
        if (token == null || roleName == null) {
            return Result.fail(PARAMS_ERROR);
        }

        ReentrantReadWriteLock.ReadLock authLock = this.authLock.readLock();
        ReentrantReadWriteLock.ReadLock roleLock = this.roleLock.readLock();
        try {
            roleLock.lock();
            authLock.lock();

            Token obj = this.tokenMap.get(token);
            if (obj == null) {
                return Result.fail(INVALID_TOKEN);
            }
            if (System.currentTimeMillis() - obj.getCreateTime()
                > AuthProperties.getInstance().getTokenExpireSeconds() * 1000) {
                return Result.fail(TOKEN_EXPIRED);
            }
            Set<String> roles = this.authMap.get(obj.getUserName());
            return Result.success(roles != null && roles.contains(roleName));
        } catch (Exception e) {
            log.log(Level.WARNING, String.format("token=%s", token), e);
        } finally {
            if (authLock != null) {
                authLock.unlock();
            }
            if (roleLock != null) {
                roleLock.unlock();
            }
        }

        return Result.fail(INNER_ERROR);
    }

    /**
     * get all roles
     *
     * @param token
     * @return the all associated roles with the user,identified with the given token
     */
    public Result<Set<String>> getAllRoles(String token) {
        //error if token is invalid
        if (token == null) {
            return Result.fail(PARAMS_ERROR);
        }

        ReentrantReadWriteLock.ReadLock authLock = this.authLock.readLock();
        ReentrantReadWriteLock.ReadLock roleLock = this.roleLock.readLock();
        try {
            roleLock.lock();
            authLock.lock();

            Token obj = this.tokenMap.get(token);
            if (obj == null) {
                return Result.fail(INVALID_TOKEN);
            }
            if (System.currentTimeMillis() - obj.getCreateTime()
                > AuthProperties.getInstance().getTokenExpireSeconds() * 1000) {
                return Result.fail(INVALID_TOKEN);
            }
            Set<String> roles = this.authMap.getOrDefault(obj.getUserName(), new HashSet<>());

            return Result.success(roles);
        } catch (Exception e) {
            log.log(Level.WARNING, String.format("token=%s", token), e);
            return Result.fail(INNER_ERROR);
        } finally {
            if (authLock != null) {
                authLock.unlock();
            }
            if (roleLock != null) {
                roleLock.unlock();
            }
        }
    }

    /**
     * password encoder
     *
     * @param orignalPwd
     * @return encoded string
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    private static String encodePwd(String orignalPwd) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        //md5
        byte[] array = getMD5(orignalPwd.getBytes("UTF-8"));
        //reverse
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            byte temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        //md5 again
        array = getMD5(array);
        //base64
        return Base64.encode(array);
    }

    /**
     * md5 algorithm
     *
     * @param data
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static byte[] getMD5(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(data);
        return digest.digest();
    }
}
