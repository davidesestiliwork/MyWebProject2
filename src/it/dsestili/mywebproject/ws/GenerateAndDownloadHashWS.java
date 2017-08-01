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

package it.dsestili.mywebproject.ws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import it.dsestili.jhashcode.core.Core;
import it.dsestili.jhashcode.core.DirectoryScanner;
import it.dsestili.jhashcode.core.DirectoryScannerNotRecursive;
import it.dsestili.jhashcode.core.DirectoryScannerRecursive;
import it.dsestili.jhashcode.core.Utils;
import it.dsestili.jhashcode.ui.MainWindow;
import it.dsestili.mywebproject.GenerateAndDownloadHash;
import java.util.Base64;

public class GenerateAndDownloadHashWS extends GenerateAndDownloadHash {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(GenerateAndDownloadHashWS.class);

	private Result r = new Result();
	private List<FileInfo> infos = new ArrayList<FileInfo>();
	private String folder;
	
	private Connection connection = null;
	private static final String PROP_FILE_NAME = "config.properties";
	
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
	
	protected boolean checkToken(String token)
	{
		boolean result = false;
		
		logger.debug("metodo checkToken()");

		openConnection();
		
		try
		{
			connection.setAutoCommit(false);
			
			String getTokenQuery = getProperty("query.getToken");
			
			PreparedStatement statement = connection.prepareStatement(getTokenQuery);
			statement.setString(1, token);
			ResultSet rs = statement.executeQuery();
			rs.next();
			int count = rs.getInt(1);
			
			if(count > 0)
			{
				String querySetUsedToken = getProperty("query.setUsedToken");
				
				statement = connection.prepareStatement(querySetUsedToken);
				statement.setString(1, token);
				statement.executeUpdate();

				result = true;
			}
			
			connection.commit();
		}
		catch(Exception e)
		{
			try 
			{
				connection.rollback();
			} 
			catch(SQLException e1) 
			{
				e1.printStackTrace();
			}
			
			logger.debug("Errore in checkToken()", e);
		}
		finally
		{
			if(connection != null)
			{
				try 
				{
					connection.close();
					connection = null;
					logger.debug("Connessione chiusa");
				} 
				catch(SQLException e) 
				{
					logger.debug("Errore di chiusura connessione", e);
				}
			}
		}
		
		logger.debug("Token " + (result ? "valido" : "NON valido"));
		return result;
	}
	
	protected String getProperty(String key)
	{
		Properties prop = new Properties();
		InputStream input = null;
		String value = null;

		try
		{
			String propFileName = PROP_FILE_NAME;
			input = Utils.class.getClassLoader().getResourceAsStream(propFileName);
			
			if(input == null) 
			{
				logger.debug("File di properties non trovato " + propFileName);
				return null;
			}

			prop.load(input);
			value = prop.getProperty(key);
		}
		catch(IOException e)
		{
			logger.debug("Errore di lettura dal file di properties", e);
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
		
		return value;
	}

	protected String decodeBase64(String enc)
	{
		byte[] decodedBytes = Base64.getDecoder().decode(enc);
		return new String(decodedBytes);
	}

	protected void openConnection()
	{
		if(connection == null)
		{
			try 
			{
				Class.forName("com.mysql.jdbc.Driver");
				
				String connectionString = getProperty("connectionString");
				String userName = decodeBase64(getProperty("userName"));
				String password = decodeBase64(getProperty("password"));
				
				connection = DriverManager.getConnection(connectionString, userName, password);
				logger.debug("Connessione riuscita");
			}
			catch(SQLException e) 
			{
				logger.debug("Errore di connessione", e);
			} 
			catch(ClassNotFoundException e) 
			{
				logger.debug("Errore di connessione", e);
			}
		}
	}

	protected boolean isAllowed(String token, String folder)
	{
		boolean result = false;
		
		logger.debug("metodo isAllowed()");
		
		openConnection();
		
		String baseDir = null;
		try
		{
			String queryGetBaseDir = getProperty("query.getBaseDir");
			
			PreparedStatement statement = connection.prepareStatement(queryGetBaseDir);
			statement.setString(1, token);
			ResultSet rs = statement.executeQuery();
			rs.next();
			baseDir = rs.getString(1);
			
			if(folder.startsWith(baseDir))
			{
				result = true;
			}
		}
		catch(Exception e)
		{
			logger.debug(e);
		}
		finally
		{
			if(connection != null)
			{
				try 
				{
					connection.close();
					connection = null;
					logger.debug("Connessione chiusa");
				} 
				catch(SQLException e) 
				{
					logger.debug("Errore di chiusura connessione", e);
				}
			}
		}

		logger.debug("baseDir: " + baseDir);
		logger.debug("Allowed: " + (result ? "Yes" : "No"));
		return result;
	}
	
	public Result generateAndDownloadHash(String folder, String algorithm, String modeParam, String token)
	{
		long start = System.currentTimeMillis();
		
		if(!checkToken(token))
		{
			return r;
		}
		
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

		if(!isAllowed(token, folder))
		{
			return r;
		}

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

		MainWindow.setItalianLocale();
		
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
