/*
 * Copyright 2014 Iñaki San Vicente

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package elh.eus.MSM;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class FileUtilsElh {

	
	
	/**
	* Check input file integrity.
	* @param name
	* the name of the file
	* @param inFile
	* the file
	*/
	public static boolean checkFile(final String name) 
	{
		return checkFile(new File(name));
	}

	/**
	* Check input file integrity.
	* @param name
	* the name of the file
	* @param inFile
	* the file
	*/
	public static boolean checkFile(final File f) 
	{
		boolean isFailure = true;
		
		if (! f.isFile()) {
			isFailure = false;
		} 
		else if (!f.canRead()) {
			isFailure = false;
		}
		return isFailure;
	}

	
	/**
	* Check input directory integrity.
	* @param name
	* the name of the directory
	* @param inFile
	* the directory
	*/
	public static boolean checkDir(final String name) 
	{
		return checkDir(new File(name));
	}
	
	/**
	* Check input directory integrity.
	* @param name
	* the name of the directory
	* @param inFile
	* the directory
	*/
	public static boolean checkDir(final File f) 
	{
		boolean isFailure = true;
		
		if (! f.isDirectory()) {
			isFailure = false;
		} 
		else if (!f.canRead()) {
			isFailure = false;
		}
		return isFailure;

	}
	
	/**
	* return the name (without path) of a string. The function assumes that the input is a file path.
	* 
	* @param fname : string to extract the name from
	* the directory
	*/
	public static String fileName(String fname) 
	{
		File f = new File(fname);
		return f.getName();
	}
	
	/**
	 * Function creates a temporal directory with a random name.
	 */
	public static File createTempDirectory()
			throws IOException
	{
		final File temp;

		temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

		if(!(temp.delete()))
		{
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if(!(temp.mkdir()))
		{
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}

		return (temp);
	}

	/**
	 * Function renames a file to a new name. if the new name already exists throws an exception.
	 *  
	 */
	public static void renameFile (String oldFile, String newName) throws IOException 
	{
		File file1 = new File(oldFile);
		File file2 = new File(newName);
		if(file2.exists())
		{
			throw new java.io.IOException("FileUtilsElh::renameFile : file exists");
		}
		else if (! file1.renameTo(file2)) 
		{
			System.err.println("FileUtilsElh::renameFile : moving file failed\n\t"+file1.getAbsolutePath()+"\t"+file2.getAbsolutePath()); 
		}
		// Rename file (or directory)
		//file1.renameTo(file2);		 
	}
	
	
}
