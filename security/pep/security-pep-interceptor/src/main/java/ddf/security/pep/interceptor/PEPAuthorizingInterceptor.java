/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.security.pep.interceptor;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.ActionPermission;
import ddf.security.service.SecurityManager;
import ddf.security.service.impl.SecurityAssertionStore;

/**
 * Interceptor used to perform service authentication.
 * 
 */
public class PEPAuthorizingInterceptor extends AbstractPhaseInterceptor<Message> {

    private Logger logger = LoggerFactory.getLogger(PEPAuthorizingInterceptor.class);

    private static final QName SOAP_ACTION = new QName("http://www.w3.org/2005/08/addressing",
            "Action");

    private SecurityManager securityManager;

    public PEPAuthorizingInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    /**
     * Sets the security manager that will be used to create a subject.
     * 
     * @param securityManager
     */
    public void setSecurityManager(SecurityManager securityManager) {
        logger.trace("Setting the security manager");
        this.securityManager = securityManager;
    }

    /**
     * Intercepts a message. Interceptors should NOT invoke handleMessage or handleFault on the next
     * interceptor - the interceptor chain will take care of this.
     * 
     * @param message
     */
    @Override
    public void handleMessage(Message message) throws Fault {
        if (message != null) {
            // grab the SAML assertion associated with this Message from the
            // token store
            SecurityAssertion assertion = SecurityAssertionStore.getSecurityAssertion(message);
            SecurityLogger.logSecurityAssertionInfo(message);
            boolean isPermitted = false;

            if (assertion != null) {
                Subject user = null;
                ActionPermission action = null;
                try {
                    user = securityManager.getSubject(assertion.getSecurityToken());

                    logger.debug("Is user authenticated: {}", user.isAuthenticated());
                    logger.debug("Checking for permission");
                    SecurityLogger.logInfo("Is user [" + user.getPrincipal() + "] authenticated: "
                            + user.isAuthenticated());
                    List<Header> headers = CastUtils
                            .cast((List<?>) message.get(Header.HEADER_LIST));
                    for (Header curHeader : headers) {
                        if (curHeader.getName().equals(SOAP_ACTION)) {
                            action = new ActionPermission(
                                    ((Element) curHeader.getObject()).getTextContent());
                            break;
                        }
                    }
                    isPermitted = user.isPermitted(action);
                    logger.debug("Result of permission: {}", isPermitted);
                    SecurityLogger.logInfo("Is user [" + user.getPrincipal() + "] permitted: "
                            + isPermitted);
                    // store the subject so the DDF framework can use it later
                    message.put(SecurityConstants.SAML_ASSERTION, user);
                    logger.debug("Added assertion information to message at key {}",
                            SecurityConstants.SAML_ASSERTION);
                } catch (Exception e) {
                    logger.warn("Caught exception when trying to perform AuthZ.", e);
                    SecurityLogger.logWarn(
                            "Denying access : Caught exception when trying to perform AuthZ for user ["
                                    + user.getPrincipal() + "] for service " + action.getAction(),
                            e);
                    throw new AccessDeniedException("Unauthorized");
                }
                if (!isPermitted) {
                    logger.info("Denying access to {} for service {}", user.getPrincipal(),
                            action.getAction());
                    SecurityLogger.logWarn("Denying access to [" + user.getPrincipal()
                            + "] for service " + action.getAction());
                    throw new AccessDeniedException("Unauthorized");
                }

            }
        } else {
            logger.warn("Unable to retrieve the current message associated with the web service call.");
            throw new AccessDeniedException("Unauthorized");
        }
    }
}
