/*
 * Copyright 2013-2015, Juha Lindfors. All rights reserved.
 */
package org.openremote.beehive.account.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author Juha Lindfors
 */
public class EntityTransactionFilter implements Filter
{

  // Constants ------------------------------------------------------------------------------------

  public static final String PERSISTENCE_CONTEXT_NAME_CONFIGURATION = "PersistenceContext";

  private static Logger log = LoggerFactory.getLogger(
          AccountManager.Log.TRANSACTION.getCanonicalLogHierarchyName());


  // Instance Fields ------------------------------------------------------------------------------


  private EntityManagerFactory emFactory = null;

  private EntityManager entityManager = null;


  // Filter Overrides -----------------------------------------------------------------------------

  @Override public void init(FilterConfig config) throws UnavailableException
  {
    log.debug("Initializing transaction management filter...");

    String persistenceCtx = config.getInitParameter(PERSISTENCE_CONTEXT_NAME_CONFIGURATION);

    if (persistenceCtx == null || persistenceCtx.equals(""))
    {
      String msg = "Persistence context configuration in web.xml <filter> element is missing. " +
                   "Transaction management is DISABLED.";

      log.error(msg);

      throw new UnavailableException(msg);
    }

    Map<String, String> persistenceProperties = new HashMap<String, String>();

    @SuppressWarnings("unchecked") Enumeration<String> paramNames = config.getInitParameterNames();

    while (paramNames.hasMoreElements())
    {
      String name = paramNames.nextElement();

      if (name.startsWith("javax.persistence.") || name.startsWith("hibernate."))
      {
        persistenceProperties.put(name, config.getInitParameter(name));
      }
    }

    emFactory = Persistence.createEntityManagerFactory(persistenceCtx, persistenceProperties);

    entityManager = emFactory.createEntityManager();

    log.debug("Transaction management filter initialized.");
  }


  @Override public void destroy()
  {
    if (entityManager != null)
    {
      entityManager.close();
    }
  }



  @Override public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
  {
    EntityTransaction tx = null;
    String user = "<no name>";

    HttpServletRequest request = (HttpServletRequest)req;
    TransactionResponse response = new TransactionResponse((HttpServletResponse)resp);

    try
    {
      user = request.getRemoteUser();

      request.setAttribute(AccountManager.ENTITY_MANAGER_LOOKUP, entityManager);

      tx = entityManager.getTransaction();

      tx.begin();

      log.info(
          "Started transaction for user ''{}'', request ''{} {}''...",
          user, request.getMethod(), request.getServletPath() + request.getPathInfo()
      );

      chain.doFilter(request, response);
    }

    catch (Throwable cause)
    {
      if (tx != null)
      {
        tx.setRollbackOnly();
      }

      log.error("Request failed, tx marked for rollback : {}", cause, cause.getMessage());
    }

    finally
    {
      if (tx != null && tx.isActive())
      {
        if (tx.getRollbackOnly())
        {
          tx.rollback();

          log.info(
              "ROLLBACK: tx for user ''{}'' was marked for roll back. Request : ''{} {}''",
              user, request.getMethod(), request.getServletPath() + request.getPathInfo()
          );
        }

        else if (response.status >= 400)
        {
          tx.rollback();

          log.info(
              "ROLLBACK: error response ''{} : {}'' to user ''{}'' request ''{} {}''.",
              response.status, response.statusMsg,
              user, request.getMethod(), request.getServletPath() + request.getPathInfo()
          );
        }

        else
        {
          tx.commit();

          log.info(
              "COMMIT: user ''{}'' request ''{} {}''",
              user, request.getMethod(), request.getServletPath() + request.getPathInfo()
          );
        }
      }
    }
  }


  private static class TransactionResponse extends HttpServletResponseWrapper
  {
    private HttpServletResponse actual;
    private int status;
    private String statusMsg;

    private TransactionResponse(HttpServletResponse actual)
    {
      super(actual);

      this.actual = actual;
    }


    // HttpServletResponse Overrides --------------------------------------------------------------

    @Override public void sendError(int code, String msg) throws IOException
    {
      actual.sendError(code, msg);

      this.statusMsg = msg;
      this.status = code;
    }

    @Override public void setStatus(int status)
    {
      actual.setStatus(status);

      this.status = status;
    }

    @Override public void setStatus(int status, String msg)
    {
      actual.setStatus(status, msg);

      this.status = status;
      this.statusMsg = msg;
    }

  }


}

