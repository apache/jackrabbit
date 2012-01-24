/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.security.user.action;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <code>PasswordValidationAction</code> provides a simple password validation
 * mechanism with the following configurable option:
 *
 * <ul>
 *     <li><strong>constraint</strong>: a regular expression that can be compiled
 *     to a {@link Pattern} defining validation rules for a password.</li>
 * </ul>
 *
 * <p>The password validation is executed on user creation and upon password
 * change. It throws a <code>ConstraintViolationException</code> if the password
 * validation fails.</p>
 *
 * <p>Example configuration:
 * <pre>
 *    &lt;UserManager class="org.apache.jackrabbit.core.security.user.UserPerWorkspaceUserManager"&gt;
 *       &lt;AuthorizableAction class="org.apache.jackrabbit.core.security.user.action.PasswordValidationAction"&gt;
 *          &lt;!--
 *          password length must be at least 8 chars and it must contain at least
 *          one upper and one lowercase ASCII character.
 *          --&gt;
 *          &lt;param name="constraint" value="^.*(?=.{8,})(?=.*[a-z])(?=.*[A-Z]).*"/&gt;
 *       &lt;/AuthorizableAction&gt;
 *    &lt;/UserManager&gt;
 * </pre>
 * </p>
 *
 * @see org.apache.jackrabbit.api.security.user.UserManager#createUser(String, String)
 * @see User#changePassword(String)
 * @see User#changePassword(String, String)
 */
public class PasswordValidationAction extends AbstractAuthorizableAction {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(PasswordValidationAction.class);

    private Pattern pattern;

    //-------------------------------------------------< AuthorizableAction >---
    @Override
    public void onCreate(User user, String password, Session session) throws RepositoryException {
        validatePassword(password);
    }

    @Override
    public void onPasswordChange(User user, String newPassword, Session session) throws RepositoryException {
        validatePassword(newPassword);
    }

    //---------------------------------------------------------< BeanConfig >---
    /**
     * Set the password constraint.
     *
     * @param constraint A regular expression that can be used to validate a new password.
     */
    public void setConstraint(String constraint) {
        try {
            pattern = Pattern.compile(constraint);
        } catch (PatternSyntaxException e) {
            log.warn("Invalid password constraint: ", e.getMessage());
        }
    }

    //------------------------------------------------------------< private >---
    /**
     * Validate the specified password.
     *
     * @param password The password to be validated
     * @throws RepositoryException If the specified password is too short or
     * doesn't match the specified password pattern.
     */
    private void validatePassword(String password) throws RepositoryException {
        if (password != null && isPlainText(password)) {
            if (pattern != null && !pattern.matcher(password).matches()) {
                throw new ConstraintViolationException("Password violates password constraint (" + pattern.pattern() + ").");
            }
        }
    }

    private static boolean isPlainText(String password) {
        try {
            return !CryptedSimpleCredentials.buildPasswordHash(password).equals(password);
        } catch (RepositoryException e) {
            // failed to build hash from pw -> proceed with the validation.
            return true;
        }
    }
}