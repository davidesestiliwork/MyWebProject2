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

package it.dsestili.mywebproject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import it.dsestili.jhashcode.ui.MainWindow;

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
	private static final String WARNING_MESSAGE = "Warning: ";
	
	protected String algorithm;
	protected boolean recursive;
	protected String folder;
	
	protected static final int BUFFER_SIZE = 128 * 1024;
	
	protected String fileName;
	
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
		MainWindow.setExcludeSymbolicLinks(true);
		MainWindow.setExcludeHiddenFiles(true);
		
		Properties prop = new Properties();
		InputStream input = null;

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

		logger.debug("Mode: " + modeParam);

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
		File zipFile = generateZipFile(temp.getAbsolutePath());
		byte[] data = getDataFromFile(zipFile.getAbsolutePath());
		
		response.setContentType("application/x-zip");
		String fileName = algorithm.replace("-", "").toUpperCase() + "SUMS.zip";
		response.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
	    response.setHeader("Cache-Control", "no-cache");
	    response.setHeader("Expires", "-1");
	    
	    response.getOutputStream().write(data);
	}

	protected File generateZipFile(String tempFilePath) throws Throwable
	{
		File zipFile = File.createTempFile("tempfile", ".zip");

		logger.debug("generateZipFile() - tempFilePath: " + tempFilePath);
		logger.debug("generateZipFile() - zipFilePath: " + zipFile.getAbsolutePath());

		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		ZipOutputStream out = null;
		
		try
		{
			fos = new FileOutputStream(zipFile.getAbsolutePath());
			bos = new BufferedOutputStream(fos);
			out = new ZipOutputStream(bos);

			String fileName = algorithm.replace("-", "").toUpperCase() + "SUMS";
			addEntry(fileName, tempFilePath, out);

			logger.debug("signing " + tempFilePath);
			int exitCode = Utils.gpgSignFile(tempFilePath);
			if(exitCode == 0)
			{
				logger.debug("file signed successfully");
				fileName = fileName + ".asc";
				String signedFilePath = tempFilePath + ".asc";
				addEntry(fileName, signedFilePath, out);
			}
			else
			{
				logger.debug("error signing file, exitCode: " + exitCode);
			}
		}
		finally
		{
			out.close();
		}

		return zipFile;
	}

	protected void addEntry(String fileName, String filePath, ZipOutputStream out) throws Throwable
	{
		logger.debug("addEntry() - fileName: " + fileName);
		logger.debug("addEntry() - filePath: " + filePath);

		byte buf[] = new byte[BUFFER_SIZE];
		
		FileInputStream fis = null;
		BufferedInputStream bis = null;

		try
		{
			fis = new FileInputStream(filePath);
			bis = new BufferedInputStream(fis);
			ZipEntry entry = new ZipEntry(fileName);
			out.putNextEntry(entry);

			int len;
			while((len = bis.read(buf, 0, buf.length)) != -1)
			{
			   out.write(buf, 0, len);
			}
		}
		finally
		{
			bis.close();
			logger.debug("addEntry() - stream closed");
		}
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

	protected File getTempFile() throws IOException
	{
		//String tempFolderPath = System.getProperty("jboss.server.temp.dir"); Removed for compatibility with other application server
		File temp = File.createTempFile("tempfile", ".tmp"/*, new File(tempFolderPath)*/);
		return temp;
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
			temp = getTempFile();
			
			fos = new FileOutputStream(temp);
			bos = new BufferedOutputStream(fos);

			File baseDir = new File(folder);
			
			for(File f : files)
			{
				fileName = f.getName();
				
				String lineOfText = null;
				try
				{
					Core core = new Core(f, algorithm);
					core.addIProgressListener(this);
					String hash = core.generateHash();

					lineOfText = hash + " *" + (recursive ? Utils.getRelativePath(baseDir, f) : f.getName()) + "\n";
				}
				catch(FileNotFoundException e)
				{
					lineOfText = WARNING_MESSAGE + e.getMessage() + "\n";
					logger.debug(e);
				}
				catch(IOException e)
				{
					lineOfText = WARNING_MESSAGE + e.getMessage() + "\n";
					logger.debug(e);
				}
				
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
		int symbolicLinkExcluded = di.getSymbolicLinksExcluded();
		
		logger.debug("Scanning completed, " + files.length + " files found, " + totalSize + " bytes total size");
		
		if(symbolicLinkExcluded > 0)
		{
			logger.debug(symbolicLinkExcluded + " symbolic link excluded");
		}
		else
		{
			logger.debug("No symbolic link excluded");
		}
		
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
		logger.debug(fileName + " " + event.toString());
	}
}
