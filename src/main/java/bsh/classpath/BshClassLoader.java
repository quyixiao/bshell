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
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/


package bsh.classpath;

import bsh.BshClassManager;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * ONE OF THE THINGS BSHCLASSLOADER DOES IS TO ADDRESS A DEFICIENCY IN
 * URLCLASSLOADER THAT PREVENTS US FROM SPECIFYING INDIVIDUAL CLASSES
 * VIA URLS.
 * BSHCLASSloaderer要做的一件事情就是解决URLCLASSloaderer中的缺陷，这阻止了我们通过URL指定单个类
 */
public class BshClassLoader extends URLClassLoader {
    BshClassManager classManager;

    /**
     * @param bases URLs JARClassLoader seems to require absolute paths   URL JARClassLoader似乎需要绝对路径
     */
    public BshClassLoader(BshClassManager classManager, URL[] bases) {
        super(bases);
        this.classManager = classManager;
    }

    /**
     * @param bases URLs JARClassLoader seems to require absolute paths
     *  URL JARClassLoader似乎需要绝对路径
     */
    public BshClassLoader(BshClassManager classManager, BshClassPath bcp) {
        this(classManager, bcp.getPathComponents());
    }

    /**
     * For use by children
     *
     * @param bases URLs JARClassLoader seems to require absolute paths
     *              URL JARClassLoader似乎需要绝对路径
     */
    protected BshClassLoader(BshClassManager classManager) {
        this(classManager, new URL[]{});
    }

    // public version of addURL
    public void addURL(URL url) {
        super.addURL(url);
    }

    /**
     * This modification allows us to reload classes which are in the
     * Java VM user classpath.  We search first rather than delegate to
     * the parent classloader (or bootstrap path) first.
     *
     * 此修改使我们可以重新加载Java VM用户类路径中的类 。
     * 我们首先搜索而不是委托给父类加载器（或引导路径）。
     *
     * <p>
     * An exception is for BeanShell core classes which are always loaded from
     * the same classloader as the interpreter.
     *
     *
     * BeanShell核心类是一个例外，这些核心类总是从与解释器相同的类加载器加载的。
     *
     *
     */
    public Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        Class c = null;

		/*
            Check first for classes loaded through this loader.
			The VM will not allow a class to be loaded twice.

			首先检查通过此加载器加载的类。 VM将不允许类加载两次。


		*/
        c = findLoadedClass(name);
        if (c != null)
            return c;

// This is copied from ClassManagerImpl
// We should refactor this somehow if it sticks around
//这是从ClassManagerImpl复制的
//如果仍然存在，我们应该以某种方式重构它
        if (name.startsWith(ClassManagerImpl.BSH_PACKAGE))
            try {
                return bsh.Interpreter.class.getClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
            }

		/*
			Try to find the class using our classloading mechanism.
			Note: I wish we didn't have to catch the exception here... slow

			尝试使用我们的类加载机制查找类。注意：我希望我们不必在这里捕获异常...慢
		*/
        try {
            c = findClass(name);
        } catch (ClassNotFoundException e) {
        }

        if (c == null)
            throw new ClassNotFoundException("here in loaClass");

        if (resolve)
            resolveClass(c);

        return c;
    }

    /**
     * Find the correct source for the class...
     * <p>
     * Try designated loader if any
     * Try our URLClassLoader paths if any
     * Try base loader if any
     * Try system ???
     */
    // add some caching for not found classes?
    // 为找不到的类添加一些缓存
    protected Class findClass(String name)
            throws ClassNotFoundException {
        // Deal with this cast somehow... maybe have this class use
        // ClassManagerImpl type directly.
        // Don't add the method to BshClassManager... it's really an impl thing
        ClassManagerImpl bcm = (ClassManagerImpl) getClassManager();

        // Should we try to load the class ourselves or delegate?
        // look for overlay loader

        ClassLoader cl = bcm.getLoaderForClass(name);

        Class c;

        // If there is a designated loader and it's not us delegate to it
        if (cl != null && cl != this)
            try {
                return cl.loadClass(name);
            } catch (ClassNotFoundException e) {
                throw new ClassNotFoundException(
                        "Designated loader could not find class: " + e);
            }

        // Let URLClassLoader try any paths it may have
        if (getURLs().length > 0)
            try {
                return super.findClass(name);
            } catch (ClassNotFoundException e) {
                //System.out.println(
                //	"base loader here caught class not found: "+name );
            }


        // If there is a baseLoader and it's not us delegate to it
        cl = bcm.getBaseLoader();

        if (cl != null && cl != this)
            try {
                return cl.loadClass(name);
            } catch (ClassNotFoundException e) {
            }

        // Try system loader
        return bcm.plainClassForName(name);
    }

	/*
		The superclass does something like this

        c = findLoadedClass(name);
        if null
            try
                if parent not null
                    c = parent.loadClass(name, false);
                else
                    c = findBootstrapClass(name);
            catch ClassNotFoundException 
                c = findClass(name);
	*/

    BshClassManager getClassManager() {
        return classManager;
    }

}
