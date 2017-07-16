package it.dsestili.mywebproject.ws;

/*
GenerateAndDownloadHashWS
Copyright (C) 2017 Davide Sestili

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import it.dsestili.jhashcode.core.Core;
import it.dsestili.jhashcode.core.DirectoryScanner;
import it.dsestili.jhashcode.core.DirectoryScannerNotRecursive;
import it.dsestili.jhashcode.core.DirectoryScannerRecursive;
import it.dsestili.jhashcode.core.Utils;
import it.dsestili.jhashcode.ui.MainWindow;
import it.dsestili.mywebproject.GenerateAndDownloadHash;

public class GenerateAndDownloadHashWS extends GenerateAndDownloadHash {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(GenerateAndDownloadHashWS.class);

	private Result r = new Result();
	private List<FileInfo> infos = new ArrayList<FileInfo>();
	private String folder;

	@Override
	protected void downloadFile(HttpServletResponse response, DirectoryScanner scanner) throws Throwable {
		File[] files = getFiles(scanner);

		for(File f : files)
		{
			logger.debug("Sto generando l'hash code del file " + f.getName());

			FileInfo info = new FileInfo();
			try
			{
				Core core = new Core(f, algorithm);
				core.addIProgressListener(this);
				String hash = core.generateHash();

				info.setHashCode(hash);
				info.setFileName(Utils.getRelativePath(folder, f.getAbsolutePath()));
			}
			catch(FileNotFoundException e)
			{
				info.setHashCode("");
				info.setFileName(e.getMessage());
				logger.debug(e);
			}
			catch(IOException e)
			{
				info.setHashCode("");
				info.setFileName(e.getMessage());
				logger.debug(e);
			}

			infos.add(info);
		}
		
		r.setResult(infos.toArray(new FileInfo[0]));
	}

	public Result generateAndDownloadHash(String folder, String algorithm, String modeParam)
	{
		long start = System.currentTimeMillis();
		
		MainWindow.setItalianLocale();
	
		this.algorithm = algorithm;
		this.folder = folder;
		
		if(folder == null || folder.trim().equals(""))
		{
			logger.debug("folder is null or blank");
			return r;
		}

		if(algorithm == null || algorithm.trim().equals(""))
		{
			logger.debug("algorithm is null or blank");
			return r;
		}
		
		if(modeParam == null || modeParam.trim().equals(""))
		{
			logger.debug("mode is null or blank");
			return r;
		}
		
		logger.debug("Folder: " + folder);
		File directory = new File(folder);
		if(!directory.exists())
		{
			logger.debug("Directory inesistente");
			return r;
		}

		logger.debug("Algorithm: " + algorithm);
		try 
		{
			MessageDigest.getInstance(algorithm);
		} 
		catch(NoSuchAlgorithmException e) 
		{
			logger.debug("Algoritmo inesistente");
			return r;
		}

		logger.debug("Mode: " + modeParam);

		DirectoryScanner scanner = null;

		if(modeParam != null && modeParam.trim().equals("not-recursive"))
		{
			try
			{
				recursive = true;
				scanner = new DirectoryScannerNotRecursive(directory, recursive);
				downloadFile(null, scanner);
			} 
			catch(Throwable e) 
			{
				logger.debug("Si è verificato un errore", e);
			}
		}
		else if(modeParam != null && modeParam.trim().equals("recursive"))
		{
			try 
			{
				recursive = true;
				scanner = new DirectoryScannerRecursive(directory, recursive);
				downloadFile(null, scanner);
			} 
			catch(Throwable e) 
			{
				logger.debug("Si è verificato un errore", e);
			}
		}
		else if(modeParam != null && modeParam.trim().equals("no-subfolders"))
		{
			try 
			{
				recursive = false;
				scanner = new DirectoryScannerNotRecursive(directory, recursive);
				downloadFile(null, scanner);
			} 
			catch(Throwable e) 
			{
				logger.debug("Si è verificato un errore", e);
			}
		}
		else
		{
			logger.debug("Mode error");
			return r;
		}
		
		long elapsed = System.currentTimeMillis() - start;
		logger.debug("Elapsed time: " + Utils.getElapsedTime(elapsed, true));

		return r;
	}
}
