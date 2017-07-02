package it.dsestili.mywebproject;

/*
GenerateAndDownloadHash servlet
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

import it.dsestili.jhashcode.core.Core;
import it.dsestili.jhashcode.core.DirectoryInfo;
import it.dsestili.jhashcode.core.DirectoryScanner;
import it.dsestili.jhashcode.core.DirectoryScannerNotRecursive;
import it.dsestili.jhashcode.core.DirectoryScannerRecursive;
import it.dsestili.jhashcode.core.IProgressListener;
import it.dsestili.jhashcode.core.IScanProgressListener;
import it.dsestili.jhashcode.core.ProgressEvent;
import it.dsestili.jhashcode.core.Utils;
import it.dsestili.jhashcode.gui.MainWindow;

/**
 * Servlet implementation class GenerateAndDownloadHash
 */
@WebServlet("/GenerateAndDownloadHash")
public class GenerateAndDownloadHash extends HttpServlet implements IProgressListener, IScanProgressListener {
	private static final long serialVersionUID = 1L;
       
	private static final Logger logger = Logger.getLogger(GenerateAndDownloadHash.class);
	private static final String MODE_PARAM = "mode";
	private static final String PROP_FILE_NAME = "config.properties";
	private static final String FOLDER = "folder";
	private static final String ALGORITHM = "algorithm";
	
	private String algorithm;
	private boolean recursive;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GenerateAndDownloadHash() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		long start = System.currentTimeMillis();
		
		String modeParam = (String)request.getParameter(MODE_PARAM);

		MainWindow.setItalianLocale();
		
		Properties prop = new Properties();
		InputStream input = null;
		String folder = null;

		try
		{
			input = getClass().getClassLoader().getResourceAsStream(PROP_FILE_NAME);

			if(input == null) 
			{
				logger.debug("File di properties non trovato " + PROP_FILE_NAME);
				return;
			}

			prop.load(input);
			folder = prop.getProperty(FOLDER);
			algorithm = prop.getProperty(ALGORITHM);
		}
		catch(IOException ex)
		{
			logger.debug("Errore di lettura dal file di properties", ex);
		}
		finally
		{
			if (input != null) 
			{
				try 
				{
					input.close();
				} 
				catch(IOException e) 
				{
					logger.debug("Errore di chiusura input stream", e);
				}
			}
		}
		
		logger.debug("Folder: " + folder);
		File directory = new File(folder);
		if(!directory.exists())
		{
			logger.debug("Directory inesistente");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Directory inesistente");
			return;
		}

		logger.debug("Algorithm: " + algorithm);
		try 
		{
			MessageDigest.getInstance(algorithm);
		} 
		catch(NoSuchAlgorithmException e) 
		{
			logger.debug("Algoritmo inesistente");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Algoritmo inesistente");
			return;
		}

		DirectoryScanner scanner = null;

		if(modeParam != null && modeParam.trim().equals("not-recursive"))
		{
			try
			{
				recursive = true;
				scanner = new DirectoryScannerNotRecursive(directory, recursive);
				downloadFile(response, scanner);
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
				downloadFile(response, scanner);
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
				downloadFile(response, scanner);
			} 
			catch(Throwable e) 
			{
				logger.debug("Si è verificato un errore", e);
			}
		}
		else
		{
			logger.debug("Mode error");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mode error");
			return;
		}
		
		long elapsed = System.currentTimeMillis() - start;
		logger.debug("Elapsed time: " + Utils.getElapsedTime(elapsed, true));
	}

	//restituisce al client il file contenente gli hash code della cartella analizzata
	//vedi config.properties chiave folder
	protected void downloadFile(HttpServletResponse response, DirectoryScanner scanner) throws Throwable
	{
		File[] files = getFiles(scanner);
		File temp = generateTempFile(files);
		byte[] data = getDataFromFile(temp.getAbsolutePath());
		
		response.setContentType("text/plain");
		String fileName = algorithm.replace("-", "").toUpperCase() + "SUMS";
		response.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
	    response.setHeader("Cache-Control", "no-cache");
	    response.setHeader("Expires", "-1");
	    
	    response.getOutputStream().write(data);
	}

	//restituisce un array di byte leggendo da un file in input
	protected byte[] getDataFromFile(String fileName) throws IOException
	{
		File file = new File(fileName);
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream bis = new BufferedInputStream(fis);
		
		byte[] data = new byte[(int) file.length()];
		bis.read(data);
		
		bis.close();
		
		return data;
	}

	//genera un file temporaneo contenente gli hash code ed i relativi nomi di file
	//a partire dall'elenco dei file contenuti nella cartella
	protected File generateTempFile(File[] files)
	{
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;

		File temp = null;
		
		try 
		{
			String tempFolderPath = System.getProperty("jboss.server.temp.dir");
			temp = File.createTempFile("tempfile", ".tmp", new File(tempFolderPath));
			
			fos = new FileOutputStream(temp);
			bos = new BufferedOutputStream(fos);

			for(File currentFile : files)
			{
				logger.debug("Sto generando l'hash code del file " + currentFile.getName());
				
				Core core = new Core(currentFile, algorithm);
				core.addIProgressListener(this);
				String hash = core.generateHash();
				
				String lineOfText = hash + " *" + (recursive ? currentFile.getAbsolutePath() : currentFile.getName()) + "\n";
				byte[] data = lineOfText.getBytes("UTF-8");
				
				bos.write(data);
			}
			
			logger.debug("File temporaneo creato");
		} 
		catch(IOException e) 
		{
			logger.debug("Errore di I/O", e);
		} 
		catch(NoSuchAlgorithmException e) 
		{
			logger.debug("Algoritmo inesistente", e);
		} 
		catch(Throwable e) 
		{
			logger.debug("Si è verificato un errore", e);
		}
		finally
		{
			if(bos != null)
			{
				try 
				{
					bos.close();
				}
				catch(IOException e)
				{
					logger.debug("Errore di chiusura file");
				}
			}
		}

		return temp;
	}

	//restituisce un elenco di file sotto forma di array prendendo in input
	//l'oggetto che scansiona la cartella
	protected File[] getFiles(DirectoryScanner scanner) throws Throwable
	{
		scanner.addIScanProgressListener(this);
		DirectoryInfo di = scanner.getFiles();
		File[] files = di.getFiles();
		long totalSize = di.getTotalSize();
		
		logger.debug("Scanning completed, " + files.length + " files found, " + totalSize + " bytes total size");
		return files;
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	@Override
	public void scanProgressEvent(ProgressEvent event) 
	{
		logger.debug(event.toString());
	}

	@Override
	public void progressEvent(ProgressEvent event) 
	{
		logger.debug(event.toString());
	}
}
