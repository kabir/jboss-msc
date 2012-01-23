/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.msc.service;

import static junit.framework.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.util.TestServiceListener;
import org.jboss.msc.value.InjectedValue;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RemoveAndAddTransitiveDependencyTestCase extends AbstractServiceTest {

    private static final int TIMEOUT = 5;

    @Test
    public void testServiceRemove() throws Exception {
        ServiceName rootName = ServiceName.of("test", "root");
        ServiceName middleName = ServiceName.of("test", "middle");
        ServiceName dependentName = ServiceName.of("test", "dependent");
        TestServiceListener testListener = new TestServiceListener();
        serviceContainer.addListener(testListener);

        Future<ServiceController<?>> rootFuture = testListener.expectServiceStart(rootName);
        Future<ServiceController<?>> middleFuture = testListener.expectServiceStart(middleName);
        Future<ServiceController<?>> dependentFuture = testListener.expectServiceStart(dependentName);
        serviceContainer.addService(rootName, new DependentService()).install();
        DependentService middleService = new DependentService();
        serviceContainer.addService(middleName, middleService).addDependency(rootName, DependentService.class, middleService.injectedService).install();
        DependentService dependentService = new DependentService();
        serviceContainer.addService(dependentName, dependentService).addDependency(middleName, DependentService.class, dependentService.injectedService).install();
        ServiceController<?> rootController = assertServiceController(rootFuture, rootName, State.UP);
        ServiceController<?> middleController = assertServiceController(middleFuture, middleName, State.UP);
        ServiceController<?> dependentController = assertServiceController(dependentFuture, dependentName, State.UP);

        rootFuture = testListener.expectServiceRemoval(rootName);
        rootFuture = testListener.expectServiceRemoval(middleName);
        dependentFuture = testListener.expectServiceStop(dependentName);
        rootController.setMode(Mode.REMOVE);
        middleController.setMode(Mode.REMOVE);
        assertServiceController(rootFuture, rootName, State.REMOVED);
        assertServiceController(middleFuture, middleName, State.REMOVED);
        assertServiceController(dependentFuture, dependentName, State.DOWN);

        rootFuture = testListener.expectServiceStart(rootName);
        middleFuture = testListener.expectServiceStart(middleName);
        dependentFuture = testListener.expectServiceStart(dependentName);
        serviceContainer.addService(rootName, new DependentService()).install();
        middleService = new DependentService();
        serviceContainer.addService(middleName, middleService).addDependency(rootName, DependentService.class, middleService.injectedService).install();
        ServiceController<?> newRootController = assertServiceController(rootFuture, rootName, State.UP);
        ServiceController<?> newMiddleController = assertServiceController(middleFuture, middleName, State.UP);
        ServiceController<?> newDependentController = assertServiceController(dependentFuture, dependentName, State.UP);
        Assert.assertNotSame(rootController, newRootController);
        Assert.assertNotSame(middleController, newMiddleController);
        Assert.assertSame(dependentController, newDependentController);
    }

    @Test
    public void testServiceMakeInactive() throws Exception {
        ServiceName rootName = ServiceName.of("test", "root");
        ServiceName middleName = ServiceName.of("test", "middle");
        ServiceName dependentName = ServiceName.of("test", "dependent");
        TestServiceListener testListener = new TestServiceListener();
        serviceContainer.addListener(testListener);

        Future<ServiceController<?>> rootFuture = testListener.expectServiceStart(rootName);
        Future<ServiceController<?>> middleFuture = testListener.expectServiceStart(middleName);
        Future<ServiceController<?>> dependentFuture = testListener.expectServiceStart(dependentName);
        serviceContainer.addService(rootName, new DependentService()).install();
        DependentService middleService = new DependentService();
        serviceContainer.addService(middleName, middleService).addDependency(rootName, DependentService.class, middleService.injectedService).install();
        DependentService dependentService = new DependentService();
        serviceContainer.addService(dependentName, dependentService).addDependency(rootName, DependentService.class, dependentService.injectedService).install();
        ServiceController<?> rootController = assertServiceController(rootFuture, rootName, State.UP);
        ServiceController<?> middleController = assertServiceController(middleFuture, middleName, State.UP);
        ServiceController<?> dependentController = assertServiceController(dependentFuture, dependentName, State.UP);

        rootFuture = testListener.expectServiceStop(rootName);
        middleFuture = testListener.expectServiceRemoval(middleName);
        dependentFuture = testListener.expectServiceStop(dependentName);
        rootController.setMode(Mode.NEVER);
        middleController.setMode(Mode.REMOVE);
        assertServiceController(rootFuture, rootName, State.DOWN);
        assertServiceController(middleFuture, middleName, State.REMOVED);
        assertServiceController(dependentFuture, dependentName, State.DOWN);

        rootFuture = testListener.expectServiceStart(rootName);
        middleFuture = testListener.expectServiceStart(middleName);
        dependentFuture = testListener.expectServiceStart(dependentName);
        rootController.setMode(Mode.ACTIVE);
        middleService = new DependentService();
        serviceContainer.addService(middleName, middleService).addDependency(rootName, DependentService.class, middleService.injectedService).install();
        ServiceController<?> newRootController = assertServiceController(rootFuture, rootName, State.UP);
        ServiceController<?> newMiddleController = assertServiceController(middleFuture, middleName, State.UP);
        ServiceController<?> newDependentController = assertServiceController(dependentFuture, dependentName, State.UP);
        Assert.assertSame(rootController, newRootController);
        Assert.assertNotSame(middleController, newMiddleController);
        Assert.assertSame(dependentController, newDependentController);
    }

    private static class DependentService implements Service<Void> {

        InjectedValue<DependentService> injectedService = new InjectedValue<DependentService>();

        @Override
        public Void getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }

        @Override
        public void start(StartContext context) throws StartException {
        }

        @Override
        public void stop(StopContext context) {
        }
    }

    private ServiceController<?> assertServiceController(Future<ServiceController<?>> future, ServiceName name, State expectedState) throws InterruptedException, TimeoutException, ExecutionException {
        ServiceController<?> futureController = future.get(TIMEOUT, TimeUnit.SECONDS);
        //Assert.assertNotNull("Service did not reach " + expectedState + " " + name, futureController);
        Assert.assertNotNull(futureController);
        if (expectedState != State.REMOVED) {
            ServiceController<?> registeredController = serviceContainer.getRequiredService(name);
            Assert.assertSame(futureController, registeredController);
        } else {
            try {
                serviceContainer.getRequiredService(name);
                fail("Should not have found service " + name);
            } catch (Exception ignore) {
            }
        }
        Assert.assertSame(expectedState, futureController.getState());
        return futureController;
    }
}
