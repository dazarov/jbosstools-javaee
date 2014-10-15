/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.cdi.core.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.cdi.core.CDICorePlugin;
import org.jboss.tools.common.java.IParametedType;
import org.jboss.tools.common.java.ParametedType;
import org.jboss.tools.common.java.ParametedTypeFactory;
import org.jboss.tools.test.util.JobUtils;
import org.jboss.tools.test.util.ResourcesUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class TypeTest extends TestCase {
	IProject project = null;

	public TypeTest() {}
	
	@Override
	protected void setUp() throws Exception {
		project = ResourcesUtils.importProject(DependentProjectsTestSetup.PLUGIN_ID, "/projects/TypeTest");
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
	}

	public void testType() throws Exception {
		ParametedTypeFactory factory = new ParametedTypeFactory();
		IJavaProject jp = JavaCore.create(project);
		IType type = jp.findType("test.Test1");
		ParametedType t = (ParametedType)factory.newParametedType(type);
		R[] rs = new R[3];
		Thread[] ts = new Thread[rs.length];
		for (int i = 0; i < ts.length; i++) {
			rs[i] = new R(t);
			ts[i] = new Thread(rs[i]);
		}
		for (int i = 0; i < ts.length; i++) {
			ts[i].start();			
		}
		for (int i = 0; i < ts.length; i++) {
			ts[i].join();
		}
		for (int i = 0; i < ts.length; i++) {
			if(rs[i].exception != null) {
				fail("" + rs[i].exception);
			}
			assertEquals(11, rs[i].size);
		}		
	}

	class R implements Runnable {
		ParametedType t;
		int size;
		ConcurrentModificationException exception;

		public R(ParametedType t) {
			this.t = t;
		}

		@Override
		public void run() {
			Collection<IParametedType> types = t.getAllTypes();
			size = types.size();
			try {
				for (IParametedType t1: types) {
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
					}
				}
			} catch (ConcurrentModificationException e) {
				exception = e;
			}
		}
	}

	/**
	 * Add copies of test method to
	 * 1) Increase probability of failure.
	 * 2) Check that type cache does not return obsolete objects.
	 * @throws Exception
	 */
	public void testType1() throws Exception {
		File file = new File("/home/daniel/bundles.txt");
		File logFile = new File("/home/daniel/bundles.log");
		FileWriter log = new FileWriter(logFile);
		
		BundleContext context = CDICorePlugin.getDefault().getBundle().getBundleContext();
		Bundle[] bds = context.getBundles();
		
		System.out.println("Bundles loaded - "+bds.length);
		log.write("Bundles loaded - "+bds.length+"\n");
		
		ArrayList<String> names = new ArrayList<String>();
		for(Bundle bundle : bds){
			names.add(bundle.getSymbolicName());
		}

		if(file.exists()){
			System.out.println("File exists, reading stored bundles...");
			log.write("File exists, reading stored bundles...\n");
			
			ArrayList<String> stored = new ArrayList<String>();
			FileReader fr = new FileReader(file);
			BufferedReader in = new BufferedReader(fr); 
			String line = null;
		    while ((line = in.readLine()) != null) {
		    	stored.add(line.trim());
		        //System.out.println("Bundle - ["+line.trim()+"]");
		        //log.write("Bundle - ["+line.trim()+"]\n");
		    }
		    System.out.println("Bundles stored - "+stored.size());
		    log.write("Bundles stored - "+stored.size()+"\n");
		    
		    if(stored.size() > names.size()){
		    	for(String name : names){
		    		if(!findBundle(name, stored)){
		    			System.out.println("Bundle "+name+" not found in stored bundles!");
		    			log.write("Bundle "+name+" not found in stored bundles!\n");
		    		}
		    	}
		    }else{
		    	for(String name : stored){
		    		if(!findBundle(name, names)){
		    			System.out.println("Bundle "+name+" not found in current loaded bundles!");
		    			log.write("Bundle "+name+" not found in current loaded bundles!\n");
		    		}
		    	}
		    }
		    in.close();
		}else{
			System.out.println("File does not exists, writing current bundles...");
			log.write("File does not exists, writing current bundles...\n");
			FileWriter out = new FileWriter(file);
			for(String name : names){
				//System.out.println("Bundle - ["+name+"]");
				//log.write("Bundle - ["+name+"]\n");
				
				out.write(name+"\n");
			}
			out.close();
		}
		log.close();
		testType();
	}
	
	private boolean findBundle(String name, ArrayList<String> names){
		for(String s : names){
			if(s.equals(name))
				return true;
		}
		return false;
	}
	
	public void testType2() throws Exception {
		testType();
	}

	public void tearDown() throws Exception {
		boolean saveAutoBuild = ResourcesUtils.setBuildAutomatically(false);
		project.delete(true, true, null);
		JobUtils.waitForIdle();
		ResourcesUtils.setBuildAutomatically(saveAutoBuild);
	}
}