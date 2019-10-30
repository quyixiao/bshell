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

import bsh.Name;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(FilteredTestRunner.class)
public class NamePartsTest {

    @Test
    public void name_parts_is_compound() throws Exception {
        assertFalse("name not compound", Name.isCompound("name"));
        assertTrue("name.name is compound", Name.isCompound("name.name"));
        assertFalse("'' not compound", Name.isCompound(""));
        assertFalse("null not compound", Name.isCompound(null));
    }


    @Test
    public void name_suffix_parts() throws Exception {
        assertEquals("name suffix 1 parts is name", "name", Name.suffix("name", 1));
        assertNull("name suffix 2 parts is null", Name.suffix("name", 2));
        assertNull("a.b suffix 0 parts is null", Name.suffix("a.b", 0));
        assertEquals("a.b suffix 1 parts is b", "b", Name.suffix("a.b", 1));
        assertEquals("a.b suffix 2 parts is a.b", "a.b", Name.suffix("a.b", 2));
        assertNull("a.b suffix 3 parts is null", Name.suffix("a.b", 3));
        assertNull("a.b.c suffix 0 parts is null", Name.suffix("a.b.c", 0));
        assertEquals("a.b.c suffix 1 parts is c", "c", Name.suffix("a.b.c", 1));
        assertEquals("a.b.c suffix 2 parts is b.c", "b.c", Name.suffix("a.b.c", 2));
        assertEquals("a.b.c suffix 3 parts is a.b.c", "a.b.c", Name.suffix("a.b.c", 3));
        assertNull("a.b.c suffix 4 parts is null", Name.suffix("a.b.c", 4));
        assertNull("a.b.c.d suffix 0 parts is null", Name.suffix("a.b.c.d", 0));
        assertEquals("a.b.c.d suffix 1 parts is d", "d", Name.suffix("a.b.c.d", 1));
        assertEquals("a.b.c.d suffix 2 parts is c.d", "c.d", Name.suffix("a.b.c.d", 2));
        assertEquals("a.b.c.d suffix 3 parts is b.c.d", "b.c.d", Name.suffix("a.b.c.d", 3));
        assertEquals("a.b.c.d suffix 4 parts is a.b.c.d", "a.b.c.d", Name.suffix("a.b.c.d", 4));
        assertNull("a.b.c.d suffix 5 parts is null", Name.suffix("a.b.c.d", 5));
        assertEquals("'' suffix 1 parts is ''", "", Name.suffix("", 1));
        assertNull("'' suffix -1 parts is null", Name.suffix("", -1));
        assertNull("null suffix 0 parts is null", Name.suffix(null, 0));
        assertNull("null suffix 1 parts is null", Name.suffix(null, 1));
    }


}
