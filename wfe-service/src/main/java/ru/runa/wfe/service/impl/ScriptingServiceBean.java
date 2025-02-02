/*
 * This file is part of the RUNA WFE project.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; version 2.1
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ru.runa.wfe.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import groovy.lang.GroovyShell;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ejb.interceptor.SpringBeanAutowiringInterceptor;
import ru.runa.wfe.ConfigurationException;
import ru.runa.wfe.commons.SystemProperties;
import ru.runa.wfe.commons.cache.CacheFreezingExecutor;
import ru.runa.wfe.script.AdminScript;
import ru.runa.wfe.script.AdminScriptOperationErrorHandler;
import ru.runa.wfe.script.AdminScriptRunner;
import ru.runa.wfe.script.common.ScriptExecutionContext;
import ru.runa.wfe.script.logic.AdminScriptLogic;
import ru.runa.wfe.service.ScriptingService;
import ru.runa.wfe.service.interceptors.EjbExceptionSupport;
import ru.runa.wfe.service.interceptors.EjbTransactionSupport;
import ru.runa.wfe.service.interceptors.PerformanceObserver;
import ru.runa.wfe.user.ExecutorAlreadyExistsException;
import ru.runa.wfe.user.SystemExecutors;
import ru.runa.wfe.user.User;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
@Interceptors({ EjbExceptionSupport.class, PerformanceObserver.class, EjbTransactionSupport.class,
        SpringBeanAutowiringInterceptor.class })
@WebService(name = "ScriptingAPI", serviceName = "ScriptingWebService")
@SOAPBinding
public class ScriptingServiceBean implements ScriptingService {
    @Autowired
    private AdminScriptRunner runner;
    @Autowired
    private AdminScriptLogic scriptLogic;
    private static final Set<String> SYSTEM_EXECUTOR_NAMES = Sets.newHashSet();
    static {
        SYSTEM_EXECUTOR_NAMES.add(SystemProperties.getAdministratorName());
        SYSTEM_EXECUTOR_NAMES.add(SystemProperties.getAdministratorsGroupName());
        SYSTEM_EXECUTOR_NAMES.add(SystemProperties.getBotsGroupName());
        SYSTEM_EXECUTOR_NAMES.add(SystemExecutors.PROCESS_STARTER_NAME);
    }

    @Override
    @WebMethod(exclude = true)
    public void executeAdminScript(@NonNull User user, @NonNull byte[] configData, @NonNull Map<String, byte[]> externalResources) {
        new CacheFreezingExecutor() {

            @Override
            protected void doExecute() {
                ScriptExecutionContext context = ScriptExecutionContext.create(user, externalResources, null);
                runner.runScript(configData, context, new AdminScriptOperationErrorHandler() {
                    @Override
                    public void handle(Throwable th) {
                        if (th instanceof ExecutorAlreadyExistsException) {
                            if (SYSTEM_EXECUTOR_NAMES.contains(((ExecutorAlreadyExistsException) th).getExecutorName())) {
                                return;
                            }
                        }
                        Throwables.propagate(th);
                    }
                });
            }

        }.execute();
    }

    @Override
    @WebMethod(exclude = true)
    public List<String> executeAdminScriptSkipError(User user, byte[] configData, Map<String, byte[]> externalResources,
            String defaultPasswordValue) {
        return executeAdminScriptSkipError(user, configData, externalResources, defaultPasswordValue, null);
    }

    @Override
    @WebMethod(exclude = true)
    public List<String> executeAdminScriptSkipError(User user, byte[] configData, Map<String, byte[]> externalResources, String defaultPasswordValue,
            String dataSourceDefaultPasswordValue) {
        Preconditions.checkArgument(user != null, "user");
        Preconditions.checkArgument(configData != null, "configData");
        Preconditions.checkArgument(externalResources != null, "externalResources");
        final List<String> errors = new ArrayList<String>();
        new CacheFreezingExecutor() {

            @Override
            protected void doExecute() {
                ScriptExecutionContext context = ScriptExecutionContext.create(user, externalResources, defaultPasswordValue,
                        dataSourceDefaultPasswordValue);
                runner.runScript(configData, context, new AdminScriptOperationErrorHandler() {
                    @Override
                    public void handle(Throwable th) {
                        if (th instanceof ExecutorAlreadyExistsException) {
                            if (SYSTEM_EXECUTOR_NAMES.contains(((ExecutorAlreadyExistsException) th).getExecutorName())) {
                                return;
                            }
                        }
                        errors.add(th.getMessage());
                    }
                });
            }

        }.execute();
        return errors;
    }

    @Override
    @WebResult(name = "result")
    public void executeGroovyScript(@WebParam(name = "user") @NonNull User user, @WebParam(name = "script") @NonNull String script) {
        if (!SystemProperties.isExecuteGroovyScriptInAPIEnabled()) {
            throw new ConfigurationException(
                    "In order to enable script execution set property 'scripting.groovy.enabled' to 'true' in system.properties or wfe.custom.system.properties");
        }
        GroovyShell shell = new GroovyShell();
        shell.evaluate(script);
    }

    @Override
    @WebResult(name = "result")
    public List<String> getScriptsNames() {
        return scriptLogic.getScriptsNames();
    }

    @Override
    @WebMethod(exclude = true)
    public void saveScript(String fileName, byte[] script) {
        scriptLogic.save(fileName, script);
    }

    @Override
    @WebMethod(exclude = true)
    public void deleteScript(String fileName) {
        scriptLogic.delete(fileName);
    }

    @Override
    @WebResult(name = "result")
    public byte[] getScriptSource(String fileName) {
        AdminScript adminScript = scriptLogic.getScriptByName(fileName);
        return adminScript.getContent();
    }
}
