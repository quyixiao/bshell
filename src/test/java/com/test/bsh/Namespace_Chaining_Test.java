/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *                                                                           *
 /****************************************************************************/

package com.test.bsh;

import bsh.ExternalNameSpace;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.Primitive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * <a href="http://code.google.com/p/beanshell2/issues/detail?id=74">Namespace chaining issue</a>
 */
@RunWith(FilteredTestRunner.class)
public class Namespace_Chaining_Test {


    @Test
    public void override_child_namespace() throws Exception {
        Interpreter root = new Interpreter();
        Interpreter child = new Interpreter(new StringReader(""), System.out, System.err, false, new NameSpace(root.getNameSpace(), "child"));

        root.eval("int bar=42;");
        child.eval("int bar=4711;");

        assertEquals(42, root.eval("bar;"));
        assertEquals(4711, child.eval("bar;"));

        // Method foo declared in parent namespace returns bar
        root.eval("int foo() { return bar; }");

        assertEquals(42, root.eval("foo();"));
        assertEquals(4711, child.eval("foo();"));
    }

    @Test
    public void override_child_child_namespace() throws Exception {
        Interpreter root = new Interpreter();
        Interpreter child = new Interpreter(new StringReader(""), System.out, System.err, false, new NameSpace(root.getNameSpace(), "child"));
        Interpreter child2 = new Interpreter(new StringReader(""), System.out, System.err, false, new NameSpace(child.getNameSpace(), "child2"));

        root.eval("int bar=42;");
        child.eval("int bar=4711;");
        child2.eval("int bar=5;");

        assertEquals(42, root.eval("bar;"));
        assertEquals(4711, child.eval("bar;"));
        assertEquals(5, child2.eval("bar;"));

        // Method foo declared in parent namespace returns bar
        root.eval("int foo() { return bar; }");

        assertEquals(42, root.eval("foo();"));
        assertEquals(4711, child.eval("foo();"));
        assertEquals(5, child2.eval("foo();"));
    }

    @Test
    public void check_ExternalNameSpace() throws Exception {
        final ExternalNameSpace externalNameSpace = new ExternalNameSpace();
        externalNameSpace.setVariable("a", Primitive.NULL, false);
        assertTrue("map should contain variable 'a'", externalNameSpace.getMap().containsKey("a"));
        assertNull("variable 'a' should have value <NULL>", externalNameSpace.getMap().get("a"));
    }

}
