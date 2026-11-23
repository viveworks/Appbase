/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://example.invalid/scratch-rehearsal).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.service.user.account.store;

import com.viveworks.appbase.core.service.log.LogService;
import com.viveworks.appbase.core.service.mediator.MediatorService;
import com.viveworks.appbase.core.service.metric.MetricService;
import com.viveworks.appbase.core.service.registry.ServiceRegistry;
import com.viveworks.appbase.core.service.validation.ValidationServiceException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.service.AbstractStatelessAppbaseService;
import com.viveworks.appbase.service.AppbaseServiceUtil;
import com.viveworks.appbase.service.password.PasswordHash;
import com.viveworks.appbase.service.password.PasswordHashes;
import com.viveworks.appbase.service.password.PasswordService;
import com.viveworks.appbase.service.user.account.data.UserAccount;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;

import javax.inject.Inject;
import java.util.Optional;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class JdbiUserAccountStore
        extends AbstractStatelessAppbaseService<JdbiUserAccountStoreConfiguration>
        implements UserAccountStore
{
    private final PasswordService passwordService;
    private final Handle jdbiHandle;
    private final Timer createUserAccountTimer;
    private final Timer readByAppbaseUserIdTimer;
    private final Timer readByIdpUserIdTimer;
    private final Timer readByEmailTimer;
    private final Timer existsAppbaseUserIdTimer;
    private final Timer existsByIdpUserIdTimer;
    private final Timer existsByEmailTimer;
    private final Timer updateUserAccountTimer;
    private final Timer deleteByIdpUserIdTimer;
    private final Timer deleteByAppbaseUserIdTimer;
    private final Timer deleteByEmailTimer;

    @Inject
    public JdbiUserAccountStore(
            final LogService logService,
            final MetricService metricService,
            final MediatorService mediatorService,
            final ServiceRegistry serviceRegistry,
            final PasswordService passwordService,
            final Handle jdbiHandle,
            final JdbiUserAccountStoreConfiguration jdbiUserAccountStoreConfiguration)
    {
        super(logService, metricService, mediatorService, serviceRegistry, jdbiUserAccountStoreConfiguration);

        AppbaseUtil.notNull(passwordService, "passwordService");
        this.passwordService = passwordService;

        AppbaseUtil.notNull(jdbiHandle, "jdbiHandle");
        this.jdbiHandle = jdbiHandle;
        this.jdbiHandle.registerColumnMapper(PasswordHash.class, new PasswordHash.PasswordHashColumnMapper());
        this.jdbiHandle.registerColumnMapper(PasswordHashes.class, new PasswordHashes.PasswordHashesColumnMapper());

        final MetricRegistry metricRegistry = metricService.getMetricRegistry();
        this.createUserAccountTimer =
                metricRegistry.timer(metricService.name(getClass(), "createUserAccountTimer"));
        this.readByAppbaseUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "readByAppbaseUserIdTimer"));
        this.readByIdpUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "readByIdpUserIdTimer"));
        this.readByEmailTimer =
                metricRegistry.timer(metricService.name(getClass(), "readByEmailTimer"));
        this.existsAppbaseUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsAppbaseUserIdTimer"));
        this.existsByIdpUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsByIdpUserIdTimer"));
        this.existsByEmailTimer =
                metricRegistry.timer(metricService.name(getClass(), "existsByEmailTimer"));
        this.updateUserAccountTimer =
                metricRegistry.timer(metricService.name(getClass(), "updateUserAccountTimer"));
        this.deleteByIdpUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteByIdpUserIdTimer"));
        this.deleteByAppbaseUserIdTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteByAppbaseUserIdTimer"));
        this.deleteByEmailTimer =
                metricRegistry.timer(metricService.name(getClass(), "deleteByEmailTimer"));
    }

    @Override
    protected void registerService() {
        serviceRegistry.registerAppbaseService(UserAccountStore.class, this);
        serviceRegistry.registerAppbaseService(JdbiUserAccountStore.class, this);
    }

    @Override
    protected void shutdownImpl() {
        jdbiHandle.close();
    }

    @Override
    public int createUserAccount(final UserAccount userAccount) throws UserAccountStoreException {
        AppbaseUtil.notNull(userAccount, "userAccount");

        final Timer.Context timer = createUserAccountTimer.time();
        try {
            if (getConfiguration().isManualValidation()) {
                AppbaseUtil.checkedValidation(new UserAccount.Validator(), userAccount);
            }

            final String passwordHashes = passwordService.serializePasswordHashes(userAccount.getPasswordHashes());

            String passwordResetCodeHash = null;
            if (userAccount.getPasswordResetCodeHash() != null) {
                passwordResetCodeHash =
                        passwordService.serializePasswordHash(userAccount.getPasswordResetCodeHash());
            }

            return jdbiHandle
                    .createUpdate("INSERT INTO " + getConfiguration().getUserAccountTableName() +
                            "(appbaseUserId, idpUserId, email, name, passwordHashes, passwordResetEmailSent, " +
                            "passwordResetCodeHash, userTopic, userHomeDataCenter, emailVerified, languageCode, " +
                            "addressLine1, addressLine2, city, stateProvince, postalCode, country, " +
                            "latitude, longitude, " +
                            "deleted, creationTimestamp, lastAccessTimestamp) " +
                            "VALUES (" +
                            ":appbaseUserId, :idpUserId, :email, :name, :passwordHashes, :passwordResetEmailSent, " +
                            ":passwordResetCodeHash, :userTopic, :userHomeDataCenter, :emailVerified, :languageCode, " +
                            ":addressLine1, :addressLine2, :city, :stateProvince, :postalCode, :country, " +
                            ":latitude, :longitude, " +
                            ":deleted, :creationTimestamp, :lastAccessTimestamp" +
                            ")")
                    .bind("appbaseUserId", userAccount.getAppbaseUserId())
                    .bind("idpUserId", userAccount.getIdpUserId())
                    .bind("email", userAccount.getEmail())
                    .bind("name", userAccount.getName())
                    .bind("passwordHashes", passwordHashes)
                    .bind("passwordResetEmailSent", AppbaseServiceUtil.sqlTinyInt(userAccount.getPasswordResetEmailSent()))
                    .bind("passwordResetCodeHash", passwordResetCodeHash)
                    .bind("userTopic", userAccount.getUserTopic())
                    .bind("userHomeDataCenter", userAccount.getUserHomeDataCenter())
                    .bind("emailVerified", userAccount.getEmailVerified())
                    .bind("languageCode", userAccount.getLanguageCode())
                    .bind("addressLine1", userAccount.getAddressLine1())
                    .bind("addressLine2", userAccount.getAddressLine2())
                    .bind("city", userAccount.getCity())
                    .bind("stateProvince", userAccount.getStateProvince())
                    .bind("postalCode", userAccount.getPostalCode())
                    .bind("country", userAccount.getCountry())
                    .bind("latitude", userAccount.getLatitude())
                    .bind("longitude", userAccount.getLongitude())
                    .bind("deleted", AppbaseServiceUtil.sqlTinyInt(userAccount.getDeleted()))
                    .bind("creationTimestamp", userAccount.getCreationTimestamp())
                    .bind("lastAccessTimestamp", userAccount.getLastAccessTimestamp())
                    .execute();
        } catch (ValidationServiceException e) {
            throw new UserAccountStoreException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public Optional<UserAccount> readByAppbaseUserId(final String appbaseUserId) throws UserAccountStoreException {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");

        final Timer.Context timer = readByAppbaseUserIdTimer.time();
        try {
            return jdbiHandle
                    .select("SELECT * FROM " + getConfiguration().getUserAccountTableName() +
                            " WHERE appbaseUserId=:appbaseUserId")
                    .bind("appbaseUserId", appbaseUserId)
                    .map(BeanMapper.of(UserAccount.class))
                    .findOne();
        } finally {
            timer.stop();
        }
    }

    @Override
    public Optional<UserAccount> readByIdpUserId(final String idpUserId) throws UserAccountStoreException {
        AppbaseUtil.notBlank(idpUserId, "idpUserId");

        final Timer.Context timer = readByIdpUserIdTimer.time();
        try {
            return jdbiHandle
                    .select("SELECT * FROM " + getConfiguration().getUserAccountTableName() +
                            " WHERE idpUserId=:idpUserId")
                    .bind("idpUserId", idpUserId)
                    .map(BeanMapper.of(UserAccount.class))
                    .findOne();
        } finally {
            timer.stop();
        }
    }

    @Override
    public Optional<UserAccount> readByEmail(final String email) throws UserAccountStoreException {
        AppbaseUtil.notBlank(email, "email");

        final Timer.Context timer = readByEmailTimer.time();
        try {
            return jdbiHandle
                    .select("SELECT * FROM " + getConfiguration().getUserAccountTableName() +
                            " WHERE email=:email")
                    .bind("email", email)
                    .map(BeanMapper.of(UserAccount.class))
                    .findOne();
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean existsByAppbaseUserId(final String appbaseUserId) throws UserAccountStoreException {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");

        final Timer.Context timer = existsAppbaseUserIdTimer.time();
        try {
            return jdbiHandle
                    .select("SELECT appbaseUserId FROM " + getConfiguration().getUserAccountTableName() +
                            " WHERE appbaseUserId=:appbaseUserId")
                    .bind("appbaseUserId", appbaseUserId)
                    .mapTo(String.class)
                    .findOne()
                    .isPresent();
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean existsByIdpUserId(final String idpUserId) throws UserAccountStoreException {
        AppbaseUtil.notBlank(idpUserId, "idpUserId");

        final Timer.Context timer = existsByIdpUserIdTimer.time();
        try {
            return jdbiHandle
                    .select("SELECT appbaseUserId FROM " + getConfiguration().getUserAccountTableName() +
                            " WHERE idpUserId=:idpUserId")
                    .bind("idpUserId", idpUserId)
                    .mapTo(String.class)
                    .findOne()
                    .isPresent();
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean existsByEmail(final String email) throws UserAccountStoreException {
        AppbaseUtil.notBlank(email, "email");

        final Timer.Context timer = existsByEmailTimer.time();
        try {
            return jdbiHandle
                    .select("SELECT appbaseUserId FROM " + getConfiguration().getUserAccountTableName() +
                            " WHERE email=:email")
                    .bind("email", email)
                    .mapTo(String.class)
                    .findOne()
                    .isPresent();
        } finally {
            timer.stop();
        }
    }

    @Override
    public int updateUserAccount(final UserAccount userAccount) throws UserAccountStoreException {
        AppbaseUtil.notNull(userAccount, "userAccount");

        final Timer.Context timer = updateUserAccountTimer.time();
        try {
            if (getConfiguration().isManualValidation()) {
                AppbaseUtil.checkedValidation(new UserAccount.Validator(), userAccount);
            }

            final String passwordHashes = passwordService.serializePasswordHashes(userAccount.getPasswordHashes());

            String passwordResetCodeHash = null;
            if (userAccount.getPasswordResetCodeHash() != null) {
                passwordResetCodeHash = passwordService.serializePasswordHash(userAccount.getPasswordResetCodeHash());
            }

            return jdbiHandle
                    .createUpdate("UPDATE " + getConfiguration().getUserAccountTableName() + " SET " +
                            "appbaseUserId=:appbaseUserId, email=:email, name=:name, passwordHashes=:passwordHashes, " +
                            "passwordResetEmailSent=:passwordResetEmailSent, passwordResetCodeHash=:passwordResetCodeHash, " +
                            "userTopic=:userTopic, userHomeDataCenter=:userHomeDataCenter, " +
                            "emailVerified=:emailVerified, languageCode=:languageCode, " +
                            "addressLine1=:addressLine1, addressLine2=:addressLine2, city=:city, " +
                            "stateProvince=:stateProvince, postalCode=:postalCode, country=:country, " +
                            "latitude=:latitude, longitude=:longitude, " +
                            "deleted=:deleted, " +
                            "creationTimestamp=:creationTimestamp, lastAccessTimestamp=:lastAccessTimestamp " +
                            "WHERE idpUserId=:idpUserId")
                    .bind("appbaseUserId", userAccount.getAppbaseUserId())
                    .bind("email", userAccount.getEmail())
                    .bind("name", userAccount.getName())
                    .bind("passwordHashes", passwordHashes)
                    .bind("passwordResetEmailSent", AppbaseServiceUtil.sqlTinyInt(userAccount.getPasswordResetEmailSent()))
                    .bind("passwordResetCodeHash", passwordResetCodeHash)
                    .bind("userTopic", userAccount.getUserTopic())
                    .bind("userHomeDataCenter", userAccount.getUserHomeDataCenter())
                    .bind("emailVerified", userAccount.getEmailVerified())
                    .bind("languageCode", userAccount.getLanguageCode())
                    .bind("addressLine1", userAccount.getAddressLine1())
                    .bind("addressLine2", userAccount.getAddressLine2())
                    .bind("city", userAccount.getCity())
                    .bind("stateProvince", userAccount.getStateProvince())
                    .bind("postalCode", userAccount.getPostalCode())
                    .bind("country", userAccount.getCountry())
                    .bind("latitude", userAccount.getLatitude())
                    .bind("longitude", userAccount.getLongitude())
                    .bind("deleted", AppbaseServiceUtil.sqlTinyInt(userAccount.getDeleted()))
                    .bind("creationTimestamp", userAccount.getCreationTimestamp())
                    .bind("lastAccessTimestamp", userAccount.getLastAccessTimestamp())
                    .bind("idpUserId", userAccount.getIdpUserId())
                    .execute();
        } catch (ValidationServiceException e) {
            throw new UserAccountStoreException(e);
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteByIdpUserId(final String idpUserId) throws UserAccountStoreException {
        AppbaseUtil.notBlank(idpUserId, "idpUserId");

        final Timer.Context timer = deleteByIdpUserIdTimer.time();
        try {
            final int updated = jdbiHandle
                    .createUpdate(
                            "DELETE FROM " + getConfiguration().getUserAccountTableName() +
                            " WHERE idpUserId=:idpUserId")
                    .bind("idpUserId", idpUserId)
                    .execute();
            return updated > 0;
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteByAppbaseUserId(final String appbaseUserId) throws UserAccountStoreException {
        AppbaseUtil.notBlank(appbaseUserId, "appbaseUserId");

        final Timer.Context timer = deleteByAppbaseUserIdTimer.time();
        try {
            final int updated = jdbiHandle
                    .createUpdate(
                            "DELETE FROM " + getConfiguration().getUserAccountTableName() +
                            " WHERE appbaseUserId=:appbaseUserId")
                    .bind("appbaseUserId", appbaseUserId)
                    .execute();
            return updated > 0;
        } finally {
            timer.stop();
        }
    }

    @Override
    public boolean deleteByEmail(final String email) throws UserAccountStoreException {
        AppbaseUtil.notBlank(email, "email");

        final Timer.Context timer = deleteByEmailTimer.time();
        try {
            final int updated = jdbiHandle
                    .createUpdate(
                            "DELETE FROM " + getConfiguration().getUserAccountTableName() +
                            " WHERE email=:email")
                    .bind("email", email)
                    .execute();
            return updated > 0;
        } finally {
            timer.stop();
        }
    }
}
