/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.organization.dao.ldap;

import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.server.exception.UserNotFoundException;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.dto.server.DtoFactory;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

public class UserDaoTest {
    UserDaoImpl        userDao;
    File               server;
    EmbeddedLdapServer embeddedLdapServer;
    User[]             users;

    @BeforeMethod
    public void setUp() throws Exception {
        URL u = Thread.currentThread().getContextClassLoader().getResource(".");
        File target = new File(u.toURI()).getParentFile();
        server = new File(target, "server");
        Assert.assertTrue(server.mkdirs(), "Unable create directory for temporary data");
        embeddedLdapServer = EmbeddedLdapServer.start(server);
        userDao = new UserDaoImpl(embeddedLdapServer.getUrl(), "dc=codenvy;dc=com", new UserAttributesMapper());
        users = new User[]{
                DtoFactory.getInstance().createDto(User.class)
                          .withId("1")
                          .withEmail("user1@mail.com")
                          .withPassword("secret")
                          .withAliases(Arrays.asList("user1@mail.com")),
                DtoFactory.getInstance().createDto(User.class)
                          .withId("2")
                          .withEmail("user2@mail.com")
                          .withPassword("secret")
                          .withAliases(Arrays.asList("user2@mail.com")),
                DtoFactory.getInstance().createDto(User.class)
                          .withId("3")
                          .withEmail("user3@mail.com")
                          .withPassword("secret")
                          .withAliases(Arrays.asList("user3@mail.com"))
        };
        for (User user : users) {
            userDao.create(user);
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {
        embeddedLdapServer.stop();
        Assert.assertTrue(IoUtil.deleteRecursive(server), "Unable remove temporary data");
    }

    @Test
    public void testGetUserById() throws Exception {
        User userById = userDao.getById(users[1].getId());
        Assert.assertEquals(userById.getId(), users[1].getId());
        Assert.assertEquals(userById.getEmail(), users[1].getEmail());
        Assert.assertEquals(userById.getPassword(), users[1].getPassword());
        Assert.assertEquals(userById.getAliases(), users[1].getAliases());
    }

    @Test
    public void testGetNotExistedUserById() throws Exception {
        Assert.assertNull(userDao.getById("invalid"));
    }

    @Test
    public void testGetUserByAlias() throws Exception {
        User userByAlias = userDao.getByAlias(users[2].getAliases().get(0));
        Assert.assertEquals(userByAlias.getId(), users[2].getId());
        Assert.assertEquals(userByAlias.getEmail(), users[2].getEmail());
        Assert.assertEquals(userByAlias.getPassword(), users[2].getPassword());
        Assert.assertEquals(userByAlias.getAliases(), users[2].getAliases());
    }

    @Test
    public void testGetNotExistedUserByAlias() throws Exception {
        Assert.assertNull(userDao.getByAlias("invalid"));
    }

    @Test
    public void testUpdateUser() throws Exception {
        User copy = DtoFactory.getInstance().clone(users[0]);
        copy.setEmail("example@mail.com");
        copy.setPassword("new_secret");
        copy.setAliases(Arrays.asList("example@mail.com"));
        userDao.update(copy);
        User updated = userDao.getById(copy.getId());
        Assert.assertEquals(updated.getId(), copy.getId());
        Assert.assertEquals(updated.getEmail(), copy.getEmail());
        Assert.assertEquals(updated.getPassword(), copy.getPassword());
        Assert.assertEquals(updated.getAliases(), copy.getAliases());
    }

    @Test
    public void testUpdateNotExistedUser() throws Exception {
        User copy = DtoFactory.getInstance().clone(users[0]);
        copy.setId("invalid"); // ID may not be updated
        copy.setEmail("example@mail.com");
        copy.setPassword("new_secret");
        copy.setAliases(Arrays.asList("example@mail.com"));
        try {
            userDao.update(copy);
            Assert.fail();
        } catch (UserNotFoundException e) {
        }
        User updated = userDao.getById(users[0].getId());
        Assert.assertEquals(updated.getId(), users[0].getId());
        Assert.assertEquals(updated.getEmail(), users[0].getEmail());
        Assert.assertEquals(updated.getPassword(), users[0].getPassword());
        Assert.assertEquals(updated.getAliases(), users[0].getAliases());
    }

    @Test
    public void testUpdateUserConflictAlias() throws Exception {
        User copy = DtoFactory.getInstance().clone(users[0]);
        copy.setEmail("example@mail.com");
        copy.setPassword("new_secret");
        String conflictAlias = copy.getAliases().get(0);
        copy.getAliases().add("example@mail.com");
        try {
            userDao.update(copy);
            Assert.fail();
        } catch (UserException e) {
            Assert.assertEquals(e.getMessage(),
                                String.format("Unable update user '%s'. User alias %s is already in use.", copy.getId(), conflictAlias));
        }
        User updated = userDao.getById(users[0].getId());
        Assert.assertEquals(updated.getId(), users[0].getId());
        Assert.assertEquals(updated.getEmail(), users[0].getEmail());
        Assert.assertEquals(updated.getPassword(), users[0].getPassword());
        Assert.assertEquals(updated.getAliases(), users[0].getAliases());
    }

    @Test
    public void testRemoveUser() throws Exception {
        User user = userDao.getById(users[0].getId());
        Assert.assertNotNull(user);
        userDao.remove(users[0].getId());
        user = userDao.getById(users[0].getId());
        Assert.assertNull(user);
    }

    @Test
    public void testRemoveNotExistedUser() throws Exception {
        try {
            userDao.remove("invalid");
            Assert.fail();
        } catch (UserNotFoundException e) {
        }
    }

    @Test(expectedExceptions = UserException.class, expectedExceptionsMessageRegExp = ".*User already exists.*")
    public void testCreateUserConflictId() throws Exception {
        User copy = DtoFactory.getInstance().clone(users[0]);
        copy.setEmail("example@mail.com");
        copy.setPassword("new_secret");
        copy.setAliases(Arrays.asList("example@mail.com"));
        userDao.create(copy);
    }

    @Test(expectedExceptions = UserException.class, expectedExceptionsMessageRegExp = ".*User alias .* is already in use.*")
    public void testCreateUserConflictAlias() throws Exception {
        User copy = DtoFactory.getInstance().clone(users[0]);
        copy.setId("new_id");
        copy.setEmail("example@mail.com");
        copy.setPassword("new_secret");
        // Keep one of aliases from existed user. Duplication of aliases is not allowed!!
        copy.getAliases().add("example@mail.com");
        userDao.create(copy);
    }
}