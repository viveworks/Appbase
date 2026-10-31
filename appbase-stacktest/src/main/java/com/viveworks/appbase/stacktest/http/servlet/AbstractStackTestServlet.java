/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
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
package com.viveworks.appbase.stacktest.http.servlet;

import com.viveworks.appbase.core.AppbaseCore;
import com.viveworks.appbase.service.web.endpoint.servlet.AbstractAppbaseSyncServlet;
import com.viveworks.appbase.stacktest.service.StackTestServices;

/**
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
public class AbstractStackTestServlet extends AbstractAppbaseSyncServlet {
    private static final long serialVersionUID = -8475551010713370903L;


    protected StackTestServices stackTestServices() {
        return (StackTestServices) getServletContext().getAttribute(StackTestServices.class.getName());
    }

    protected AppbaseCore appbaseCore() {
        return stackTestServices().getAppbaseServices().getCoreServices().appbaseCore();
    }
}
